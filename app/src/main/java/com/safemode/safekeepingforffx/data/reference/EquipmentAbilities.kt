package com.safemode.safekeepingforffx.data.reference

/**
 * Every auto-ability that can be added to a weapon or a piece of armor through customization,
 * listed alphabetically with what it does and the item cost.
 *
 * This is a lookup table, not a to-do list, so the category sets `trackProgress = false`: no
 * checkboxes, no progress bar, and it stays off the Home screen summary.
 *
 * The percentage boosts (Strength, Magic, Defense, Magic Def, HP, MP) are listed as one entry per
 * tier, because in game each tier is its own ability with its own price.
 *
 * Ids are permanent. See [ReferenceItem.id].
 */
object EquipmentAbilities {

    const val CATEGORY_ID = "equipment_abilities"

    val category = ChecklistCategory(
        id = CATEGORY_ID,
        label = "Weapon and Armor Abilities",
        note = "Available after Rikku joins the party upon reaching Guadosalam. The item cost shown " +
            "is what it takes to add the ability to one empty slot.",
        trackProgress = false,
        items = listOf(
            ability(
                "alchemy", "Alchemy", "Weapon",
                "Healing Water", 4,
                "Increases the effect of recovery items by 100%."
            ),
            ability(
                "auto_haste", "Auto-Haste", "Armor",
                "Chocobo Wing", 80,
                "The character will start off the battle with the Haste status (increased action speeds) as soon as the battle begins. The status will also not wear off. This is an incredibly helpful ability to add to your character’s armor."
            ),
            ability(
                "auto_med", "Auto-Med", "Armor",
                "Remedy", 20,
                "The character will automatically use a healing item to remove any status effects, using cheaper items first."
            ),
            ability(
                "auto_phoenix", "Auto-Phoenix", "Armor",
                "Mega Phoenix", 20,
                "If an ally is KO’d, they will automatically be revived using a Phoenix Down in your inventory."
            ),
            ability(
                "auto_potion", "Auto-Potion", "Armor",
                "Stamina Tablet", 4,
                "The character will automatically use the most powerful potion available in your inventory when their HP drops below 50%."
            ),
            ability(
                "auto_protect", "Auto-Protect", "Armor",
                "Light Curtain", 70,
                "The character will start off the battle with the Protect status (increased resistance to physical attacks) as soon as the battle begins. The status will also not wear off."
            ),
            ability(
                "auto_reflect", "Auto-Reflect", "Armor",
                "Star Curtain", 40,
                "The character will start off the battle with the Reflect status (rebounds spells cast back at the user) as soon as the battle begins. The status will also not wear off."
            ),
            ability(
                "auto_regen", "Auto-Regen", "Armor",
                "Healing Spring", 80,
                "The character will start off the battle with the Regen status (slowly regenerate HP) as soon as the battle begins. The status will also not wear off. This is an incredibly helpful ability to add to your character’s armor."
            ),
            ability(
                "auto_shell", "Auto-Shell", "Armor",
                "Lunar Curtain", 80,
                "The character will start off the battle with the Shell status (increased resistance to magic attacks) as soon as the battle begins. The status will also not wear off."
            ),
            ability(
                "berserk_ward", "Berserk Ward", "Armor",
                "Hypello Potion", 8,
                "Increases the likelihood of resisting the Berserk status effect."
            ),
            ability(
                "berserkproof", "Berserkproof", "Armor",
                "Hypello Potion", 32,
                "Grants immunity to the Berserk status effect."
            ),
            ability(
                "break_damage_limit", "Break Damage Limit", "Weapon",
                "Dark Matter", 60,
                "Increases the amount of damage that a character can deal with an attack from 9,999 to 99,999. This affects attacks, magic, and healing spells."
            ),
            ability(
                "break_hp_limit", "Break HP Limit", "Armor",
                "Wings to Discovery", 30,
                "Allows a character’s maximum HP to increase from a maximum of 9,999 up to a maximum of 99,999."
            ),
            ability(
                "break_mp_limit", "Break MP Limit", "Armor",
                "Three Stars", 30,
                "Allows a character’s maximum MP to increase from a maximum of 999 up to a maximum of 9,999."
            ),
            ability(
                "confuse_ward", "Confuse Ward", "Armor",
                "Musk", 16,
                "Increases the likelihood of resisting the Confuse status effect."
            ),
            ability(
                "confuseproof", "Confuseproof", "Armor",
                "Musk", 48,
                "Grants immunity to the Confusion status effect."
            ),
            ability(
                "counterattack", "Counterattack", "Weapon",
                "Friend Sphere", 1,
                "Automatically causes the party member to hit back with a physical attack."
            ),
            ability(
                "curseproof", "Curseproof", "Armor",
                "Tetra Elemental", 12,
                "Grants immunity to the Curse status effect."
            ),
            ability(
                "dark_ward", "Dark Ward", "Armor",
                "Eye Drops", 40,
                "Increases the likelihood of resisting the Darkness (Blind) status effect."
            ),
            ability(
                "darkproof", "Darkproof", "Armor",
                "Smoke Bomb", 10,
                "Grants immunity to the Darkness (Blind) status effect."
            ),
            ability(
                "darkstrike", "Darkstrike", "Weapon",
                "Smoke Bomb", 20,
                "Causes regular attacks to nearly always inflict the Darkness status effect. This effect will only work if the enemy is susceptible to Darkness."
            ),
            ability(
                "darktouch", "Darktouch", "Weapon",
                "Eye Drops", 60,
                "Causes regular attacks to sometimes inflict the Darkness status effect. This effect will only work if the enemy is susceptible to Darkness."
            ),
            ability(
                "death_ward", "Death Ward", "Armor",
                "Farplane Shadow", 15,
                "Increases the likelihood of resisting the Instant Death status effect."
            ),
            ability(
                "deathproof", "Deathproof", "Armor",
                "Farplane Wind", 60,
                "Grants immunity to the Instant Death status effect."
            ),
            ability(
                "deathstrike", "Deathstrike", "Weapon",
                "Farplane Wind", 60,
                "Causes regular attacks to nearly always instantly kill the target. This effect will only work if the enemy is susceptible to instant death."
            ),
            ability(
                "deathtouch", "Deathtouch", "Weapon",
                "Farplane Shadow", 30,
                "Causes regular attacks to sometimes instantly kill a target. This effect will only work if the enemy is susceptible to instant death."
            ),
            ability(
                "defense_10", "Defense +10%", "Armor",
                "Special Sphere", 1,
                "Reduces the damage from physical attacks by the stated percentage."
            ),
            ability(
                "defense_20", "Defense +20%", "Armor",
                "Blessed Gem", 4,
                "Reduces the damage from physical attacks by the stated percentage."
            ),
            ability(
                "defense_3", "Defense +3%", "Armor",
                "Power Sphere", 3,
                "Reduces the damage from physical attacks by the stated percentage."
            ),
            ability(
                "defense_5", "Defense +5%", "Armor",
                "Stamina Spring", 2,
                "Reduces the damage from physical attacks by the stated percentage."
            ),
            ability(
                "distill_ability", "Distill Ability", "Weapon",
                "Ability Sphere", 2,
                "Causes any defeated fiends to drop Ability Spheres at the end of the battle."
            ),
            ability(
                "distill_mana", "Distill Mana", "Weapon",
                "Mana Sphere", 2,
                "Causes any defeated fiends to drop Mana Spheres at the end of the battle."
            ),
            ability(
                "distill_power", "Distill Power", "Weapon",
                "Power Sphere", 2,
                "Causes any defeated fiends to drop Power Spheres at the end of the battle."
            ),
            ability(
                "distill_speed", "Distill Speed", "Weapon",
                "Speed Sphere", 2,
                "Causes any defeated fiends to drop Speed Spheres at the end of the battle."
            ),
            ability(
                "double_ap", "Double AP", "Weapon",
                "Megalixir", 20,
                "Increases the amount of AP earned after a battle has concluded by 100%."
            ),
            ability(
                "double_overdrive", "Double Overdrive", "Weapon",
                "Underdog’s Secret", 30,
                "Causes the character’s Overdrive gauge to fill at twice the speed at all times."
            ),
            ability(
                "evade_counter", "Evade & Counter", "Weapon",
                "Teleport Sphere", 1,
                "Allows the character to evade a physical attack and then counterattack the attacker."
            ),
            ability(
                "fire_eater", "Fire Eater", "Armor",
                "Fire Gem", 20,
                "Causes any damage done by Fire-based elemental magic attacks to heal your character’s HP."
            ),
            ability(
                "fire_ward", "Fire Ward", "Armor",
                "Bomb Fragment", 4,
                "Reduces damage from Fire-based elemental magic attacks."
            ),
            ability(
                "fireproof", "Fireproof", "Armor",
                "Bomb Core", 8,
                "Completely reduces any damage done by Fire-based elemental magic attacks."
            ),
            ability(
                "firestrike", "Firestrike", "Weapon",
                "Bomb Fragment", 4,
                "Causes regular attacks to include a Fire-element. This weapon effect can have a different impact depending on the enemy’s elemental alignment."
            ),
            ability(
                "first_strike", "First Strike", "Weapon",
                "Return Sphere", 1,
                "Allows the character to have the first move in battle."
            ),
            ability(
                "gillionaire", "Gillionaire", "Weapon",
                "Designer Wallet", 30,
                "Increases the amount of Gil earned after a battle by 100%."
            ),
            ability(
                "half_mp_cost", "Half MP Cost", "Weapon",
                "Twin Stars", 20,
                "Reduces the cost of magic spells by 50%."
            ),
            ability(
                "hp_10", "HP +10%", "Armor",
                "Soul Spring", 3,
                "Increases a character’s HP by the percent stated. Note that HP is capped at 9,999 HP unless the character is wearing armor that is also equipped with the Break HP Limit Ability."
            ),
            ability(
                "hp_20", "HP +20%", "Armor",
                "Elixir", 5,
                "Increases a character’s HP by the percent stated. Note that HP is capped at 9,999 HP unless the character is wearing armor that is also equipped with the Break HP Limit Ability."
            ),
            ability(
                "hp_30", "HP +30%", "Armor",
                "Stamina Tonic", 1,
                "Increases a character’s HP by the percent stated. Note that HP is capped at 9,999 HP unless the character is wearing armor that is also equipped with the Break HP Limit Ability."
            ),
            ability(
                "hp_5", "HP +5%", "Armor",
                "X-Potion", 1,
                "Increases a character’s HP by the percent stated. Note that HP is capped at 9,999 HP unless the character is wearing armor that is also equipped with the Break HP Limit Ability."
            ),
            ability(
                "hp_stroll", "HP Stroll", "Armor",
                "Stamina Tablet", 2,
                "The character will gradually regain HP as they walk around."
            ),
            ability(
                "ice_eater", "Ice Eater", "Armor",
                "Ice Gem", 20,
                "Causes any damage done by Ice-based elemental magic attacks to heal your character’s HP."
            ),
            ability(
                "ice_ward", "Ice Ward", "Armor",
                "Antarctic Wind", 4,
                "Reduces damage from Ice-based elemental magic attacks."
            ),
            ability(
                "iceproof", "Iceproof", "Armor",
                "Arctic Wind", 8,
                "Completely reduces any damage done by Ice-based elemental magic attacks."
            ),
            ability(
                "icestrike", "Icestrike", "Weapon",
                "Antarctic Wind", 4,
                "Causes regular attacks to include an Ice-element. This weapon effect can have a different impact depending on the enemy’s elemental alignment."
            ),
            ability(
                "initiative", "Initiative", "Weapon",
                "Chocobo Feather", 6,
                "Removes the chance for an enemy to ambush your team and increases the likelihood of you initiating a preemptive strike to start the battle."
            ),
            ability(
                "lightning_eater", "Lightning Eater", "Armor",
                "Lightning Gem", 20,
                "Causes any damage done by Lightning-based elemental magic attacks to heal your character’s HP."
            ),
            ability(
                "lightning_ward", "Lightning Ward", "Armor",
                "Electro Marble", 4,
                "Reduces damage from Lightning-based elemental magic attacks."
            ),
            ability(
                "lightningproof", "Lightningproof", "Armor",
                "Lightning Marble", 8,
                "Completely reduces any damage done by Lightning-based elemental magic attacks."
            ),
            ability(
                "lightningstrike", "Lightningstrike", "Weapon",
                "Electro Marble", 4,
                "Causes regular attacks to include a Thunder-element. This weapon effect can have a different impact depending on the enemy’s elemental alignment."
            ),
            ability(
                "magic_10", "Magic +10%", "Weapon",
                "Blk Magic Sphere", 1,
                "Increases the magical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "magic_20", "Magic +20%", "Weapon",
                "Supreme Gem", 4,
                "Increases the magical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "magic_3", "Magic +3%", "Weapon",
                "Mana Sphere", 3,
                "Increases the magical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "magic_5", "Magic +5%", "Weapon",
                "Mana Spring", 2,
                "Increases the magical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "magic_booster", "Magic Booster", "Weapon",
                "Turbo Ether", 30,
                "Increases the attack power of magic by 50% while also increasing the MP cost of magic spells by 100%."
            ),
            ability(
                "magic_counter", "Magic Counter", "Weapon",
                "Shining Gem", 16,
                "Allows the character to counterattack when hit by a magical attack."
            ),
            ability(
                "magic_def_10", "Magic Def +10%", "Armor",
                "Wht Magic Sphere", 1,
                "Reduces the damage from magic attacks by the stated percentage."
            ),
            ability(
                "magic_def_20", "Magic Def +20%", "Armor",
                "Blessed Gem", 4,
                "Reduces the damage from magic attacks by the stated percentage."
            ),
            ability(
                "magic_def_3", "Magic Def +3%", "Armor",
                "Mana Sphere", 3,
                "Reduces the damage from magic attacks by the stated percentage."
            ),
            ability(
                "magic_def_5", "Magic Def +5%", "Armor",
                "Mana Spring", 2,
                "Reduces the damage from magic attacks by the stated percentage."
            ),
            ability(
                "master_thief", "Master Thief", "Armor",
                "Pendulum", 30,
                "Increases the likelihood that a character will steal higher value items when using the Steal or Mug command to 100% (more effective than Pickpocket)."
            ),
            ability(
                "mp_10", "MP +10%", "Armor",
                "Soul Spring", 3,
                "Increases a character’s MP by the percent stated. Note that MP is capped at 999 HP unless the character is wearing armor that is also equipped with the Break MP Limit Ability."
            ),
            ability(
                "mp_20", "MP +20%", "Armor",
                "Elixir", 5,
                "Increases a character’s MP by the percent stated. Note that MP is capped at 999 HP unless the character is wearing armor that is also equipped with the Break MP Limit Ability."
            ),
            ability(
                "mp_30", "MP +30%", "Armor",
                "Mana Tonic", 1,
                "Increases a character’s MP by the percent stated. Note that MP is capped at 999 HP unless the character is wearing armor that is also equipped with the Break MP Limit Ability."
            ),
            ability(
                "mp_5", "MP +5%", "Armor",
                "Ether", 1,
                "Increases a character’s MP by the percent stated. Note that MP is capped at 999 HP unless the character is wearing armor that is also equipped with the Break MP Limit Ability."
            ),
            ability(
                "mp_stroll", "MP Stroll", "Armor",
                "Mana Tablet", 2,
                "The character will gradually regain MP as they walk around."
            ),
            ability(
                "no_encounters", "No Encounters", "Armor",
                "Purifying Salt", 30,
                "Prevents your characters from being thrown into random battles as they travel through Spira."
            ),
            ability(
                "one_mp_cost", "One MP Cost", "Weapon",
                "Three Stars", 20,
                "Reduces the cost of magic spells to 1 MP per cast."
            ),
            ability(
                "overdrive_ap", "Overdrive → AP", "Weapon",
                "Door to Tomorrow", 10,
                "Increases the AP that you gain after a battle rather than having the character charge their Overdrive gauge."
            ),
            ability(
                "pickpocket", "Pickpocket", "Armor",
                "Amulet", 30,
                "Increases the likelihood that a character will steal higher value items when using the Steal or Mug command."
            ),
            ability(
                "piercing", "Piercing", "Weapon",
                "Lv. 2 Key Sphere", 1,
                "Allows attacks to deal full damage to “Armored” enemies."
            ),
            ability(
                "poison_ward", "Poison Ward", "Armor",
                "Antidote", 40,
                "Increases the likelihood of resisting the Poison status effect."
            ),
            ability(
                "poisonproof", "Poisonproof", "Armor",
                "Poison Fang", 12,
                "Grants immunity to the Poison status effect."
            ),
            ability(
                "poisonstrike", "Poisonstrike", "Weapon",
                "Poison Fang", 24,
                "Causes regular attacks to nearly always inflict the Poison status effect. This effect will only work if the enemy is susceptible to Poison."
            ),
            ability(
                "poisontouch", "Poisontouch", "Weapon",
                "Antidote", 99,
                "Causes regular attacks to sometimes inflict the Poison status effect. This effect will only work if the enemy is susceptible to Poison."
            ),
            ability(
                "ribbon", "Ribbon", "Armor",
                "Dark Matter", 99,
                "Protects the character against all negative status effects."
            ),
            ability(
                "sensor", "Sensor", "Weapon",
                "Ability Sphere", 2,
                "Can be used during a battle to reveal the maximum HP, elemental characteristics, and status effects."
            ),
            ability(
                "silence_ward", "Silence Ward", "Armor",
                "Echo Screen", 30,
                "Increases the likelihood of resisting the Silence status effect."
            ),
            ability(
                "silenceproof", "Silenceproof", "Armor",
                "Silence Grenade", 10,
                "Grants immunity to the Silence status effect."
            ),
            ability(
                "silencestrike", "Silencestrike", "Weapon",
                "Silence Grenade", 20,
                "Causes regular attacks to nearly always inflict the Silence status effect. This effect will only work if the enemy is susceptible to Silence."
            ),
            ability(
                "silencetouch", "Silencetouch", "Weapon",
                "Echo Screen", 60,
                "Causes regular attacks to sometimes inflict the Silence status effect. This effect will only work if the enemy is susceptible to Silence."
            ),
            ability(
                "sleep_ward", "Sleep Ward", "Armor",
                "Sleeping Powder", 6,
                "Increases the likelihood of resisting the Sleep status effect."
            ),
            ability(
                "sleepproof", "Sleepproof", "Armor",
                "Dream Powder", 8,
                "Grants immunity to the Sleep status effect."
            ),
            ability(
                "sleepstrike", "Sleepstrike", "Weapon",
                "Dream Powder", 16,
                "Causes regular attacks to nearly always inflict the Sleep status effect. This effect will only work if the enemy is susceptible to Sleep."
            ),
            ability(
                "sleeptouch", "Sleeptouch", "Weapon",
                "Sleeping Powder", 10,
                "Causes regular attacks to sometimes inflict the Sleep status effect. This effect will only work if the enemy is susceptible to Sleep."
            ),
            ability(
                "slow_ward", "Slow Ward", "Armor",
                "Silver Hourglass", 10,
                "Increases the likelihood of resisting the Slow status effect."
            ),
            ability(
                "slowproof", "Slowproof", "Armor",
                "Gold Hourglass", 20,
                "Grants immunity to the Slow status effect."
            ),
            ability(
                "slowstrike", "Slowstrike", "Weapon",
                "Gold Hourglass", 30,
                "Causes regular attacks to nearly always inflict the Slow status effect. This effect will only work if the enemy is susceptible to Slow."
            ),
            ability(
                "slowtouch", "Slowtouch", "Weapon",
                "Silver Hourglass", 16,
                "Causes regular attacks to sometimes inflict the Slow status effect. This effect will only work if the enemy is susceptible to Slow."
            ),
            ability(
                "sos_haste", "SOS Haste", "Armor",
                "Chocobo Feather", 20,
                "Automatically casts Haste (increased attack speed) on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_nulblaze", "SOS NulBlaze", "Armor",
                "Bomb Core", 1,
                "Automatically casts NulBlaze on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_nulfrost", "SOS NulFrost", "Armor",
                "Arctic Wind", 1,
                "Automatically casts NulFrost on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_nulshock", "SOS NulShock", "Armor",
                "Lightning Marble", 1,
                "Automatically casts NulShock on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_nultide", "SOS NulTide", "Armor",
                "Dragon Scale", 1,
                "Automatically casts NulTide on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_overdrive", "SOS Overdrive", "Weapon",
                "Gambler’s Spirit", 20,
                "Causes the character’s Overdrive gauge to fill at twice the speed when the character has low HP."
            ),
            ability(
                "sos_protect", "SOS Protect", "Armor",
                "Light Curtain", 8,
                "Automatically casts Protect (protection against physical attacks) on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_reflect", "SOS Reflect", "Armor",
                "Star Curtain", 8,
                "Automatically casts Reflect (bounces magic-based attacks back at the caster) on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_regen", "SOS Regen", "Armor",
                "Healing Spring", 12,
                "Automatically casts Regen (slowly regenerate HP) on your character when their HP reaches a critical level."
            ),
            ability(
                "sos_shell", "SOS Shell", "Armor",
                "Lunar Curtain", 8,
                "Automatically casts Shell (protection against magic attacks) on your character when their HP reaches a critical level."
            ),
            ability(
                "stone_ward", "Stone Ward", "Armor",
                "Soft", 30,
                "Increases the likelihood of resisting the Petrification status effect."
            ),
            ability(
                "stoneproof", "Stoneproof", "Armor",
                "Petrify Grenade", 20,
                "Grants immunity to the Petrification status effect."
            ),
            ability(
                "stonestrike", "Stonestrike", "Weapon",
                "Petrify Grenade", 60,
                "Causes regular attacks to nearly always inflict the Petrification status effect. This effect will only work if the enemy is susceptible to Petrification."
            ),
            ability(
                "stonetouch", "Stonetouch", "Weapon",
                "Petrify Grenade", 10,
                "Causes regular attacks to sometimes inflict the Petrification status effect. This effect will only work if the enemy is susceptible to Petrification."
            ),
            ability(
                "strength_10", "Strength +10%", "Weapon",
                "Skill Sphere", 1,
                "Increases the physical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "strength_20", "Strength +20%", "Weapon",
                "Supreme Gem", 4,
                "Increases the physical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "strength_3", "Strength +3%", "Weapon",
                "Power Sphere", 3,
                "Increases the physical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "strength_5", "Strength +5%", "Weapon",
                "Stamina Spring", 2,
                "Increases the physical attack damage done by regular attacks by either 3%, 5%, 10%, or 20%."
            ),
            ability(
                "triple_ap", "Triple AP", "Weapon",
                "Wings to Discovery", 50,
                "Increases the amount of AP earned after a battle has concluded by 200%."
            ),
            ability(
                "triple_overdrive", "Triple Overdrive", "Weapon",
                "Winning Formula", 30,
                "Causes the character’s Overdrive gauge to fill at three times the speed at all times."
            ),
            ability(
                "water_eater", "Water Eater", "Armor",
                "Water Gem", 20,
                "Causes any damage done by Water-based elemental magic attacks to heal your character’s HP."
            ),
            ability(
                "water_ward", "Water Ward", "Armor",
                "Fish Scale", 4,
                "Reduces damage from Water-based elemental magic attacks."
            ),
            ability(
                "waterproof", "Waterproof", "Armor",
                "Dragon Scale", 8,
                "Completely reduces any damage done by Water-based elemental magic attacks."
            ),
            ability(
                "waterstrike", "Waterstrike", "Weapon",
                "Fish Scale", 4,
                "Causes regular attacks to include a Water-element. This weapon effect can have a different impact depending on the enemy’s elemental alignment."
            ),
            ability(
                "zombie_ward", "Zombie Ward", "Armor",
                "Holy Water", 30,
                "Increases the likelihood of resisting the Zombie status effect."
            ),
            ability(
                "zombieproof", "Zombieproof", "Armor",
                "Candle of Life", 10,
                "Grants immunity to the Zombie status effect."
            ),
            ability(
                "zombiestrike", "Zombiestrike", "Weapon",
                "Candle of Life", 30,
                "Causes regular attacks to nearly always inflict the Zombie status effect. This effect will only work if the enemy is susceptible to Zombie."
            ),
            ability(
                "zombietouch", "Zombietouch", "Weapon",
                "Holy Water", 70,
                "Causes regular attacks to sometimes inflict the Zombie status effect. This effect will only work if the enemy is susceptible to Zombie."
            )
        )
    )

    val items: List<ReferenceItem> get() = category.items

    private fun ability(
        key: String,
        name: String,
        slot: String,
        item: String,
        quantity: Int,
        effect: String
    ) = ReferenceItem(
        id = "ability_" + key,
        title = name,
        location = item + " x" + quantity,
        detail = effect,
        tag = slot
    )
}
