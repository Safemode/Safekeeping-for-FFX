package com.safemode.safekeepingforffx.data.reference

/**
 * Every recruitable blitzball player outside the Besaid Aurochs, grouped by the team they start
 * on, with where to sign them and the three Key Techniques they bring.
 *
 * The Aurochs live in [BlitzballKeyTechs] instead, where each of their key techs is tracked
 * separately along with what it does. Here one entry is one player, so ticking a row means "signed
 * them", which is the thing you are actually working through when you tour Spira scouting.
 *
 * Wakka counts as an Aurochs for our purposes even though he is technically a free agent once he
 * leaves the team; he is in the other list.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object BlitzballRecruits {

    const val CATEGORY_ID = "blitzball_recruits"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Blitzball Recruits",
        showOnHome = false,
        note = "Approach a player and press the confirm button to open contract talks. A player " +
            "under contract to another team can only be signed once their current contract " +
            "runs out. Each player's three key techniques unlock the rest of their tech slots.",
        items = listOf(
            // Luca Goers
            player(
                "abus", "Luca Goers", "Abus",
                "Luca, Port 3",
                "Key techniques: Grip Gloves, Venom Tackle 2, Venom Shot 3. Signing costs 120 gil per game."
            ),
            player(
                "balgerda", "Luca Goers", "Balgerda",
                "Luca, Port 3",
                "Key techniques: Nap Tackle, Anti Wither, Drain Tackle 2. Signing costs 110 gil per game."
            ),
            player(
                "bickson", "Luca Goers", "Bickson",
                "Luca, Port 3",
                "Key techniques: Wither Shot, Nap Pass, Anti Nap. Signing costs 70 gil per game."
            ),
            player(
                "doram", "Luca Goers", "Doram",
                "Luca, Port 3",
                "Key techniques: Wither Tackle, Nap Tackle, Volley Shot. Signing costs 100 gil per game."
            ),
            player(
                "graav", "Luca Goers", "Graav",
                "Luca, Port 3",
                "Key techniques: Venom Pass, Tackle Slip, Drain Tackle 2. Signing costs 200 gil per game."
            ),
            player(
                "raudy", "Luca Goers", "Raudy",
                "Luca, Port 3",
                "Key techniques: Grip Gloves, Gamble, Tackle Slip 2. Signing costs 10 gil per game."
            ),

            // Kilika Beasts
            player(
                "diem", "Kilika Beasts", "Diem",
                "Kilika Temple",
                "Key techniques: Venom Tackle, Wither Pass, Pile Wither. Signing costs 90 gil per game."
            ),
            player(
                "isken", "Kilika Beasts", "Isken",
                "Port Kilika, Residence",
                "Key techniques: Wither Pass, Wither Tackle, Wither Tackle 2. Signing costs 120 gil per game."
            ),
            player(
                "kulukan", "Kilika Beasts", "Kulukan",
                "Port Kilika, Tavern",
                "Key techniques: Drain Tackle, Nap Pass, Venom Tackle 3. Signing costs 50 gil per game."
            ),
            player(
                "larbeight", "Kilika Beasts", "Larbeight",
                "Port Kilika, Docks",
                "Key techniques: Wither Shot, Anti Nap, Tackle Slip 2. Signing costs 70 gil per game."
            ),
            player(
                "nizarut", "Kilika Beasts", "Nizarut",
                "Kilika Temple",
                "Key techniques: Venom Shot, Anti Wither, Anti Nap. Signing costs 150 gil per game."
            ),
            player(
                "vuroja", "Kilika Beasts", "Vuroja",
                "Port Kilika, Docks",
                "Key techniques: Wither Tackle, Nap Pass, Anti Nap. Signing costs 50 gil per game."
            ),

            // Al Bhed Psyches
            player(
                "berrik", "Al Bhed Psyches", "Berrik",
                "Airship, Cargo Hold",
                "Key techniques: Venom Tackle, Wither Tackle 2, Elite Defense. Signing costs 30 gil per game."
            ),
            player(
                "blappa", "Al Bhed Psyches", "Blappa",
                "Airship, Cargo Hold",
                "Key techniques: Elite Defense, Drain Tackle, Nap Shot. Signing costs 130 gil per game."
            ),
            player(
                "eigaar", "Al Bhed Psyches", "Eigaar",
                "Airship, Cargo Hold",
                "Key techniques: Venom Tackle, Spin Ball, Volley Shot 3. Signing costs 180 gil per game."
            ),
            player(
                "judda", "Al Bhed Psyches", "Judda",
                "Airship, Cargo Hold",
                "Key techniques: Anti Nap, Wither Tackle, Anti Wither. Signing costs 50 gil per game."
            ),
            player(
                "lakkam", "Al Bhed Psyches", "Lakkam",
                "Airship, Cargo Hold",
                "Key techniques: Venom Tackle, Nap Pass, Tackle Slip. Signing costs 20 gil per game."
            ),
            player(
                "nimrook", "Al Bhed Psyches", "Nimrook",
                "Airship, Cargo Hold",
                "Key techniques: Venom Tackle, Venom Tackle 2, Anti Drain. Signing costs 100 gil per game."
            ),

            // Ronso Fangs
            player(
                "argai_ronso", "Ronso Fangs", "Argai Ronso",
                "Luca, Port 4",
                "Key techniques: Wither Tackle, Venom Pass 2, Anti Drain. Signing costs 200 gil per game."
            ),
            player(
                "basik_ronso", "Ronso Fangs", "Basik Ronso",
                "Luca, Port 4",
                "Key techniques: Nap Shot, Venom Tackle, Invisible Shot. Signing costs 3500 gil per game."
            ),
            player(
                "gazna_ronso", "Ronso Fangs", "Gazna Ronso",
                "Luca, Port 4",
                "Key techniques: Venom Pass, Drain Tackle, Volley Shot 2. Signing costs 150 gil per game."
            ),
            player(
                "irga_ronso", "Ronso Fangs", "Irga Ronso",
                "Luca, Port 4",
                "Key techniques: Pile Wither, Wither Tackle 3, Super Goalie. Signing costs 450 gil per game."
            ),
            player(
                "nuvy_ronso", "Ronso Fangs", "Nuvy Ronso",
                "Luca, Port 4",
                "Key techniques: Venom Tackle, Volley Shot, Tackle Slip. Signing costs 650 gil per game."
            ),
            player(
                "zamzi_ronso", "Ronso Fangs", "Zamzi Ronso",
                "Luca, Port 4",
                "Key techniques: Spin Ball, Super Goalie, Invisible Shot. Signing costs 300 gil per game."
            ),

            // Guado Glories
            player(
                "auda_guado", "Guado Glories", "Auda Guado",
                "Guadosalam, area map",
                "Key techniques: Anti Venom, Anti Nap, Anti Wither. Signing costs 120 gil per game."
            ),
            player(
                "giera_guado", "Guado Glories", "Giera Guado",
                "Guadosalam, area map",
                "Key techniques: Venom Shot, Nap Shot, Pile Venom. Signing costs 1000 gil per game."
            ),
            player(
                "navara_guado", "Guado Glories", "Navara Guado",
                "Guadosalam, area map",
                "Key techniques: Drain Tackle, Super Goalie, Nap Tackle 2. Signing costs 100 gil per game."
            ),
            player(
                "noy_guado", "Guado Glories", "Noy Guado",
                "Guadosalam, Inn",
                "Key techniques: Anti Nap, Wither Pass 2, Elite Defense. Signing costs 80 gil per game."
            ),
            player(
                "pah_guado", "Guado Glories", "Pah Guado",
                "Guadosalam, Residence",
                "Key techniques: Drain Tackle, Venom Tackle 2, Gamble. Signing costs 10 gil per game."
            ),
            player(
                "zazi_guado", "Guado Glories", "Zazi Guado",
                "Guadosalam, Residence",
                "Key techniques: Anti Venom, Wither Shot 2, Anti Venom 2. Signing costs 90 gil per game."
            ),

            // Free Agents
            player(
                "biggs", "Free Agents", "Biggs",
                "Luca, guarding the Stadium",
                "Key techniques: Wither Shot, Nap Tackle 2, Spin Ball. Signing costs 100 gil per game."
            ),
            player(
                "brother", "Free Agents", "Brother",
                "Airship Bridge, at the helm",
                "Key techniques: Wither Tackle 2, Nap Tackle, Sphere Shot. Signing costs 210 gil per game."
            ),
            player(
                "durren", "Free Agents", "Durren",
                "Outside the Cavern of the Stolen Fayth",
                "Key techniques: Nap Pass 2, Nap Pass, Anti Nap. Signing costs 400 gil per game."
            ),
            player(
                "jumal", "Free Agents", "Jumal",
                "Luca Square, near the fountain on a park bench",
                "Key techniques: Spin Ball, Tackle Slip, Tackle Slip 2. Signing costs 200 gil per game."
            ),
            player(
                "kiyuri", "Free Agents", "Kiyuri",
                "S.S. Winno, Deck",
                "Key techniques: Wither Shot, Sphere Shot, Volley Shot 2. Signing costs 500 gil per game."
            ),
            player(
                "kyou", "Free Agents", "Kyou",
                "Outside Djose Temple",
                "Key techniques: Venom Pass, Volley Shot, Nap Tackle 2. Signing costs 300 gil per game."
            ),
            player(
                "linna", "Free Agents", "Linna",
                "Macalania Temple",
                "Key techniques: Nap Shot, Nap Tackle, Drain Tackle 2. Signing costs 900 gil per game."
            ),
            player(
                "mep", "Free Agents", "Mep",
                "Kilika Temple",
                "Key techniques: Anti Drain, Drain Tackle, Pile Venom. Signing costs 150 gil per game."
            ),
            player(
                "mifurey", "Free Agents", "Mifurey",
                "Thunder Plains, Travel Agency",
                "Key techniques: Nap Pass, Super Goalie, Wither Tackle. Signing costs 600 gil per game."
            ),
            player(
                "miyu", "Free Agents", "Miyu",
                "Moonflow, North Dock",
                "Key techniques: Super Goalie, Gamble, Hi Risk. Signing costs 200 gil per game."
            ),
            player(
                "naida", "Free Agents", "Naida",
                "Calm Lands, trading post",
                "Key techniques: Wither Shot, Nap Tackle 2, Spin Ball. Signing costs 340 gil per game."
            ),
            player(
                "nedus", "Free Agents", "Nedus",
                "Luca, Port 1",
                "Key techniques: Volley Shot, Volley Shot 2, Anti Wither. Signing costs 60 gil per game."
            ),
            player(
                "rin", "Free Agents", "Rin",
                "Airship, Hallway",
                "Key techniques: Venom Pass, Anti Venom, Venom Tackle. Signing costs 100 gil per game."
            ),
            player(
                "ropp", "Free Agents", "Ropp",
                "Mi'ihen Highroad, Travel Agency",
                "Key techniques: Nap Tackle, Venom Pass 3, Anti Venom 2. Signing costs 200 gil per game."
            ),
            player(
                "shaami", "Free Agents", "Shaami",
                "Luca, on the screen south of Luca Theater",
                "Key techniques: Wither Shot, Wither Pass, Venom Pass 3. Signing costs 190 gil per game."
            ),
            player(
                "shuu", "Free Agents", "Shuu",
                "Luca Cafe",
                "Key techniques: Venom Tackle 2, Anti Venom, Pile Venom. Signing costs 80 gil per game."
            ),
            player(
                "svanda", "Free Agents", "Svanda",
                "Calm Lands, central north, looking over the Scar",
                "Key techniques: Nap Shot, Venom Tackle 2, Regen. Signing costs 130 gil per game."
            ),
            player(
                "tatts", "Free Agents", "Tatts",
                "Port Kilika, by piles of cargo on the Dock",
                "Key techniques: Nap Tackle, Venom Tackle, Nap Tackle 2. Signing costs 320 gil per game."
            ),
            player(
                "vilucha", "Free Agents", "Vilucha",
                "Besaid Village, Residence",
                "Key techniques: Tackle Slip, Volley Shot, Anti Venom 2. Signing costs 320 gil per game."
            ),
            player(
                "wedge", "Free Agents", "Wedge",
                "Luca, guarding the Stadium",
                "Key techniques: Nap Tackle, Wither Tackle, Anti Venom 2. Signing costs 160 gil per game."
            ),
            player(
                "yuma_guado", "Free Agents", "Yuma Guado",
                "Guadosalam, Residence",
                "Key techniques: Venom Tackle, Nap Tackle 2, Anti Wither. Signing costs 100 gil per game."
            ),
            player(
                "zalitz", "Free Agents", "Zalitz",
                "Outside Luca Theater",
                "Key techniques: Anti Venom, Venom Pass, Tackle Slip. Signing costs 150 gil per game."
            ),
            player(
                "zev_ronso", "Free Agents", "Zev Ronso",
                "Luca, Port 5",
                "Key techniques: Volley Shot, Anti Wither, Pile Venom. Signing costs 150 gil per game."
            )
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun player(
        key: String,
        section: String,
        name: String,
        location: String,
        detail: String
    ) = ReferenceItem(
        id = "recruit_$key",
        title = name,
        location = location,
        detail = detail,
        section = section
    )
}
