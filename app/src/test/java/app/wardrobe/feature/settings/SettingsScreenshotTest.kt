package app.wardrobe.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import app.wardrobe.testing.captureScreenInTheme
import app.wardrobe.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun systemTheme() = composeRule.captureInTheme {
        SettingsScreen(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}, place = SampleData.places.first(), onOpenLocation = {}, onOpenTasteProfile = {}, sampleItemCount = 17, onRemoveSampleItems = {}, versionName = "0.1.0")
    }

    @Test
    fun darkTheme() = composeRule.captureInTheme(darkTheme = true) {
        SettingsScreen(themeMode = ThemeMode.DARK, onThemeModeChange = {}, place = SampleData.places.first(), onOpenLocation = {}, onOpenTasteProfile = {}, sampleItemCount = 17, onRemoveSampleItems = {}, versionName = "0.1.0")
    }

    @Test
    fun removeSampleItemsDialog() = composeRule.captureScreenInTheme {
        SettingsScreen(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            place = SampleData.places.first(),
            onOpenLocation = {},
            onOpenTasteProfile = {},
            sampleItemCount = 17,
            onRemoveSampleItems = {},
            versionName = "0.1.0",
            confirmRemovalInitially = true,
        )
    }

    /** With the sample items gone, the Wardrobe section goes too. */
    @Test
    fun noSampleItemsLeft() = composeRule.captureInTheme {
        SettingsScreen(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            place = SampleData.places.first(),
            onOpenLocation = {},
            onOpenTasteProfile = {},
            sampleItemCount = 0,
            onRemoveSampleItems = {},
            versionName = "0.1.0",
        )
    }
}
