package com.safemode.safekeepingforffx.data.reference


/**
 * The 10 optional Jecht Spheres - "Movie Spheres" - that unlock Auron's Bushido Overdrives.
 *
 * Not all are literally Jecht Spheres; a few are Auron or Braska Spheres. The first is handed to
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
            sphere(1, "Macalania Woods", "Given automatically after you defeat Spherimorph. Triggers the explanation of what the spheres are."),
            sphere(2, "Besaid Village", "To the right of Besaid Temple. Hardest of the set.", caution = Caution.Guarded("Dark Valefor")),
            sphere(3, "S.S. Liki", "On the bridge, lying on the ground beside the ship's captain."),
            sphere(4, "Luca Stadium", "In the hallway outside the Besaid Aurochs' locker room (Stadium - Basement A)."),
            sphere(5, "Mi'ihen Highroad", "Oldroad South, beside the treasure chest down the path where O'aka XXIII was hiding."),
            sphere(6, "Mushroom Rock Road", "An Auron Sphere, at the top of the ridge where you met Gatta and Luzzu.", caution = Caution.Guarded("Dark Magus Sisters")),
            sphere(7, "Moonflow", "In the South Bank Wharf area."),
            sphere(8, "Thunder Plains", "In the south half, beside one of the lightning rod towers.", caution = Caution.Guarded("Dark Ixion")),
            sphere(9, "Macalania Woods", "Near the entrance area, off to the right of the screen. Last one reachable on the backtrack."),
            sphere(10, "Mt. Gagazet", "A Braska Sphere on the Mountain Trail, before the Fayth Cluster and the Caves. The last optional sphere in the game.", caution = Caution.Guarded("Dark Anima"))
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun sphere(
        number: Int,
        location: String,
        detail: String,
        caution: Caution? = null,
        shot: Int? = null
    ) = ReferenceItem(
        id = "jecht_%02d".format(number),
        title = "Sphere $number",
        location = location,
        detail = detail,
        caution = caution,
        imageRes = shot
    )
}
