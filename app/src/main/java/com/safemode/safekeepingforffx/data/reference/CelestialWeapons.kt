package com.safemode.safekeepingforffx.data.reference


/**
 * The seven Celestial (Ultimate) Weapons, one section per character.
 *
 * Each weapon needs three things: the weapon itself, its Crest and its Sigil. The weapon is inert
 * until both are applied at the glowing orb in Macalania Woods, so all three are tracked
 * separately. The Celestial Mirror gates every one of them and is listed first.
 *
 * Nothing here is permanently missable - but eight of these sit in areas a Dark Aeon claims once
 * you leave Bevelle, and several of those are endgame fights in their own right.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object CelestialWeapons {

    const val CATEGORY_ID = "celestial_weapons"

    private const val MIRROR = "Prerequisite"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Celestial Weapons",
        note = "Every weapon chest needs the Celestial Mirror, so get that first. Crests and " +
            "Sigils are applied at the glowing orb in Macalania Woods where the Mirror was made.",
        items = listOf(
            item("mirror_cloudy", MIRROR, "Cloudy Mirror", "Remiem Temple",
                "Finish all four Chocobo training challenges in the Calm Lands, then ride a chocobo to Remiem Temple and win the chocobo race.",
                stage = StoryStage.CALM_LANDS),
            item("mirror_celestial", MIRROR, "Celestial Mirror", "Macalania Woods",
                "Take the Cloudy Mirror to the family in Macalania Woods - speak to the wife, then the husband at the campsite, then the son further north.",
                stage = StoryStage.CALM_LANDS),

            item("tidus_weapon", "Tidus - Caladbolg", "Caladbolg", "Calm Lands",
                "Down the pathway in the northwest section of the Calm Lands. Requires the Celestial Mirror and all four chocobo training challenges.",
                stage = StoryStage.CALM_LANDS),
            item("tidus_crest", "Tidus - Caladbolg", "Sun Crest", "Zanarkand Ruins",
                "In the room where you fought Yunalesca. Descend the stairs after beating her to make the chest appear.",
                caution = Caution.Guarded("Dark Bahamut"),
                stage = StoryStage.ZANARKAND_RUINS),
            item("tidus_sigil", "Tidus - Caladbolg", "Sun Sigil", "Calm Lands",
                "Reward from the Chocobo Trainer for finishing the Catcher Chocobo race in 0:0:0 - the hardest sigil in the game.",
                stage = StoryStage.CALM_LANDS),

            item("yuna_weapon", "Yuna - Nirvana", "Nirvana", "Monster Arena, Calm Lands",
                "Capture one of every fiend in the Calm Lands for the Monster Arena owner; a chest then appears, opened with the Celestial Mirror.",
                stage = StoryStage.CALM_LANDS),
            item("yuna_crest", "Yuna - Nirvana", "Moon Crest", "Besaid Beach",
                "Swim to the chest just right of where Tidus washes ashore.",
                caution = Caution.Guarded("Dark Valefor"),
                stage = StoryStage.BESAID),
            item("yuna_sigil", "Yuna - Nirvana", "Moon Sigil", "Remiem Temple",
                "Defeat every one of Belgemine's aeons, including the three optional ones.",
                stage = StoryStage.AIRSHIP),

            item("auron_weapon", "Auron - Masamune", "Masamune", "Mushroom Rock Road",
                "Take the Rusty Sword from the Calm Lands gorge to the Statue of Lord Mi'ihen, press the button, then touch the glyph on the far wall.",
                caution = Caution.Guarded("Dark Magus Sisters"),
                stage = StoryStage.AIRSHIP),
            item("auron_crest", "Auron - Masamune", "Mars Crest", "Mi'ihen Highroad",
                "Rent a chocobo at Rin's Agency, ride to the North End, then southeast down the small path to the Oldroad. Chest at the end.",
                stage = StoryStage.MIIHEN_HIGHROAD),
            item("auron_sigil", "Auron - Masamune", "Mars Sigil", "Monster Arena, Calm Lands",
                "Reward for completing 10 Area and/or Species Conquests at the Monster Arena.",
                stage = StoryStage.AIRSHIP),

            item("wakka_weapon", "Wakka - World Champion", "World Champion", "Luca Square",
                "Place at least third in a Blitzball tournament, then speak to the café owner in Luca Square holding the Celestial Mirror.",
                stage = StoryStage.AIRSHIP),
            item("wakka_crest", "Wakka - World Champion", "Jupiter Crest", "Luca Stadium",
                "In the chest at the back of the Besaid Aurochs' locker room, available after the Aurochs' first match.",
                stage = StoryStage.LUCA),
            item("wakka_sigil", "Wakka - World Champion", "Jupiter Sigil", "Blitzball",
                "Awarded for winning Blitzball tournament matches - a long grind rather than a fixed location.",
                stage = StoryStage.AIRSHIP),

            item("lulu_weapon", "Lulu - Onion Knight", "Onion Knight", "Baaj Temple",
                "Defeat Geosgaeno at Baaj Temple; a chest appears in the southernmost stretch of water.",
                stage = StoryStage.AIRSHIP),
            item("lulu_crest", "Lulu - Onion Knight", "Venus Crest", "Guadosalam - Farplane",
                "In a chest on the left side of the Farplane. Available as soon as Seymour leaves Guadosalam.",
                stage = StoryStage.GUADOSALAM),
            item("lulu_sigil", "Lulu - Onion Knight", "Venus Sigil", "Thunder Plains",
                "Dodge 200 lightning bolts in a row - the Lightning Dodger side quest.",
                caution = Caution.Guarded("Dark Ixion"),
                stage = StoryStage.THUNDER_PLAINS),

            item("kimahri_weapon", "Kimahri - Spirit Lance", "Spirit Lance", "Thunder Plains",
                "Pray at the three glowing Qactuar Stones, then follow the Qactuar ghost to the leaning tower on the right and pray there.",
                caution = Caution.Guarded("Dark Ixion"),
                stage = StoryStage.AIRSHIP),
            item("kimahri_crest", "Kimahri - Spirit Lance", "Saturn Crest", "Mt. Gagazet",
                "In the passage through to Zanarkand, just past where you fought Seymour Flux.",
                caution = Caution.Guarded("Dark Anima"),
                stage = StoryStage.MT_GAGAZET),
            item("kimahri_sigil", "Kimahri - Spirit Lance", "Saturn Sigil", "Macalania Woods",
                "Complete the Butterfly Hunt - catch the blue butterflies, avoid the red ones, beat the timer.",
                stage = StoryStage.MACALANIA_WOODS),

            item("rikku_weapon", "Rikku - Godhand", "Godhand", "Mushroom Rock (airship)",
                "Enter the Al Bhed password GODHAND on the airship map screen to unlock the location, then open the chest with the Celestial Mirror.",
                caution = Caution.Guarded("Dark Magus Sisters"),
                stage = StoryStage.AIRSHIP),
            item("rikku_crest", "Rikku - Godhand", "Mercury Crest", "Bikanel Island",
                "Sanubia Desert - West. The chest sits inside the whirlpool to the west.",
                caution = Caution.Guarded("Dark Ifrit"),
                stage = StoryStage.BIKANEL_ISLAND),
            item("rikku_sigil", "Rikku - Godhand", "Mercury Sigil", "Village of the Cactuars",
                "Reward for completing the Village of the Cactuars side quest - track down all ten Cactuar stones.",
                stage = StoryStage.AIRSHIP)
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun item(
        key: String,
        section: String,
        title: String,
        location: String,
        detail: String,
        caution: Caution? = null,
        shot: Int? = null,
        stage: StoryStage
    ) = ReferenceItem(
        id = "celestial_$key",
        title = title,
        location = location,
        detail = detail,
        caution = caution,
        section = section,
        imageRes = shot,
        storyStage = stage
    )
}
