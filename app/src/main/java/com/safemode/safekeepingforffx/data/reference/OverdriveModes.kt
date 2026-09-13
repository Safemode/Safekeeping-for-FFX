package com.safemode.safekeepingforffx.data.reference


/**
 * The seventeen Overdrive Modes - the ways a character's Overdrive gauge can be set to fill.
 *
 * Every character starts on Stoic and learns the rest by playing the game the way each mode rewards.
 * The modes are the same for everyone, but each character unlocks them independently, so the list is
 * [ChecklistCategory.perCharacter]: pick a character at the top and tick off what they have learned.
 *
 * The condition is in [ReferenceItem.location] as a short trigger, spelled out in [detail].
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
                "Learned as the character damages enemies, but not through items or Overdrives."),
            mode("comrade", "Comrade", "Party takes damage",
                "Learned as other party members take damage."),
            mode("healer", "Healer", "Heal allies",
                "Learned by restoring HP to allies with magic or items."),
            mode("tactician", "Tactician", "Inflict status ailments",
                "Learned by successfully landing negative status effects on enemies."),
            mode("victim", "Victim", "Suffer status ailments",
                "Learned when the character is hit with negative status effects."),
            mode("dancer", "Dancer", "Evade attacks",
                "Learned by evading enemy attacks."),
            mode("avenger", "Avenger", "Allies are KO'd",
                "Learned when party members are knocked out in battle."),
            mode("slayer", "Slayer", "Defeat enemies",
                "Learned by defeating enemies."),
            mode("hero", "Hero", "Defeat strong enemies",
                "Learned by defeating enemies with 10,000 or more max HP."),
            mode("rook", "Rook", "Block with defensive magic",
                "Learned by nullifying attacks with defensive magic such as NulBlaze or Reflect."),
            mode("victor", "Victor", "Win battles",
                "Learned by winning battles."),
            mode("coward", "Coward", "Flee from battle",
                "Learned by escaping from battle."),
            mode("ally", "Ally", "Progress the story",
                "Learned naturally as you play through the story."),
            mode("sufferer", "Sufferer", "Hold a status ailment",
                "Learned while the character carries a negative status effect across turns."),
            mode("daredevil", "Daredevil", "Fight at low HP",
                "Learned by acting while at 50 percent HP or less."),
            mode("loner", "Loner", "Fight alone",
                "Learned while the character is the only one left standing in battle.")
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun mode(
        key: String,
        title: String,
        location: String,
        detail: String
    ) = ReferenceItem(
        id = "overdrive_mode_$key",
        title = title,
        location = location,
        detail = detail
    )
}
