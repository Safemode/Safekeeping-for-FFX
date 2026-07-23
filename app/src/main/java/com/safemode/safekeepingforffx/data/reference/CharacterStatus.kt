package com.safemode.safekeepingforffx.data.reference

/**
 * The in-game ceiling for each stat. Nothing on the grid can push a stat past these, so a line that
 * reaches its cap is wasted sphere spend - exactly what a planner wants flagged.
 */
private fun capOf(attribute: NodeType): Int = when (attribute) {
    NodeType.HP -> 99999
    NodeType.MP -> 9999
    else -> 255
}

/** One stat: where the character started, what the activated nodes added, and where that lands. */
data class StatLine(
    val attribute: NodeType,
    val base: Int,
    val fromGrid: Int
) {
    val cap: Int get() = capOf(attribute)

    /** The playable value, which never exceeds the cap however many nodes are activated. */
    val total: Int get() = (base + fromGrid).coerceAtMost(cap)

    val isCapped: Boolean get() = base + fromGrid >= cap

    /** Grid gain that lands past the cap and buys nothing. Zero unless [isCapped]. */
    val wasted: Int get() = (base + fromGrid - cap).coerceAtLeast(0)
}

/**
 * One ability family: what the character has learned and what the grid still holds for them. The
 * remainder is drawn from the grid as it currently stands, edits included, so moving an ability onto
 * a node the character can reach shows up here immediately.
 */
data class AbilityGroup(
    val family: NodeType,
    val learned: List<String>,
    val remaining: List<String>
) {
    val availableOnGrid: Int get() = learned.size + remaining.size
}

/**
 * A character's status derived purely from the grid in front of the player: base stats plus every
 * activated node's content. Nothing here is stored - it is recomputed whenever the grid, the edits,
 * the path or the selected character change, so it always describes what is on screen.
 */
data class CharacterStatus(
    val character: GridCharacter,
    val attributes: List<StatLine>,
    val abilities: Map<NodeType, AbilityGroup>,
    val activatedNodes: Int
) {
    fun group(family: NodeType): AbilityGroup =
        abilities[family] ?: AbilityGroup(family, emptyList(), emptyList())
}

/** The ability families the status view pages through, in the order it shows them. */
val AbilityFamilies: List<NodeType> =
    listOf(NodeType.SKILL, NodeType.SPECIAL, NodeType.WHITE_MAGIC, NodeType.BLACK_MAGIC)

object CharacterStatusCalculator {

    /**
     * Rolls the character's activated nodes up into a status readout.
     *
     * [overrides] and [activated] are passed in rather than read from anywhere, so this works
     * unchanged for live progress and for a read-only route replay - whatever the canvas is drawing,
     * this describes. A node's content is its override if the player edited it, else its vanilla
     * content, which is the same rule the canvas draws by.
     *
     * Duplicate abilities collapse: the grid holds Cure on several nodes, but learning it twice does
     * nothing in game, so it counts once.
     */
    fun compute(
        character: GridCharacter,
        baseStats: BaseStats?,
        grid: GridData,
        overrides: Map<String, NodeContent>,
        activated: Set<String>
    ): CharacterStatus {
        val gains = HashMap<NodeType, Int>()
        val learned = HashMap<NodeType, LinkedHashSet<String>>()
        val onGrid = HashMap<NodeType, LinkedHashSet<String>>()
        var activatedNodes = 0

        grid.nodes.forEach { node ->
            val content = overrides[node.id] ?: node.original
            if (content is NodeContent.Ability) {
                onGrid.getOrPut(content.family) { LinkedHashSet() }.add(content.name)
            }
            if (node.id !in activated) return@forEach
            activatedNodes++
            when (content) {
                is NodeContent.Attribute ->
                    gains[content.attribute] = (gains[content.attribute] ?: 0) + content.value
                is NodeContent.Ability ->
                    learned.getOrPut(content.family) { LinkedHashSet() }.add(content.name)
                // An activated blank node, and a lock the path has walked through, add nothing.
                else -> Unit
            }
        }

        val attributes = NodeType.attributes.map { attribute ->
            StatLine(
                attribute = attribute,
                base = baseStats?.valueOf(attribute) ?: 0,
                fromGrid = gains[attribute] ?: 0
            )
        }

        val abilities = AbilityFamilies.associateWith { family ->
            val known = learned[family].orEmpty()
            AbilityGroup(
                family = family,
                learned = known.sorted(),
                remaining = onGrid[family].orEmpty().filterNot { it in known }.sorted()
            )
        }

        return CharacterStatus(
            character = character,
            attributes = attributes,
            abilities = abilities,
            activatedNodes = activatedNodes
        )
    }

    /**
     * Which node to jump to when the player taps an ability in the status sheet.
     *
     * On both vanilla grids every ability sits on exactly one node, so there is normally a single
     * candidate and no choice to make. Edits break that: nothing stops the player writing Cure onto
     * a dozen nodes, so the tie has to be broken anyway. A learned ability resolves to the node the
     * character actually took it from; an unlearned one to whichever copy sits closest to their
     * path, which is the copy they would realistically walk to; with no path yet, to the first copy
     * on the grid. Returns null when no node holds the ability, which an edit can also cause.
     */
    fun nodeForAbility(
        name: String,
        family: NodeType,
        grid: GridData,
        overrides: Map<String, NodeContent>,
        activated: Set<String>
    ): String? {
        val candidates = grid.nodes.filter { node ->
            val content = overrides[node.id] ?: node.original
            content is NodeContent.Ability && content.name == name && content.family == family
        }
        if (candidates.isEmpty()) return null

        candidates.firstOrNull { it.id in activated }?.let { return it.id }

        val path = grid.nodes.filter { it.id in activated }
        if (path.isEmpty()) return candidates.first().id

        return candidates.minByOrNull { candidate ->
            path.minOf { step ->
                val dx = (candidate.x - step.x).toDouble()
                val dy = (candidate.y - step.y).toDouble()
                dx * dx + dy * dy
            }
        }?.id
    }
}
