package com.safemode.safekeepingforffx

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.ui.AppScaffold
import com.safemode.safekeepingforffx.ui.ThemeViewModel
import com.safemode.safekeepingforffx.ui.theme.SafekeepingForFFXTheme

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels { ThemeViewModel.Factory }

    /** Set once a composition has run with the saved theme, which is what lets the first frame draw. */
    private var themeApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holdFirstFrameUntilThemeApplied()
        setContent {
            val loaded by themeViewModel.theme.collectAsStateWithLifecycle()
            // Only reached unloaded if the wait below ran out, in which case System is the best guess.
            val preference = loaded ?: ThemePreference.DEFAULT
            if (loaded != null) SideEffect { themeApplied = true }

            val darkTheme = when (preference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK, ThemePreference.MIDNIGHT -> true
            }

            // Re-apply edge-to-edge whenever the effective darkness changes. Without this the
            // system bar icons keep following the *system* setting, so forcing Light while the
            // device is in dark mode leaves white icons on a white bar.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_SCRIM,
                        DARK_SCRIM
                    ) { darkTheme }
                )
                onDispose {}
            }

            SafekeepingForFFXTheme(themePreference = preference) {
                AppScaffold()
            }
        }
    }

    /**
     * Keeps the launch screen up until the saved theme has been composed, so a player who forced
     * Light or Dark never sees a frame drawn in the system theme first. The check is on the
     * composition rather than on the loaded value, because the value can arrive a frame before the
     * UI has been rebuilt with it.
     *
     * Capped at [THEME_WAIT_MS] of held frames. On a cold start of a debug build on the emulator the
     * theme landed 150-210 ms after the first frame tried to draw; a slow disk should cost at most a
     * brief flash, never a launch screen that won't go away.
     */
    private fun holdFirstFrameUntilThemeApplied() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            /**
             * When the first frame tried to draw. The wait is timed from here rather than from
             * onCreate: building the first screen can keep the main thread busy for longer than the
             * whole wait, which would use it up before a single frame was held.
             */
            private var firstAttempt = 0L

            override fun onPreDraw(): Boolean {
                val now = SystemClock.uptimeMillis()
                if (firstAttempt == 0L) firstAttempt = now
                val ready = themeApplied || now - firstAttempt > THEME_WAIT_MS
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
    }

    private companion object {
        // Matches the scrims androidx uses for three-button navigation bars.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

        const val THEME_WAIT_MS = 500L
    }
}
