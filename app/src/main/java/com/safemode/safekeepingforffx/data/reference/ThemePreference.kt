package com.safemode.safekeepingforffx.data.reference

enum class ThemePreference(val label: String, val description: String) {

    SYSTEM(
        label = "Automatic",
        description = "Follows your device's light and dark setting."
    ),

    LIGHT(
        label = "Light",
        description = "Always light, regardless of the system setting."
    ),

    DARK(
        label = "Dark",
        description = "Always dark, with the usual dark grey surfaces."
    ),

    MIDNIGHT(
        label = "Midnight",
        description = "Dark with pure black backgrounds. Saves power on OLED screens."
    );

    companion object {
        val DEFAULT = SYSTEM
    }
}
