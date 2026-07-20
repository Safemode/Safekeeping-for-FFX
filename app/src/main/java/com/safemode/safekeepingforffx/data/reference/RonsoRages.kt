package com.safemode.safekeepingforffx.data.reference

/**
 * Kimahri's Overdrive: 12 Ronso Rage abilities, learned by using Lancet on specific fiends.
 *
 * Unlike the other categories these are not things lying on the floor somewhere, so there are no
 * location screenshots. What matters is which fiend to Lancet, where that fiend lives, and what
 * the ability actually does.
 *
 * Learning one immediately fills Kimahri's Overdrive gauge, so you get a free Overdrive each time.
 *
 * Biran teaches Self-Destruct, Thrust Kick, Doom and Mighty Guard; Yenke teaches Fire Breath,
 * Stone Breath, Aqua Breath and White Wind. Seed Cannon, Bad Breath and Nova are not available
 * from either of them.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object RonsoRages {

    const val CATEGORY_ID = "ronso_rages"

    private const val KNOWN = "Known from the start"
    private const val LANCET = "Learned with Lancet"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Ronso Rage",
        note = "Biran and Yenke Ronso between them teach eight of these during the mandatory solo " +
            "fight on Mt. Gagazet, so that battle is the cheapest way to fill most of the list. " +
            "Seed Cannon, Bad Breath and Nova have to come from ordinary fiends.",
        items = listOf(
            rage(
                "jump", KNOWN, "Jump",
                "No Lancet needed",
                "Deals damage to one enemy. Kimahri already knows this at the start of the game."
            ),
            rage(
                "seed_cannon", LANCET, "Seed Cannon",
                "Ragora (Kilika Woods)",
                "Deals damage to one enemy. Taught during the Lancet tutorial in Kilika Woods."
            ),
            rage(
                "self_destruct", LANCET, "Self-Destruct",
                "Bomb (Mi'ihen Highroad); Grenade (Mt. Gagazet); Puroboros (Omega Ruins); " +
                    "Biran Ronso (Mt. Gagazet fight)",
                "Deals heavy damage to one enemy but KOs Kimahri."
            ),
            rage(
                "thrust_kick", LANCET, "Thrust Kick",
                "YKT-63 (Bevelle); YKT-11 (Zanarkand); Biran Ronso (Mt. Gagazet fight)",
                "Damages one enemy and has a chance to eject them from battle."
            ),
            rage(
                "fire_breath", LANCET, "Fire Breath",
                "Dual Horn (Mi'ihen Highroad); Valaha (Cavern of the Stolen Fayth); " +
                    "Grendel (Mt. Gagazet); Yenke Ronso (Mt. Gagazet fight)",
                "Deals fire damage to all enemies."
            ),
            rage(
                "stone_breath", LANCET, "Stone Breath",
                "Basilisk (Djose Highroad); Anacondaur (Calm Lands); " +
                    "Yenke Ronso (Mt. Gagazet fight)",
                "Chance to petrify all enemies."
            ),
            rage(
                "aqua_breath", LANCET, "Aqua Breath",
                "Chimera (Macalania Woods); Chimera Brain (Calm Lands); " +
                    "Yenke Ronso (Mt. Gagazet fight)",
                "Deals water damage to all enemies."
            ),
            rage(
                "doom", LANCET, "Doom",
                "Ghost (Cavern of the Stolen Fayth); Wraith (Omega Ruins); " +
                    "Biran Ronso (Mt. Gagazet fight)",
                "Chance to inflict Doom on one enemy. It dies when the timer reaches zero."
            ),
            rage(
                "white_wind", LANCET, "White Wind",
                "Dark Flan (Mt. Gagazet); Spirit (Omega Ruins); Yenke Ronso (Mt. Gagazet fight)",
                "Restores HP to the entire party. Yenke must use it first before you can Lancet it."
            ),
            rage(
                "bad_breath", LANCET, "Bad Breath",
                "Malboro (Calm Lands, Cavern of the Stolen Fayth); " +
                    "Great Malboro (Omega Ruins, Inside Sin)",
                "Chance to inflict multiple status effects on all enemies. Not taught by Biran " +
                    "or Yenke."
            ),
            rage(
                "mighty_guard", LANCET, "Mighty Guard",
                "Behemoth (Mt. Gagazet, Zanarkand); Behemoth King (Inside Sin); " +
                    "Biran Ronso (Mt. Gagazet fight)",
                "Casts Protect, Shell, and NulAll on the entire party. Biran must use it first " +
                    "before you can Lancet it."
            ),
            rage(
                "nova", LANCET, "Nova",
                "Omega Weapon (Omega Ruins); Nemesis (Monster Arena)",
                "Deals heavy non-elemental damage to all enemies. Not taught by Biran or Yenke."
            )
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun rage(
        key: String,
        section: String,
        title: String,
        source: String,
        effect: String
    ) = ReferenceItem(
        id = "ronso_$key",
        title = title,
        location = source,
        detail = effect,
        section = section
    )
}
