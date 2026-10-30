package app.wardrobe.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.data.outfitKey
import app.wardrobe.domain.model.Rating
import app.wardrobe.testing.captureInTheme
import app.wardrobe.testing.captureScreenInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class HistoryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val byId = SampleData.items.associateBy { it.id }

    private fun outfit(rating: Rating, day: Int, vararg ids: String) = RatedOutfit(
        id = outfitKey(ids.toSet()),
        items = ids.map { byId.getValue(it) }.sortedBy { it.category },
        rating = rating,
        ratedOn = LocalDate.of(2026, 10, day),
    )

    private val jeans = outfit(Rating.LIKED, 29, "black-tee", "dark-jeans", "white-sneakers", "camel-coat")
    private val skirt = outfit(Rating.DISLIKED, 28, "black-tee", "grey-skirt", "white-sneakers")
    private val shirt = outfit(Rating.LIKED, 27, "oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket")

    @Test
    fun all() = composeRule.captureInTheme {
        History(HistoryUiState.Ready(HistoryFilter.ALL, listOf(jeans, skirt, shirt)))
    }

    @Test
    fun likedGallery() = composeRule.captureInTheme {
        History(HistoryUiState.Ready(HistoryFilter.LIKED, listOf(jeans, shirt)))
    }

    @Test
    fun likedGalleryDark() = composeRule.captureInTheme(darkTheme = true) {
        History(HistoryUiState.Ready(HistoryFilter.LIKED, listOf(jeans, shirt)))
    }

    @Test
    fun nothingRated() = composeRule.captureInTheme { History(HistoryUiState.Ready(HistoryFilter.ALL, emptyList())) }

    @Test
    fun openTile() = composeRule.captureScreenInTheme {
        History(HistoryUiState.Ready(HistoryFilter.LIKED, listOf(jeans, shirt)), openOutfit = jeans.id)
    }

    @Composable
    private fun History(state: HistoryUiState, openOutfit: String? = null) {
        HistoryContent(state, onBack = {}, onFilterChange = {}, onRate = { _, _ -> }, openOutfitInitially = openOutfit)
    }
}
