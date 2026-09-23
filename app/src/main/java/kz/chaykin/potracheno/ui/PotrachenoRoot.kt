package kz.chaykin.potracheno.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.chaykin.potracheno.data.prefs.Settings
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.prefs.ThemeMode
import kz.chaykin.potracheno.ui.navigation.PotrachenoNavHost
import kz.chaykin.potracheno.ui.theme.PotrachenoTheme

@Composable
fun PotrachenoRoot(settingsStore: SettingsStore) {
    val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = Settings())

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    PotrachenoTheme(darkTheme = darkTheme) {
        PotrachenoNavHost()
    }
}
