package app.wardrobe.feature.outfits

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class OutfitGeneratorScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val forecast = SampleData.forecast(LocalDate.of(2026, 10, 8))
    private val items = SampleData.items.sortedWith(compareBy({ it.category }, { it.name }))
    private val nothingChosen = OutfitGeneratorUiState.Ready(
        items = items,
        selectedItem = null,
        forecast = forecast,
        selectedDay = forecast.days[0],
    )
    private val jeansTomorrow = nothingChosen.copy(
        selectedItem = items.first { it.id == "dark-jeans" },
        selectedDay = forecast.days[1],
    )

    @Test
    fun nothingChosen() = composeRule.captureInTheme { Generator(nothingChosen) }

    @Test
    fun itemAndDayChosen() = composeRule.captureInTheme { Generator(jeansTomorrow) }

    @Test
    fun itemAndDayChosenDark() = composeRule.captureInTheme(darkTheme = true) { Generator(jeansTomorrow) }

    /** Fetching a newer forecast failed, so the card says when the shown one is from. */
    @Test
    fun offline() = composeRule.captureInTheme {
        Generator(nothingChosen.copy(offlineSince = LocalDateTime.of(2026, 10, 7, 18, 30)))
    }

    @Test
    fun noForecast() = composeRule.captureInTheme { Generator(OutfitGeneratorUiState.NoForecast) }

    @Composable
    private fun Generator(state: OutfitGeneratorUiState) {
        OutfitGeneratorContent(
            state,
            onSelectItem = {},
            onSelectDay = {},
            onShowOutfits = { _, _ -> },
            onOpenHistory = {},
            onRetry = {},
        )
    }
}
