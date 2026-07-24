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
                stage = StoryStage.CALM_LANDS,
                stageNote = "Clear all four of the trainer's runs first, then ride south to Remiem and win the race."),
            item("mirror_celestial", MIRROR, "Celestial Mirror", "Macalania Woods",
                "Take the Cloudy Mirror to the family in Macalania Woods - speak to the wife, then the husband at the campsite, then the son further north.",
                stage = StoryStage.CALM_LANDS,
                stageNote = "Walk back south into Macalania Woods with the Cloudy Mirror in hand."),

            item("tidus_weapon", "Tidus - Caladbolg", "Caladbolg", "Calm Lands",
                "Down the pathway in the northwest section of the Calm Lands. Requires the Celestial Mirror and all four chocobo training challenges.",
                stage = StoryStage.CALM_LANDS,
                stageNote = "Once the Mirror and the chocobo training are done, ride into the northwest gorge."),
            item("tidus_crest", "Tidus - Caladbolg", "Sun Crest", "Zanarkand Ruins",
                "In the room where you fought Yunalesca. Descend the stairs after beating her to make the chest appear.",
                caution = Caution.Guarded("Dark Bahamut"),
                stage = StoryStage.ZANARKAND_RUINS,
                stageNote = "Turn straight back down the stairs after Yunalesca falls - the chest only appears then."),
            item("tidus_sigil", "Tidus - Caladbolg", "Sun Sigil", "Calm Lands",
                "Reward from the Chocobo Trainer for finishing the Catcher Chocobo race in 0:0:0 - the hardest sigil in the game.",
                stage = StoryStage.CALM_LANDS,
                stageNote = "Nothing gates this but the run itself. Budget a long sitting with the trainer."),

            item("yuna_weapon", "Yuna - Nirvana", "Nirvana", "Monster Arena, Calm Lands",
                "Capture one of every fiend in the Calm Lands for the Monster Arena owner; a chest then appears, opened with the Celestial Mirror.",
                stage = StoryStage.CALM_LANDS,
                stageNote = "Buy weapons with Capture from the arena owner, clear out every local fiend, then open the chest with the Mirror."),
            item("yuna_crest", "Yuna - Nirvana", "Moon Crest", "Besaid Beach",
                "Swim to the chest just right of where Tidus washes ashore.",
                caution = Caution.Guarded("Dark Valefor"),
                stage = StoryStage.BESAID,
                stageNote = "Swim out on your very first visit - nothing in the story gates it."),
            item("yuna_sigil", "Yuna - Nirvana", "Moon Sigil", "Remiem Temple",
                "Defeat every one of Belgemine's aeons, including the three optional ones.",
                stage = StoryStage.AIRSHIP,
                stageNote = "Collect every optional aeon first: Anima from Baaj, the Magus Sisters from the Blossom Crown and Flower Scepter, Yojimbo from the Cavern of the Stolen Fayth."),

            item("auron_weapon", "Auron - Masamune", "Masamune", "Mushroom Rock Road",
                "Take the Rusty Sword from the Calm Lands gorge to the Statue of Lord Mi'ihen, press the button, then touch the glyph on the far wall.",
                caution = Caution.Guarded("Dark Magus Sisters"),
                stage = StoryStage.AIRSHIP,
                stageNote = "Collect the Rusty Sword from the Calm Lands chasm, then fly back - Mushroom Rock is unreachable on foot by now."),
            item("auron_crest", "Auron - Masamune", "Mars Crest", "Mi'ihen Highroad",
                "Rent a chocobo at Rin's Agency, ride to the North End, then southeast down the small path to the Oldroad. Chest at the end.",
                stage = StoryStage.MIIHEN_HIGHROAD,
                stageNote = "Rent a chocobo at Rin's Agency as you pass through and detour onto the Oldroad."),
            item("auron_sigil", "Auron - Masamune", "Mars Sigil", "Monster Arena, Calm Lands",
                "Reward for completing 10 Area and/or Species Conquests at the Monster Arena.",
                stage = StoryStage.AIRSHIP,
                stageNote = "Ten conquests needs more arena areas than you can reach before the airship."),

            item("wakka_weapon", "Wakka - World Champion", "World Champion", "Luca Square",
                "Place at least third in a Blitzball tournament, then speak to the café owner in Luca Square holding the Celestial Mirror.",
                stage = StoryStage.AIRSHIP,
                stageNote = "Luca is sealed off from the Calm Lands onward, so this waits for the airship even though the tournament itself does not."),
            item("wakka_crest", "Wakka - World Champion", "Jupiter Crest", "Luca Stadium",
                "In the chest at the back of the Besaid Aurochs' locker room, available after the Aurochs' first match.",
                stage = StoryStage.LUCA,
                stageNote = "Duck into the Aurochs' locker room once their first match is over."),
            item("wakka_sigil", "Wakka - World Champion", "Jupiter Sigil", "Blitzball",
                "Awarded for winning Blitzball tournament matches - a long grind rather than a fixed location.",
                stage = StoryStage.LUCA,
                stageNote = "Blitzball opens up at save spheres the moment Luca ends. The Sigil only joins the prize pool after you have played a fair number of matches, so start early."),

            item("lulu_weapon", "Lulu - Onion Knight", "Onion Knight", "Baaj Temple",
                "Defeat Geosgaeno at Baaj Temple; a chest appears in the southernmost stretch of water.",
                stage = StoryStage.AIRSHIP,
                stageNote = "Baaj is airship-only. The chest is not in the water until Geosgaeno is dead, so do not go looking first."),
            item("lulu_crest", "Lulu - Onion Knight", "Venus Crest", "Guadosalam - Farplane",
                "In a chest on the left side of the Farplane. Available as soon as Seymour leaves Guadosalam, but Guadosalam itself is sealed off from the moment you reach the Calm Lands until you have the airship.",
                stage = StoryStage.GUADOSALAM,
                stageNote = "Take the Farplane detour before you move on. Miss it and Guadosalam stays shut until the airship."),
            item("lulu_sigil", "Lulu - Onion Knight", "Venus Sigil", "Thunder Plains",
                "Dodge 200 lightning bolts in a row - the Lightning Dodger side quest.",
                caution = Caution.Guarded("Dark Ixion"),
                stage = StoryStage.THUNDER_PLAINS,
                stageNote = "The counter runs from your first crossing. Nothing gates it but your nerve."),

            item("kimahri_weapon", "Kimahri - Spirit Lance", "Spirit Lance", "Thunder Plains",
                "Pray at the three glowing Qactuar Stones, then follow the Qactuar ghost to the leaning tower on the right and pray there.",
                caution = Caution.Guarded("Dark Ixion"),
                stage = StoryStage.CALM_LANDS,
                stageNote = "Backtrack south out of the Calm Lands to the Thunder Plains, pray at all three stones, then chase the ghost to the leaning tower."),
            item("kimahri_crest", "Kimahri - Spirit Lance", "Saturn Crest", "Mt. Gagazet",
                "In the passage through to Zanarkand, just past where you fought Seymour Flux.",
                caution = Caution.Guarded("Dark Anima"),
                stage = StoryStage.MT_GAGAZET,
                stageNote = "Sits right on the path past Seymour Flux. Easy to walk straight by on the way to Zanarkand."),
            item("kimahri_sigil", "Kimahri - Spirit Lance", "Saturn Sigil", "Macalania Woods",
                "Complete the Butterfly Hunt - catch the blue butterflies, avoid the red ones, beat the timer.",
                stage = StoryStage.AIRSHIP,
                stageNote = "The hunt is playable from your first visit to the woods, but it only pays out the Sigil once you have the airship. Doing it early wins you nothing but practice."),

            item("rikku_weapon", "Rikku - Godhand", "Godhand", "Mushroom Rock (airship)",
                "Enter the Al Bhed password GODHAND on the airship map screen to unlock the location, then open the chest with the Celestial Mirror.",
                caution = Caution.Guarded("Dark Magus Sisters"),
                stage = StoryStage.AIRSHIP,
                stageNote = "Type GODHAND into the airship's map search to make the destination exist at all."),
            item("rikku_crest", "Rikku - Godhand", "Mercury Crest", "Bikanel Island",
                "Sanubia Desert - West. The chest sits inside the whirlpool to the west.",
                caution = Caution.Guarded("Dark Ifrit"),
                stage = StoryStage.BIKANEL_ISLAND,
                stageNote = "Step into the western whirlpool while you are crossing the desert for the story."),
            item("rikku_sigil", "Rikku - Godhand", "Mercury Sigil", "Village of the Cactuars",
                "Reward for completing the Village of the Cactuars side quest - track down all ten Cactuar stones.",
                stage = StoryStage.AIRSHIP,
                stageNote = "The cactuar hunt only opens on a return trip to Bikanel, not the story visit.")
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
        stage: StoryStage,
        stageNote: String
    ) = ReferenceItem(
        id = "celestial_$key",
        title = title,
        location = location,
        detail = detail,
        caution = caution,
        section = section,
        imageRes = shot,
        storyStage = stage,
        stageNote = stageNote
    )
}
