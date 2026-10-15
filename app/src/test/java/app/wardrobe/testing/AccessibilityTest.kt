package app.wardrobe.testing

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.feature.outfits.OutfitGeneratorContent
import app.wardrobe.feature.outfits.OutfitGeneratorUiState
import app.wardrobe.feature.outfits.OutfitResultsContent
import app.wardrobe.feature.outfits.OutfitResultsUiState
import app.wardrobe.feature.outfits.OutfitResultsViewModel
import app.wardrobe.feature.settings.SettingsScreen
import app.wardrobe.ui.theme.ThemeMode
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack users get beyond the visible text: headings to jump between and labels for icons. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 8)
    private val forecast = SampleData.forecast(today)

    @Test
    fun outfitTitlesAreHeadings() {
        val state = runBlocking {
            OutfitResultsViewModel("dark-jeans", today, InMemoryWardrobeRepository(SampleData.items), forecast)
                .uiState.first { it is OutfitResultsUiState.Ready }
        }
        composeRule.setContent { WardrobeTheme { OutfitResultsContent(state, onBack = {}, onRate = { _, _ -> }) } }
        composeRule.onNodeWithText("All neutrals").assert(isHeading())
    }

    @Test
    fun settingsSectionsAreHeadings() {
        composeRule.setContent {
            WardrobeTheme { SettingsScreen(ThemeMode.SYSTEM, onThemeModeChange = {}, versionName = "0.1.0") }
        }
        composeRule.onNodeWithText("Appearance").assert(isHeading())
        composeRule.onNodeWithText("Coming later").assert(isHeading())
    }

    @Test
    fun dayChipsSayTheWeather() {
        val items = SampleData.items
        composeRule.setContent {
            WardrobeTheme {
                OutfitGeneratorContent(
                    state = OutfitGeneratorUiState.Ready(items, null, forecast, forecast.days[0]),
                    onSelectItem = {},
                    onSelectDay = {},
                    onShowOutfits = { _, _ -> },
                )
            }
        }
        composeRule.onNodeWithText("Tomorrow").assertContentDescriptionContains("Rain")
        composeRule.onNodeWithText("Start from").assert(isHeading())
    }
}
