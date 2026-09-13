package com.safemode.safekeepingforffx.data.reference


/**
 * The eight Dark Aeons and Penance - the optional superbosses added from the International release
 * on. They do not exist in the original North American PS2 release, which is why the category note
 * says so: a player on that version can tick nothing here and has missed nothing.
 *
 * Penance only appears once all eight Dark Aeons are down (Dark Yojimbo has to be beaten five times
 * in a row), so it is listed last.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object DarkAeonsAndPenance {

    const val CATEGORY_ID = "dark_aeons_penance"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Dark Aeons & Penance",
        requiresDarkAeons = true,
        note = "These exist only in the International, PAL and HD Remaster versions, not the " +
            "original NA PS2 release. Clear all eight Dark Aeons and Penance appears in the Calm Lands.",
        items = listOf(
            boss("valefor", "Dark Valefor", "Besaid Village",
                "Ambushes you the moment you step into Besaid Village."),
            boss("ifrit", "Dark Ifrit", "Sanubia Desert",
                "In the northern part of the desert, where the Al Bhed Home once stood."),
            boss("ixion", "Dark Ixion", "Thunder Plains",
                "Roams the Thunder Plains and turns up after a few encounters."),
            boss("shiva", "Dark Shiva", "Lake Macalania",
                "Guards the entrance to Macalania Temple."),
            boss("bahamut", "Dark Bahamut", "Zanarkand Ruins",
                "In the room where you fought Yunalesca."),
            boss("anima", "Dark Anima", "Mt. Gagazet",
                "Appears on Mt. Gagazet after a set series of steps."),
            boss("yojimbo", "Dark Yojimbo", "Cavern of the Stolen Fayth",
                "In the same cavern as Yojimbo. Must be beaten five times in a row, without leaving, " +
                    "to count toward Penance."),
            boss("magus_sisters", "Dark Magus Sisters", "Mushroom Rock",
                "Fought in the Mushroom Rock valley, one sister at a time if you outrun the others."),
            boss("penance", "Penance", "Calm Lands (airship)",
                "The toughest fight in the game. Appears in the Calm Lands only after all eight Dark " +
                    "Aeons have been defeated.")
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun boss(
        key: String,
        title: String,
        location: String,
        detail: String
    ) = ReferenceItem(
        id = "dark_$key",
        title = title,
        location = location,
        detail = detail
    )
}
