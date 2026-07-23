package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.BaseStatsCsvParser
import com.safemode.safekeepingforffx.data.reference.CharacterStatusCalculator
import com.safemode.safekeepingforffx.data.reference.GridBounds
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Covers the two halves of the character status readout: reading the bundled base stats, and
 * totalling a character's activated nodes on top of them.
 *
 * The base stats file is parsed straight off disk, so a bad edit to the CSV fails here rather than
 * silently showing a character with zeroed stats.
 */
class CharacterStatusTest {

    private val baseStats by lazy {
        BaseStatsCsvParser.parse(File("src/main/assets/base_stats.csv").readText())
    }

    @Test
    fun everyCharacterHasBaseStats() {
        assertEquals(GridCharacter.entries.size, baseStats.size)
        GridCharacter.entries.forEach { character ->
            val stats = baseStats[character]
            assertTrue("$character is missing from base_stats.csv", stats != null)
            NodeType.attributes.forEach { attribute ->
                assertTrue(
                    "$character has no $attribute in base_stats.csv",
                    stats!!.values.containsKey(attribute)
                )
            }
        }
    }

    @Test
    fun baseStatsAreReadByColumnName() {
        val tidus = baseStats.getValue(GridCharacter.TIDUS)
        assertEquals(520, tidus.valueOf(NodeType.HP))
        assertEquals(12, tidus.valueOf(NodeType.MP))
        assertEquals(15, tidus.valueOf(NodeType.STRENGTH))
        // Luck sits between Agility and Evasion in the file, so a positional read would swap these.
        assertEquals(18, tidus.valueOf(NodeType.LUCK))
        assertEquals(10, tidus.valueOf(NodeType.EVASION))
        assertEquals(10, tidus.valueOf(NodeType.ACCURACY))

        val lulu = baseStats.getValue(GridCharacter.LULU)
        assertEquals(30, lulu.valueOf(NodeType.MAGIC_DEFENSE))
    }

    /** A header written with a space, as the file was originally, must still land on the same stat. */
    @Test
    fun headerMatchingIgnoresSpacing() {
        val parsed = BaseStatsCsvParser.parse(
            "Character,HP,Magic Defense\nYuna,475,20\n"
        )
        assertEquals(20, parsed.getValue(GridCharacter.YUNA).valueOf(NodeType.MAGIC_DEFENSE))
    }

    @Test
    fun onlyActivatedNodesCountTowardStats() {
        val grid = gridOf(
            "n1" to NodeContent.Attribute(NodeType.STRENGTH, 4),
            "n2" to NodeContent.Attribute(NodeType.STRENGTH, 3),
            "n3" to NodeContent.Attribute(NodeType.HP, 300)
        )
        val status = CharacterStatusCalculator.compute(
            character = GridCharacter.TIDUS,
            baseStats = baseStats[GridCharacter.TIDUS],
            grid = grid,
            overrides = emptyMap(),
            activated = setOf("n1", "n3")
        )

        val strength = status.attributes.first { it.attribute == NodeType.STRENGTH }
        assertEquals(15, strength.base)
        assertEquals(4, strength.fromGrid)
        assertEquals(19, strength.total)

        val hp = status.attributes.first { it.attribute == NodeType.HP }
        assertEquals(820, hp.total)
        assertEquals(2, status.activatedNodes)
    }

    @Test
    fun editedContentWinsOverVanilla() {
        val grid = gridOf("n1" to NodeContent.Attribute(NodeType.STRENGTH, 4))
        val status = CharacterStatusCalculator.compute(
            character = GridCharacter.TIDUS,
            baseStats = baseStats[GridCharacter.TIDUS],
            grid = grid,
            overrides = mapOf("n1" to NodeContent.Attribute(NodeType.MAGIC, 4)),
            activated = setOf("n1")
        )
        assertEquals(0, status.attributes.first { it.attribute == NodeType.STRENGTH }.fromGrid)
        assertEquals(4, status.attributes.first { it.attribute == NodeType.MAGIC }.fromGrid)
    }

    @Test
    fun statsStopAtTheirCap() {
        // 64 Strength +4 nodes is 256 on top of a base of 15 - well past the 255 ceiling.
        val nodes = (1..64).map { "n$it" to NodeContent.Attribute(NodeType.STRENGTH, 4) }
        val status = CharacterStatusCalculator.compute(
            character = GridCharacter.TIDUS,
            baseStats = baseStats[GridCharacter.TIDUS],
            grid = gridOf(*nodes.toTypedArray()),
            overrides = emptyMap(),
            activated = nodes.map { it.first }.toSet()
        )
        val strength = status.attributes.first { it.attribute == NodeType.STRENGTH }
        assertEquals(255, strength.total)
        assertTrue(strength.isCapped)
        assertEquals(16, strength.wasted)
    }

    @Test
    fun abilitiesSplitIntoLearnedAndRemaining() {
        val grid = gridOf(
            "n1" to NodeContent.Ability("Cure", NodeType.WHITE_MAGIC),
            // The grid holds Cure twice; learning it twice does nothing, so it counts once.
            "n2" to NodeContent.Ability("Cure", NodeType.WHITE_MAGIC),
            "n3" to NodeContent.Ability("Esuna", NodeType.WHITE_MAGIC),
            "n4" to NodeContent.Ability("Fire", NodeType.BLACK_MAGIC)
        )
        val status = CharacterStatusCalculator.compute(
            character = GridCharacter.YUNA,
            baseStats = baseStats[GridCharacter.YUNA],
            grid = grid,
            overrides = emptyMap(),
            activated = setOf("n1", "n2")
        )

        val white = status.group(NodeType.WHITE_MAGIC)
        assertEquals(listOf("Cure"), white.learned)
        assertEquals(listOf("Esuna"), white.remaining)
        assertEquals(2, white.availableOnGrid)

        val black = status.group(NodeType.BLACK_MAGIC)
        assertTrue(black.learned.isEmpty())
        assertEquals(listOf("Fire"), black.remaining)
    }

    @Test
    fun activatedLocksAndBlanksAddNothing() {
        val grid = gridOf(
            "n1" to NodeContent.Lock(2),
            "n2" to NodeContent.Empty
        )
        val status = CharacterStatusCalculator.compute(
            character = GridCharacter.AURON,
            baseStats = baseStats[GridCharacter.AURON],
            grid = grid,
            overrides = emptyMap(),
            activated = setOf("n1", "n2")
        )
        assertTrue(status.attributes.all { it.fromGrid == 0 })
        assertEquals(1030, status.attributes.first { it.attribute == NodeType.HP }.total)
        assertEquals(2, status.activatedNodes)
    }

    @Test
    fun abilityLookupPrefersTheNodeItWasLearnedFrom() {
        val grid = gridAt(
            Triple("n1", 0f, NodeContent.Ability("Cure", NodeType.WHITE_MAGIC)),
            Triple("n2", 10f, NodeContent.Ability("Cure", NodeType.WHITE_MAGIC)),
            Triple("n3", 20f, NodeContent.Attribute(NodeType.MP, 10))
        )
        val node = CharacterStatusCalculator.nodeForAbility(
            name = "Cure",
            family = NodeType.WHITE_MAGIC,
            grid = grid,
            overrides = emptyMap(),
            activated = setOf("n2", "n3")
        )
        assertEquals("n2", node)
    }

    @Test
    fun unlearnedAbilityResolvesToTheCopyNearestThePath() {
        val grid = gridAt(
            Triple("far", 0f, NodeContent.Ability("Holy", NodeType.WHITE_MAGIC)),
            Triple("near", 90f, NodeContent.Ability("Holy", NodeType.WHITE_MAGIC)),
            Triple("path", 100f, NodeContent.Attribute(NodeType.MP, 10))
        )
        val node = CharacterStatusCalculator.nodeForAbility(
            name = "Holy",
            family = NodeType.WHITE_MAGIC,
            grid = grid,
            overrides = emptyMap(),
            activated = setOf("path")
        )
        assertEquals("near", node)
    }

    @Test
    fun abilityLookupWithNoPathFallsBackToTheFirstCopy() {
        val grid = gridAt(
            Triple("n1", 0f, NodeContent.Ability("Flare", NodeType.BLACK_MAGIC)),
            Triple("n2", 50f, NodeContent.Ability("Flare", NodeType.BLACK_MAGIC))
        )
        val node = CharacterStatusCalculator.nodeForAbility(
            name = "Flare",
            family = NodeType.BLACK_MAGIC,
            grid = grid,
            overrides = emptyMap(),
            activated = emptySet()
        )
        assertEquals("n1", node)
    }

    /** An edit can take the last copy of an ability off the grid, leaving nowhere to jump to. */
    @Test
    fun abilityLookupIsNullWhenAnEditRemovedItFromTheGrid() {
        val grid = gridOf("n1" to NodeContent.Ability("Ultima", NodeType.BLACK_MAGIC))
        val node = CharacterStatusCalculator.nodeForAbility(
            name = "Ultima",
            family = NodeType.BLACK_MAGIC,
            grid = grid,
            overrides = mapOf("n1" to NodeContent.Attribute(NodeType.MAGIC, 4)),
            activated = emptySet()
        )
        assertEquals(null, node)
    }

    /** Grid with explicit x positions, so "nearest to the path" has something to measure. */
    private fun gridAt(vararg nodes: Triple<String, Float, NodeContent>): GridData = GridData(
        nodes = nodes.map { (id, x, content) ->
            SphereGridNode(id = id, x = x, y = 0f, original = content)
        },
        edges = emptyList(),
        bounds = GridBounds(0f, 0f, 1f, 1f)
    )

    private fun gridOf(vararg nodes: Pair<String, NodeContent>): GridData = GridData(
        nodes = nodes.mapIndexed { index, (id, content) ->
            SphereGridNode(id = id, x = index.toFloat(), y = 0f, original = content)
        },
        edges = emptyList(),
        bounds = GridBounds(0f, 0f, 1f, 1f)
    )
}
