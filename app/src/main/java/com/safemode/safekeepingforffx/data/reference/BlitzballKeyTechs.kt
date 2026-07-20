package com.safemode.safekeepingforffx.data.reference

/**
 * The three Key Techniques belonging to each Besaid Aurochs player.
 *
 * Key Techs are not a global list: every player has their own three, and each one learned opens up
 * more of that player's ordinary tech slots. Tidus cannot reach Jecht Shot 2, and Wakka cannot
 * reach Aurochs Spirit, until all three of their own Key Techs are learned.
 *
 * They are learned with Techcopy, by marking an opposing player who already has the technique and
 * waiting for them to use it.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object BlitzballKeyTechs {

    const val CATEGORY_ID = "blitzball_key_techs"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Blitzball Key Techs",
        showOnHome = false,
        note = "Learned with Techcopy: mark an opponent who already has the tech and wait for " +
            "them to use it. Status techniques only land under specific conditions - shots only " +
            "if they fail to score, passes only if a defender touches the ball without catching " +
            "it, and tackles on any contact.",
        items = listOf(
            // Tidus
            tech(
                "tidus", 1, "Tidus", "Venom Tackle",
                "Tackling the ball carrier poisons them. Poison speeds up their HP loss while " +
                    "they hold the ball and stops them recovering HP."
            ),
            tech(
                "tidus", 2, "Tidus", "Drain Tackle 2",
                "Tackling absorbs HP from the carrier and adds it to Tidus. It only drains if " +
                    "he already has at least as much HP as the tackle would take."
            ),
            tech(
                "tidus", 3, "Tidus", "Anti Venom 2",
                "Blocks poison outright. The tier 2 version is a full 100% block rather than the " +
                    "50% of plain Anti Venom."
            ),

            // Wakka
            tech(
                "wakka", 1, "Wakka", "Wither Shot",
                "A shot that halves one of the goalie's attributes if it fails to score. The " +
                    "withered stat shows blue and recovers over time or at the end of the half."
            ),
            tech(
                "wakka", 2, "Wakka", "Drain Tackle",
                "Tackling absorbs HP from the ball carrier."
            ),
            tech(
                "wakka", 3, "Wakka", "Tackle Slip",
                "Roughly a 40% chance to slip past a tackle instead of losing the ball, though " +
                    "he may be left disoriented afterwards."
            ),

            // Botta
            tech(
                "botta", 1, "Botta", "Venom Shot",
                "A shot that poisons the goalie, but only if the shot fails to score."
            ),
            tech(
                "botta", 2, "Botta", "Venom Pass 2",
                "A pass that poisons a defender who touches the ball without catching it."
            ),
            tech(
                "botta", 3, "Botta", "Nap Tackle",
                "Tackling puts the ball carrier to sleep. They wake after a while, when a goal " +
                    "is scored, or if a pass hits them."
            ),

            // Datto
            tech(
                "datto", 1, "Datto", "Wither Shot",
                "A shot that halves one of the goalie's attributes if it fails to score."
            ),
            tech(
                "datto", 2, "Datto", "Anti Venom",
                "Roughly a 50% chance to block poison."
            ),
            tech(
                "datto", 3, "Datto", "Wither Shot 2",
                "A stronger Wither Shot: costs more HP but lands the stat drop more reliably."
            ),

            // Jassu
            tech(
                "jassu", 1, "Jassu", "Wither Tackle",
                "Tackling halves one of the ball carrier's attributes."
            ),
            tech(
                "jassu", 2, "Jassu", "Wither Tackle 2",
                "A stronger Wither Tackle: costs more HP but lands more reliably."
            ),
            tech(
                "jassu", 3, "Jassu", "Nap Tackle 2",
                "A stronger Nap Tackle, more likely to put the carrier to sleep."
            ),

            // Keepa
            tech(
                "keepa", 1, "Keepa", "Super Goalie",
                "Roughly a 60% chance to add a random bonus to the goalie's Catch rating when " +
                    "facing a shot. Keepa is the Aurochs' keeper, so this is his best trait."
            ),
            tech(
                "keepa", 2, "Keepa", "Volley Shot",
                "Roughly a 50% chance to shoot a loose ball out of the air unblocked."
            ),
            tech(
                "keepa", 3, "Keepa", "Anti Wither",
                "Roughly a 50% chance to prevent his attributes being halved by a Wither move."
            ),

            // Letty
            tech(
                "letty", 1, "Letty", "Wither Tackle",
                "Tackling halves one of the ball carrier's attributes."
            ),
            tech(
                "letty", 2, "Letty", "Nap Pass",
                "A pass that puts a defender to sleep if they touch the ball without catching it."
            ),
            tech(
                "letty", 3, "Letty", "Venom Pass 3",
                "The strongest Venom Pass: the most reliable at poisoning a defender who touches " +
                    "the ball without catching it."
            )
        )
    )

    val items: List<ReferenceItem> get() = category.items

    // A `when` rather than a map property: object initialisation runs top-down, and `category`
    // above would read a not-yet-initialised map.
    private fun ordinal(order: Int) = when (order) {
        1 -> "First"
        2 -> "Second"
        else -> "Third"
    }

    private fun tech(
        player: String,
        order: Int,
        section: String,
        title: String,
        effect: String
    ) = ReferenceItem(
        id = "blitz_${player}_$order",
        title = title,
        location = "${ordinal(order)} key technique",
        detail = effect,
        section = section
    )
}
