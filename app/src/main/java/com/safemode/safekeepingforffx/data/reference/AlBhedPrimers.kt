package com.safemode.safekeepingforffx.data.reference


/**
 * The 26 Al Bhed Primers, in the order they become available.
 *
 * Four are permanently [Caution.Missable]: XIX, XX and XXI vanish with Home, and XXII is behind
 * Bevelle, which you cannot re-enter. Several others are [Caution.Guarded] - reachable, but a Dark
 * Aeon moved in after Bevelle. A handful of the early ones (I, III, V, XIV) can be picked up again
 * in the Bikanel Desert if missed the first time, so they carry no warning at all.
 *
 * Location screenshots have been pulled out for now; see `for-later/RESTORING-SCREENSHOTS.md`.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object AlBhedPrimers {

    const val CATEGORY_ID = "al_bhed_primers"

    val category by lazy {
        ChecklistCategory(id = CATEGORY_ID, label = "Al Bhed Primers", items = items)
    }

    val items: List<ReferenceItem> = listOf(
        primer(1, "I", "Salvage Ship", "At the rear of the ship. If missed, it reappears in the Bikanel Desert near where you reunite with Wakka."),
        primer(2, "II", "Besaid Village", "Inside the Crusaders' tent.", caution = Caution.Guarded("Dark Valefor")),
        primer(3, "III", "S.S. Liki", "In the engine room, with the chocobos. Fallback: Bikanel Desert."),
        primer(4, "IV", "Kilika", "In one of the huts, next to Kulukan."),
        primer(5, "V", "S.S. Winno", "On the bridge. Fallback: Bikanel Desert."),
        primer(6, "VI", "Luca Stadium", "In the locker room opposite the Besaid Aurochs' own."),
        primer(7, "VII", "Luca Sphere Theatre", "Near the stairs leading up to the theatre."),
        primer(8, "VIII", "Rin's Travel Agency, Mi'ihen", "Given to you automatically by Rin."),
        primer(9, "IX", "Mi'ihen Highroad", "On the trail just after you leave Rin's Travel Agency."),
        primer(10, "X", "Mushroom Rock Road", "Down a secret pathway in the Valley area.", caution = Caution.Guarded("Dark Magus Sisters")),
        primer(11, "XI", "Djose Highroad", "Tucked behind a large pillar."),
        primer(12, "XII", "Moonflow, North Wharf", "On the far bank, after riding the shoopuf."),
        primer(13, "XIII", "Guadosalam", "Inside one of the houses."),
        primer(14, "XIV", "Thunder Plains Agency", "From Rin - tell him your study is going \"Okay\". Fallback: Bikanel Desert.", caution = Caution.Guarded("Dark Ixion")),
        primer(15, "XV", "Macalania Woods", "Near the save sphere, by where O'aka XXIII stands."),
        primer(16, "XVI", "Lake Macalania", "To the left of Rin's Travel Agency.", caution = Caution.Guarded("Dark Shiva")),
        primer(17, "XVII", "Bikanel Desert", "Sanubia Sands, central area.", caution = Caution.Guarded("Dark Ifrit")),
        primer(18, "XVIII", "Bikanel Desert", "Sanubia Sands, central area.", caution = Caution.Guarded("Dark Ifrit")),
        primer(19, "XIX", "Home - Entrance", "On the sand just outside the entrance to Home. Home is destroyed later - there is no second chance.", caution = Caution.Missable),
        primer(20, "XX", "Home - Living Quarters", "Off the main hallway. Lost with Home.", caution = Caution.Missable),
        primer(21, "XXI", "Home - Main Corridor", "In the Main Corridor. Lost with Home.", caution = Caution.Missable),
        primer(22, "XXII", "Bevelle Temple", "In the Priest's Passage, after the wedding scene. You cannot return to Bevelle.", caution = Caution.Missable),
        primer(23, "XXIII", "Calm Lands", "In the northwest corner."),
        primer(24, "XXIV", "Remiem Temple", "In the northwest corner."),
        primer(25, "XXV", "Cavern of the Stolen Fayth", "In an offshoot near the second save point.", caution = Caution.Guarded("Dark Yojimbo")),
        primer(26, "XXVI", "Omega Ruins", "In the room with the four treasure chests.")
    )

    private fun primer(
        number: Int,
        numeral: String,
        location: String,
        detail: String,
        caution: Caution? = null,
        shot: Int? = null
    ) = ReferenceItem(
        id = "albhed_%02d".format(number),
        title = "Al Bhed Primer $numeral",
        location = location,
        detail = detail,
        caution = caution,
        imageRes = shot
    )
}
