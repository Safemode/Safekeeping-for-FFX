package com.safemode.safekeepingforffx.data.reference

/**
 * Which release the player is on. This matters because Dark Aeons did not exist in the original
 * North American PS2 release - they arrived with the International version and everything since.
 *
 * On [ORIGINAL_PS2] the guarded warnings are noise at best and misleading at worst, so they are
 * hidden. [Caution.Missable] items are unaffected: Home is destroyed in every version.
 */
enum class GameVersion(val label: String, val description: String) {

    INTERNATIONAL_HD(
        label = "International / HD Remaster",
        description = "Dark Aeons guard several areas after Bevelle. This covers the HD " +
            "Remaster, the PS3/PS4/Switch/PC ports, and the original International release."
    ),

    ORIGINAL_PS2(
        label = "Original NA PS2 release",
        description = "No Dark Aeons. Guarded-area warnings are hidden; you can walk back into " +
            "Besaid and the rest freely."
    );

    val hasDarkAeons: Boolean get() = this == INTERNATIONAL_HD

    companion object {
        val DEFAULT = INTERNATIONAL_HD
    }
}
