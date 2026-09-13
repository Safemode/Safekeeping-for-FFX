package com.safemode.safekeepingforffx.data.reference


/**
 * The aeons Yuna can summon: the five obtained automatically on the pilgrimage, and the three
 * optional ones you have to go out of your way for.
 *
 * The standard five are unmissable - the story hands them to you at each temple. The optional three
 * are the real checklist: Anima and Yojimbo can be fetched once you reach the Calm Lands, and the
 * Magus Sisters need both of them plus two key items first.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object Aeons {

    const val CATEGORY_ID = "aeons"

    private const val STANDARD = "Standard"
    private const val OPTIONAL = "Optional"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Aeons",
        note = "The five standard aeons are given to you at each temple. The three optional ones " +
            "are the ones to plan for - and the Magus Sisters need Anima and Yojimbo first.",
        items = listOf(
            aeon("valefor", STANDARD, "Valefor", "Besaid Temple",
                "Received automatically after clearing the Besaid Cloister of Trials, early in the story."),
            aeon("ifrit", STANDARD, "Ifrit", "Kilika Temple",
                "Received automatically after clearing the Kilika Cloister of Trials."),
            aeon("ixion", STANDARD, "Ixion", "Djose Temple",
                "Received automatically after clearing the Djose Cloister of Trials."),
            aeon("shiva", STANDARD, "Shiva", "Macalania Temple",
                "Received automatically after clearing the Macalania Cloister of Trials."),
            aeon("bahamut", STANDARD, "Bahamut", "Bevelle Temple",
                "Received during the story in Bevelle. Cannot be missed."),

            aeon("yojimbo", OPTIONAL, "Yojimbo", "Cavern of the Stolen Fayth",
                "In the cavern off the Calm Lands. He has to be recruited by paying gil in the " +
                    "negotiation - offer generously and you can bring him aboard for somewhere " +
                    "around 190,000 to 250,000 gil. You then pay him each battle to act."),
            aeon("anima", OPTIONAL, "Anima", "Baaj Temple",
                "Reach Baaj Temple with the airship search command and beat Geosgaeno. Anima only " +
                    "appears once you have opened the Destruction Sphere treasure in all six " +
                    "Cloisters of Trials: Besaid, Kilika, Djose, Macalania, Bevelle and Zanarkand."),
            aeon("magus_sisters", OPTIONAL, "Magus Sisters", "Remiem Temple",
                "In the Chamber of the Fayth behind Belgemine. The sealed door needs the Flower " +
                    "Scepter (beat Belgemine's Bahamut at Remiem) and the Blossom Crown (capture " +
                    "every fiend in the Mt. Gagazet area for the Monster Arena). Obtain Anima and " +
                    "Yojimbo first.")
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun aeon(
        key: String,
        section: String,
        title: String,
        location: String,
        detail: String
    ) = ReferenceItem(
        id = "aeon_$key",
        title = title,
        location = location,
        detail = detail,
        section = section
    )
}
