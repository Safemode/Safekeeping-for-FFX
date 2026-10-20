package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Every id that ends up as a database key - and so in players' saved progress and backup files - is
 * checked against a frozen list in `src/test/frozen-ids`.
 *
 * Most of these ids are derived rather than written out: Monster Arena and Item List ids are slugs
 * of the CSV names, so fixing a typo in a fiend's name or area silently changes its id and orphans
 * every capture count and star stored under the old one. Nothing else would notice - the list still
 * renders, just with that progress gone.
 *
 * - An id that **disappears** fails the test. Undo the edit that changed it (for the CSV lists that
 *   means the name or area, since the id is built from them), or, if the change is deliberate, write
 *   a migration that moves the stored rows to the new id and then remove the line by hand.
 * - An id that is **new** also fails, so the list never falls behind. Freeze new ids with
 *   `./gradlew test -PupdateFrozenIds`, which only ever appends, then commit the file.
 */
class FrozenIdsTest {

    private val update = System.getProperty("updateFrozenIds") == "true"

    /** Checklist item ids, as `categoryId/itemId` - an item id is only unique inside its list. */
    private val checklistItems: Set<String> = favoriteSources.categories
        .flatMap { category -> category.items.map { "${category.id}/${it.id}" } }
        .toSet()

    /**
     * The category keys progress is stored under. Per-character lists keep one per character
     * (e.g. `overdrive_modes_tidus`), so renaming a character or a list would orphan those too.
     */
    private val progressKeys: Set<String> =
        favoriteSources.categories.flatMap { it.progressKeys }.toSet()

    private val itemList: Set<String> =
        ItemListCsvParser.parse(asset("item_list.csv")).items.map { it.id }.toSet()

    private val monsterArena: Set<String> =
        MonsterArenaCsvParser.parse(asset("monster_arena.csv")).map { it.id }.toSet()

    private val sphereGridNodes: Set<String> = GridType.entries.flatMap { type ->
        val asset = type.asset ?: return@flatMap emptyList()
        SphereGridParser.parse(asset(asset), type.idPrefix).nodes.map { it.id }
    }.toSet()

    @Test
    fun checklistItemIdsAreFrozen() = assertFrozen("checklist-items", checklistItems)

    @Test
    fun progressKeysAreFrozen() = assertFrozen("progress-keys", progressKeys)

    @Test
    fun itemListIdsAreFrozen() = assertFrozen("item-list", itemList)

    @Test
    fun monsterArenaIdsAreFrozen() = assertFrozen("monster-arena", monsterArena)

    @Test
    fun sphereGridNodeIdsAreFrozen() = assertFrozen("sphere-grid-nodes", sphereGridNodes)

    /**
     * Ids must come out the same on every device. Building one with locale-aware formatting
     * ("%02d".format) writes native digits under e.g. a Persian locale, which would key saved
     * progress differently per device language.
     */
    @Test
    fun everyStoredIdIsPlainAscii() {
        val nonAscii = (checklistItems + progressKeys + itemList + monsterArena + sphereGridNodes)
            .filterNot { id -> id.all { it.code < 128 } }

        assertEquals(emptyList<String>(), nonAscii)
    }

    private fun assertFrozen(name: String, current: Set<String>) {
        val file = File("src/test/frozen-ids/$name.txt")
        val frozen = if (file.exists()) {
            file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        } else {
            emptySet()
        }

        val missing = (frozen - current).sorted()
        val added = (current - frozen).sorted()

        if (added.isNotEmpty() && update) {
            file.parentFile.mkdirs()
            file.writeText((frozen + added).sorted().joinToString("\n", postfix = "\n"))
        }

        val problems = buildList {
            if (missing.isNotEmpty()) {
                add(
                    "${missing.size} frozen id(s) in ${file.path} no longer exist. Saved progress, " +
                        "stars and backups still point at them, so changing or removing one orphans " +
                        "that progress. Undo the edit that changed it (for the CSV lists, the name or " +
                        "area the id is built from), or write a migration that moves the stored rows " +
                        "and then remove the line by hand:\n  " + missing.joinToString("\n  ")
                )
            }
            if (added.isNotEmpty() && !update) {
                add(
                    "${added.size} new id(s) are not frozen yet. Run `./gradlew test " +
                        "-PupdateFrozenIds` to append them to ${file.path}, then commit it:\n  " +
                        added.joinToString("\n  ")
                )
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n\n"))
    }

    private fun asset(name: String): String = File("src/main/assets/$name").readText()
}
