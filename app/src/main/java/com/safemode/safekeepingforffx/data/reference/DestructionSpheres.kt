package com.safemode.safekeepingforffx.data.reference


/**
 * The six Destruction Sphere treasures, one hidden in each temple's Cloister of Trials.
 *
 * These are the gate to Anima: the hidden aeon at Baaj Temple only appears once all six chests have
 * been opened. None are permanently missable - every temple is reachable again from the airship, and
 * the Bevelle chest is handed to you during the story - but from the International release on, a Dark
 * Aeon parks itself at Besaid and at the Macalania Temple entrance, so those two get expensive to go
 * back for.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object DestructionSpheres {

    const val CATEGORY_ID = "destruction_spheres"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Destruction Spheres",
        note = "Open all six and Anima becomes available at Baaj Temple. Solve each Cloister so the " +
            "Destruction Sphere is the last thing you place, or you can seal the treasure in until " +
            "you come back.",
        items = listOf(
            sphere("besaid", "Besaid Temple", "Rod of Wisdom",
                caution = Caution.Guarded("Dark Valefor"), stage = StoryStage.BESAID),
            sphere("kilika", "Kilika Temple", "Red Armlet"),
            sphere("djose", "Djose Temple", "Magic Sphere"),
            sphere("macalania", "Macalania Temple", "Luck Sphere",
                caution = Caution.Guarded("Dark Shiva"), stage = StoryStage.LAKE_MACALANIA),
            sphere("bevelle", "Bevelle Temple", "HP Sphere",
                "Can't be missed - it's a requirement for completing the trial."),
            sphere("zanarkand", "Zanarkand Dome", "Magistral Rod",
                "Not available on your first trip through Zanarkand - come back with the airship " +
                    "to get it.")
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun sphere(
        key: String,
        location: String,
        title: String,
        // Blank unless there is something to know about getting it - the row hides an empty detail.
        detail: String = "",
        caution: Caution? = null,
        // Only the guarded temples carry one, so the Missables Timeline can place them. Staging all
        // six would also switch on the story-order sort for this list.
        stage: StoryStage? = null
    ) = ReferenceItem(
        id = "destruction_$key",
        title = title,
        location = location,
        detail = detail,
        caution = caution,
        tag = "Treasure",
        storyStage = stage
    )
}
