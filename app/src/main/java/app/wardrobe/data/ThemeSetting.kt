package app.wardrobe.data

import android.content.SharedPreferences
import androidx.core.content.edit
import app.wardrobe.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The theme picked in Settings, saved in [prefs] so it survives restarts, or kept in memory without them. */
class ThemeSetting(private val prefs: SharedPreferences? = null) {

    private val state = MutableStateFlow(saved() ?: ThemeMode.SYSTEM)

    val mode: StateFlow<ThemeMode> = state.asStateFlow()

    fun choose(mode: ThemeMode) {
        prefs?.edit { putString(KEY, mode.name) }
        state.value = mode
    }

    /** The saved theme, or null when none is saved or a later version saved one this version doesn't know. */
    private fun saved(): ThemeMode? {
        val name = prefs?.getString(KEY, null) ?: return null
        return ThemeMode.entries.firstOrNull { it.name == name }
    }

    private companion object {
        const val KEY = "theme"
    }
}
