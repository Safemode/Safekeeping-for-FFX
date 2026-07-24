package com.safemode.safekeepingforffx.data.reference

/**
 * Where the game drops each character onto the grid: the node they occupy before they have activated
 * anything. Used to frame the planner's opening view on a character who has no path yet, so the
 * canvas lands on their corner of the grid instead of the whole board zoomed out.
 *
 * These are vanilla positions, so they are fixed node ids rather than something derived from the
 * node's current content - a player edit that turns a neighbour into an ability must not move
 * anyone's starting point.
 */
object GridStartNodes {

    /**
     * Standard grid starting nodes, each identified by the ability it sits beside:
     *
     * - Tidus: the second blank node out from Cheer, against the Lv. 1 lock
     * - Yuna: the blank node on Esuna
     * - Auron: the blank node on Power Break
     * - Kimahri: the blank node under Lancet
     * - Wakka: the blank node on Dark Attack
     * - Lulu: the blank node under Thunder
     * - Rikku: the blank node on Steal
     */
    private val standard: Map<GridCharacter, String> = mapOf(
        GridCharacter.TIDUS to "n479",
        GridCharacter.YUNA to "n371",
        GridCharacter.AURON to "n549",
        GridCharacter.KIMAHRI to "n291",
        GridCharacter.WAKKA to "n556",
        GridCharacter.LULU to "n250",
        GridCharacter.RIKKU to "n165"
    )

    /**
     * Expert grid starting nodes. The Expert grid redistributes everything, so these sit beside
     * different abilities than their Standard counterparts:
     *
     * - Tidus: the blank node on Cheer
     * - Yuna: the blank node on Extract Ability
     * - Auron: the blank node on Power Break
     * - Kimahri: Lancet itself - the one character who starts on an ability rather than a blank
     * - Wakka: the middle of the run of three blank nodes leading off Dark Attack
     * - Lulu: the blank node on Blizzard, the one carrying three further blanks
     * - Rikku: the blank node on Use, the one carrying a blank and an HP +200
     */
    private val expert: Map<GridCharacter, String> = mapOf(
        GridCharacter.TIDUS to "x44",
        GridCharacter.YUNA to "x10",
        GridCharacter.AURON to "x39",
        GridCharacter.KIMAHRI to "x14",
        GridCharacter.WAKKA to "x25",
        GridCharacter.LULU to "x20",
        GridCharacter.RIKKU to "x81"
    )

    /** The node [character] starts on for [gridType], or null when that grid has no mapping. */
    fun forCharacter(gridType: GridType, character: GridCharacter): String? = when (gridType) {
        GridType.STANDARD -> standard[character]
        GridType.EXPERT -> expert[character]
    }
}
