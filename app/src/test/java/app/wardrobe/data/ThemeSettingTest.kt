package app.wardrobe.data

import android.content.Context
import androidx.core.content.edit
import app.wardrobe.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Runs with Robolectric for SharedPreferences. */
@RunWith(RobolectricTestRunner::class)
class ThemeSettingTest {

    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("settings", Context.MODE_PRIVATE)

    @Test
    fun startsWithTheSystemTheme() {
        assertEquals(ThemeMode.SYSTEM, ThemeSetting(prefs).mode.value)
    }

    @Test
    fun theChosenThemeSurvivesARestart() {
        ThemeSetting(prefs).choose(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeSetting(prefs).mode.value)
    }

    @Test
    fun withoutPrefsTheChoiceStaysInMemory() {
        val theme = ThemeSetting()
        theme.choose(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, theme.mode.value)
    }

    /** A value a later version might no longer know. */
    @Test
    fun anUnknownSavedValueFallsBackToTheSystemTheme() {
        prefs.edit { putString("theme", "SEPIA") }
        assertEquals(ThemeMode.SYSTEM, ThemeSetting(prefs).mode.value)
    }
}
