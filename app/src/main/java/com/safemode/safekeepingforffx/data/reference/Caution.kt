package com.safemode.safekeepingforffx.data.reference

/**
 * Why an item needs attention. These are two genuinely different problems and the app should not
 * blur them:
 *
 * - [Missable] is fatal. The area is gone - Home is destroyed, Bevelle cannot be re-entered - and
 *   no amount of grinding brings it back. Miss it and the playthrough is incomplete.
 * - [Guarded] is only expensive. From the International release onward a Dark Aeon parks itself in
 *   the area once you leave Bevelle. The item is still there; you just have to beat something that
 *   badly outclasses a story-level party first.
 */
sealed class Caution {

    data object Missable : Caution()

    data class Guarded(val aeon: String) : Caution()

    val label: String
        get() = when (this) {
            Missable -> "Missable"
            is Guarded -> aeon
        }
}
