package app.wardrobe.feature.outfits

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.InMemoryRatingRepository
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleWeather
import app.wardrobe.domain.model.Rating
import app.wardrobe.testing.captureInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class OutfitResultsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 8)

    /** The state the real view model builds, so the screenshots show the engine's outfits for the sample wardrobe. */
    private fun viewModel(itemId: String) = OutfitResultsViewModel(
        itemId = itemId,
        date = today.plusDays(1),
        repository = InMemoryWardrobeRepository(SampleData.items),
        weather = SampleWeather { today },
        ratings = InMemoryRatingRepository(),
    )

    private fun OutfitResultsViewModel.ready(): OutfitResultsUiState.Ready =
        runBlocking { uiState.first { it is OutfitResultsUiState.Ready } as OutfitResultsUiState.Ready }

    // WARN: rate before the first collection. Waiting for a later emission inside composition blocks the
    // main looper the view model emits on, and the test hangs.
    private fun state(itemId: String, likeFirst: Boolean = false): OutfitResultsUiState {
        if (!likeFirst) return viewModel(itemId).ready()
        val firstId = viewModel(itemId).ready().outfits.first().id
        return viewModel(itemId).apply { rate(firstId, Rating.LIKED) }.ready()
    }

    @Test
    fun outfits() = composeRule.captureInTheme {
        OutfitResultsContent(state("dark-jeans"), onBack = {}, onRate = { _, _ -> }, onRetry = {})
    }

    @Test
    fun outfitsDark() = composeRule.captureInTheme(darkTheme = true) {
        OutfitResultsContent(state("dark-jeans"), onBack = {}, onRate = { _, _ -> }, onRetry = {})
    }

    @Test
    fun rated() = composeRule.captureInTheme {
        val state = state("dark-jeans", likeFirst = true)
        OutfitResultsContent(state, onBack = {}, onRate = { _, _ -> }, onRetry = {})
    }

    @Test
    fun noOutfitsForThisItem() = composeRule.captureInTheme {
        OutfitResultsContent(state("khaki-shorts"), onBack = {}, onRate = { _, _ -> }, onRetry = {})
    }
}
