package com.safemode.safekeepingforffx.data.reference

/**
 * Where the game drops each character onto the grid: the blank node they occupy before they have
 * activated anything. Used to frame the planner's opening view on a character who has no path yet,
 * so the canvas lands on their corner of the grid instead of the whole board zoomed out.
 *
 * These are vanilla positions, so they are fixed node ids rather than something derived from the
 * node's current content - a player edit that turns a neighbour into an ability must not move
 * anyone's starting point.
 *
 * Only the Standard grid is mapped. The Expert grid redistributes everything and its starting
 * cluster is not recorded here, so it keeps the fit-the-whole-grid opening view.
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

    /** The node [character] starts on for [gridType], or null when that grid has no mapping. */
    fun forCharacter(gridType: GridType, character: GridCharacter): String? = when (gridType) {
        GridType.STANDARD -> standard[character]
        GridType.EXPERT -> null
    }
}
