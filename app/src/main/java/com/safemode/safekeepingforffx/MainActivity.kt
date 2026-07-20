package com.safemode.safekeepingforffx

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.ui.AppScaffold
import com.safemode.safekeepingforffx.ui.ThemeViewModel
import com.safemode.safekeepingforffx.ui.theme.SafekeepingForFFXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
            val preference by themeViewModel.theme.collectAsStateWithLifecycle()

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

    private companion object {
        // Matches the scrims androidx uses for three-button navigation bars.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
