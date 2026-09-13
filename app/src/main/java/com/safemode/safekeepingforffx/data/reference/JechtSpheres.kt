package com.safemode.safekeepingforffx.data.reference


/**
 * The 10 optional Jecht Spheres - "Movie Spheres" - that unlock Auron's Bushido Overdrives.
 *
 * Two of them are not Jecht's: one is Auron's and one is Braska's, and each is named for whose it
 * is. The eight Jecht Spheres are numbered among themselves, so the count has no gaps where those
 * two sit. Titles are display only - ids stay keyed to list position. The first is handed to
 * you automatically after Spherimorph; the rest sit in areas you have already passed through, so
 * the practical approach is to backtrack from Macalania Woods to Besaid immediately after that
 * fight. Listed here in the order you meet them travelling *forwards*.
 *
 * None are permanently missable, but four sit in areas a Dark Aeon claims after Bevelle.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object JechtSpheres {

    const val CATEGORY_ID = "jecht_spheres"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Jecht Spheres",
        note = "Most of these sit in areas you have already left. Backtrack to Besaid straight " +
            "after beating Spherimorph in Macalania Woods.",
        darkAeonNote = "Leave it any later and you'll be fighting Dark Aeons for several of them.",
        items = listOf(
            sphere(1, "Jecht's Sphere 1", "Macalania Woods", "Given automatically after you defeat Spherimorph. Triggers the explanation of what the spheres are."),
            sphere(2, "Jecht's Sphere 2", "Besaid Village", "To the right of Besaid Temple. Hardest of the set.", caution = Caution.Guarded("Dark Valefor"),
                stage = StoryStage.MACALANIA_WOODS,
                stageNote = "Only appears once Spherimorph is beaten, so this means backtracking all the way to Besaid."),
            sphere(3, "Jecht's Sphere 3", "S.S. Liki", "On the bridge, lying on the ground beside the ship's captain."),
            sphere(4, "Jecht's Sphere 4", "Luca Stadium", "In the hallway outside the Besaid Aurochs' locker room (Stadium - Basement A)."),
            sphere(5, "Jecht's Sphere 5", "Mi'ihen Highroad", "Oldroad South, beside the treasure chest down the path where O'aka XXIII was hiding."),
            sphere(6, "Auron's Sphere", "Mushroom Rock Road", "At the top of the ridge where you met Gatta and Luzzu.", caution = Caution.Guarded("Dark Magus Sisters"),
                stage = StoryStage.MACALANIA_WOODS,
                stageNote = "Only appears once Spherimorph is beaten, so backtrack to Mushroom Rock Road for it."),
            sphere(7, "Jecht's Sphere 6", "Moonflow", "In the South Bank Wharf area."),
            sphere(8, "Jecht's Sphere 7", "Thunder Plains", "In the south half, beside one of the lightning rod towers.", caution = Caution.Guarded("Dark Ixion"),
                stage = StoryStage.MACALANIA_WOODS,
                stageNote = "Only appears once Spherimorph is beaten. The Thunder Plains are just back south of the woods."),
            sphere(9, "Jecht's Sphere 8", "Macalania Woods", "Near the entrance area, off to the right of the screen. Last one reachable on the backtrack."),
            sphere(10, "Braska's Sphere", "Mt. Gagazet", "On the Mountain Trail, before the Fayth Cluster and the Caves. The last optional sphere in the game.", caution = Caution.Guarded("Dark Anima"),
                stage = StoryStage.MT_GAGAZET)
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun sphere(
        number: Int,
        title: String,
        location: String,
        detail: String,
        caution: Caution? = null,
        shot: Int? = null,
        // Set on the flagged spheres so the Missables Timeline can place them. A stage is when the
        // game first allows a sphere, which for all but the first and last is after Spherimorph -
        // not when you first walk past where it sits.
        stage: StoryStage? = null,
        stageNote: String? = null
    ) = ReferenceItem(
        id = "jecht_%02d".format(number),
        title = title,
        location = location,
        detail = detail,
        caution = caution,
        imageRes = shot,
        storyStage = stage,
        stageNote = stageNote
    )
}
