package com.safemode.safekeepingforffx.data.reference


/**
 * The seventeen Overdrive Modes - the ways a character's Overdrive gauge can be set to fill.
 *
 * Every character starts on Stoic and learns the rest by playing the game the way each mode rewards.
 * The modes are the same for everyone, but each character unlocks them independently, so the list is
 * [ChecklistCategory.perCharacter]: pick a character at the top and tick off what they have learned.
 *
 * The condition is in [ReferenceItem.location] as a short trigger, spelled out in [detail]. How
 * many times each character has to trigger it to learn the mode is in
 * [ReferenceItem.characterCounts], from jegged's Overdrive Modes page. Stoic has none, since every
 * character starts with it.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object OverdriveModes {

    const val CATEGORY_ID = "overdrive_modes"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Overdrive Modes",
        note = "Modes fill the gauge in different ways and every character learns them separately. " +
            "Learning all of them for one character is part of what the grind-heavy trophies ask for.",
        perCharacter = true,
        items = listOf(
            mode("stoic", "Stoic", "Known from the start",
                "The default mode. The gauge fills as the character takes damage."),
            mode("warrior", "Warrior", "Deal damage to enemies",
                "Learned as the character damages enemies, but not through items or Overdrives.",
                counts(tidus = 150, yuna = 200, auron = 100, kimahri = 120, wakka = 160, lulu = 300, rikku = 140)),
            mode("comrade", "Comrade", "Party takes damage",
                "Learned as other party members take damage.",
                counts(tidus = 300, yuna = 240, auron = 220, kimahri = 100, wakka = 100, lulu = 300, rikku = 100)),
            mode("healer", "Healer", "Heal allies",
                "Learned by restoring HP to allies with magic or items.",
                counts(tidus = 80, yuna = 60, auron = 200, kimahri = 100, wakka = 100, lulu = 170, rikku = 70)),
            mode("tactician", "Tactician", "Inflict status ailments",
                "Learned by successfully landing negative status effects on enemies.",
                counts(tidus = 75, yuna = 100, auron = 200, kimahri = 60, wakka = 80, lulu = 75, rikku = 70)),
            mode("victim", "Victim", "Suffer status ailments",
                "Learned when the character is hit with negative status effects.",
                counts(tidus = 100, yuna = 80, auron = 120, kimahri = 130, wakka = 100, lulu = 110, rikku = 90)),
            mode("dancer", "Dancer", "Evade attacks",
                "Learned by evading enemy attacks.",
                counts(tidus = 250, yuna = 200, auron = 200, kimahri = 130, wakka = 200, lulu = 300, rikku = 200)),
            mode("avenger", "Avenger", "Allies are KO'd",
                "Learned when party members are knocked out in battle.",
                counts(tidus = 100, yuna = 80, auron = 120, kimahri = 100, wakka = 100, lulu = 150, rikku = 90)),
            mode("slayer", "Slayer", "Defeat enemies",
                "Learned by defeating enemies.",
                counts(tidus = 100, yuna = 110, auron = 80, kimahri = 120, wakka = 90, lulu = 130, rikku = 100)),
            mode("hero", "Hero", "Defeat strong enemies",
                "Learned by defeating enemies with 10,000 or more max HP.",
                counts(tidus = 50, yuna = 50, auron = 40, kimahri = 45, wakka = 50, lulu = 70, rikku = 50)),
            mode("rook", "Rook", "Block with defensive magic",
                "Learned by nullifying attacks with defensive magic such as NulBlaze or Reflect.",
                counts(tidus = 120, yuna = 110, auron = 120, kimahri = 120, wakka = 120, lulu = 120, rikku = 120)),
            mode("victor", "Victor", "Win battles",
                "Learned by winning battles.",
                counts(tidus = 120, yuna = 150, auron = 200, kimahri = 120, wakka = 160, lulu = 200, rikku = 140)),
            mode("coward", "Coward", "Flee from battle",
                "Learned by escaping from battle.",
                counts(tidus = 600, yuna = 900, auron = 1000, kimahri = 700, wakka = 400, lulu = 980, rikku = 450)),
            mode("ally", "Ally", "Progress the story",
                "Learned naturally as you play through the story.",
                counts(tidus = 600, yuna = 500, auron = 450, kimahri = 300, wakka = 350, lulu = 480, rikku = 320)),
            mode("sufferer", "Sufferer", "Hold a status ailment",
                "Learned while the character carries a negative status effect across turns.",
                counts(tidus = 100, yuna = 80, auron = 120, kimahri = 130, wakka = 100, lulu = 110, rikku = 90)),
            mode("daredevil", "Daredevil", "Fight at low HP",
                "Learned by acting while at 50 percent HP or less.",
                counts(tidus = 170, yuna = 90, auron = 260, kimahri = 200, wakka = 140, lulu = 150, rikku = 110)),
            mode("loner", "Loner", "Fight alone",
                "Learned while the character is the only one left standing in battle.",
                counts(tidus = 60, yuna = 180, auron = 35, kimahri = 90, wakka = 110, lulu = 45, rikku = 170))
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun mode(
        key: String,
        title: String,
        location: String,
        detail: String,
        counts: Map<GridCharacter, Int> = emptyMap()
    ) = ReferenceItem(
        id = "overdrive_mode_$key",
        title = title,
        location = location,
        detail = detail,
        characterCounts = counts
    )

    /**
     * Named rather than positional, so a figure can't slide onto the wrong character - the source
     * page lists them in a different order from the character picker.
     */
    private fun counts(
        tidus: Int,
        yuna: Int,
        auron: Int,
        kimahri: Int,
        wakka: Int,
        lulu: Int,
        rikku: Int
    ) = mapOf(
        GridCharacter.TIDUS to tidus,
        GridCharacter.YUNA to yuna,
        GridCharacter.AURON to auron,
        GridCharacter.KIMAHRI to kimahri,
        GridCharacter.WAKKA to wakka,
        GridCharacter.LULU to lulu,
        GridCharacter.RIKKU to rikku
    )
}
