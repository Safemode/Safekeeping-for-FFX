package com.safemode.safekeepingforffx.data.reference


/**
 * The Final Fantasy X trophy/achievement set from the HD Remaster (and the PS3/PS4/Switch/PC ports
 * that share it). Not the FFX-2 list.
 *
 * Kept off the Home summary ([showOnHome] = false): it overlaps heavily with the other lists - all
 * primers, all aeons, all celestial weapons each have their own category already - so counting it on
 * Home too would double-report the same work. It still has its own screen, checkboxes and progress.
 *
 * The trophy's rough type is in [ReferenceItem.location] as a quick filter; the condition is in
 * [detail].
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object Trophies {

    const val CATEGORY_ID = "trophies"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Trophies",
        note = "Only applies to the HD Remaster.",
        showOnHome = false,
        // Its condition is literally "every other trophy", so it keeps itself in step with them.
        completionItemId = "trophy_completion",
        items = listOf(
            trophy("completion", "Completion", "Platinum",
                "Obtain all other Final Fantasy X trophies."),
            trophy("speaking_in_tongues", "Speaking in Tongues", "Collection",
                "Find one Al Bhed Primer."),
            trophy("teamwork", "Teamwork!", "Blitzball",
                "Win a Blitzball match."),
            trophy("the_right_thing", "The Right Thing", "Story",
                "Clear the Besaid Cloister of Trials."),
            trophy("talent_for_acquisition", "A Talent for Acquisition", "Grind",
                "Steal successfully with Rikku 200 times."),
            trophy("all_together", "All Together", "Story",
                "Have all party members join the group."),
            trophy("heartstrings", "Heartstrings", "Story",
                "View the underwater date scene in Macalania Woods."),
            trophy("show_off", "Show Off!", "Blitzball",
                "Win a Blitzball tournament."),
            trophy("striker", "Striker", "Blitzball",
                "Learn the Jecht Shot."),
            trophy("chocobo_license", "Chocobo License", "Side quest",
                "Pass all Chocobo training in the Calm Lands."),
            trophy("overcoming_the_past", "Overcoming the Past", "Story",
                "Defeat Yunalesca."),
            trophy("destination_of_hatred", "The Destination of Hatred", "Story",
                "Defeat Seymour Omnis."),
            trophy("lightning_dancer", "Lightning Dancer", "Side quest",
                "Dodge 200 lightning strikes and claim the reward."),
            trophy("feel_the_pain", "Feel the Pain", "Aeon",
                "Obtain Anima."),
            trophy("all_about_money", "It's All About the Money", "Aeon",
                "Obtain Yojimbo."),
            trophy("delta_attack", "Delta Attack!", "Aeon",
                "Obtain the Magus Sisters."),
            trophy("theater_enthusiast", "Theater Enthusiast", "Collection",
                "Buy every sphere at the Luca Sphere Theater."),
            trophy("chocobo_rider", "Chocobo Rider", "Side quest",
                "Win a Catcher Chocobo race with a time of 0:0:0."),
            trophy("power_strike", "Power Strike", "Battle",
                "Deal 9999 or more damage in a single attack."),
            trophy("under_the_table", "Under the Table", "Grind",
                "Spend 100,000 gil or more on bribes."),
            trophy("messenger_from_past", "Messenger from the Past", "Collection",
                "Obtain all Jecht Spheres."),
            trophy("mega_strike", "Mega Strike", "Battle",
                "Deal 99999 damage with one attack."),
            trophy("chocobo_master", "Chocobo Master", "Side quest",
                "Collect all five treasure chests during the Remiem Temple race and win it."),
            trophy("sphere_master", "Sphere Master", "Sphere Grid",
                "Complete a Sphere Grid for one character."),
            trophy("blitzball_master", "Blitzball Master", "Blitzball",
                "Unlock all Blitzball slot reels."),
            trophy("learning", "Learning!", "Collection",
                "Learn to use all enemy abilities (Kimahri's Ronso Rages)."),
            trophy("summon_master", "Summon Master", "Aeon",
                "Obtain all aeons."),
            trophy("weapon_master", "Weapon Master", "Collection",
                "Obtain all Celestial Weapons."),
            trophy("master_linguist", "Master Linguist", "Collection",
                "Find all 26 Al Bhed Primers."),
            trophy("perfect_sphere_master", "Perfect Sphere Master", "Sphere Grid",
                "Complete the Sphere Grids for all main characters."),
            trophy("perseverance", "Perseverance", "Superboss",
                "Defeat Penance."),
            trophy("overcoming_the_nemesis", "Overcoming the Nemesis", "Superboss",
                "Defeat Nemesis in the Monster Arena."),
            trophy("the_eternal_calm", "The Eternal Calm", "Story",
                "Defeat Yu Yevon and finish the game."),
            trophy("journeys_catalyst", "A Journey's Catalyst", "Story",
                "View the \"Eternal Calm\" cinematic.")
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun trophy(
        key: String,
        title: String,
        location: String,
        detail: String
    ) = ReferenceItem(
        id = "trophy_$key",
        title = title,
        location = location,
        detail = detail,
        tag = "Trophy"
    )
}
