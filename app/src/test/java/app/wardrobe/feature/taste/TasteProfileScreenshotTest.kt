package app.wardrobe.feature.taste

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.Taste
import app.wardrobe.domain.model.OutfitRating
import app.wardrobe.domain.model.Rating
import app.wardrobe.testing.captureInTheme
import app.wardrobe.testing.captureScreenInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Taste profile of docs/ENGINE.md section 13.7, with example 3's ratings of section 13.9. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TasteProfileScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val at = Instant.parse("2026-11-21T08:00:00Z")
    private val outfit3 = setOf("olive-overshirt", "dark-jeans", "black-sneakers", "olive-rain-jacket")

    private val example3 = Taste.of(
        listOf(
            OutfitRating(outfit3, Rating.LIKED, at),
            OutfitRating(setOf("oxford-shirt", "dark-jeans", "black-sneakers", "denim-jacket"), Rating.LIKED, at),
            OutfitRating(setOf("black-tee", "dark-jeans", "white-sneakers", "camel-coat"), Rating.DISLIKED, at),
        ),
        SampleData.items,
    )
    private val learned = TasteProfileUiState.Ready(3, null, example3.liked.take(5), example3.avoided.take(5))

    @Test
    fun profile() = composeRule.captureInTheme { Profile(learned) }

    @Test
    fun profileDark() = composeRule.captureInTheme(darkTheme = true) { Profile(learned) }

    /** One like since a Start over on 21 November. */
    @Test
    fun sinceStartOver() = composeRule.captureInTheme {
        val oneLike = Taste.of(listOf(OutfitRating(outfit3, Rating.LIKED, at)), SampleData.items)
        Profile(TasteProfileUiState.Ready(1, LocalDate.of(2026, 11, 21), oneLike.liked.take(5), emptyList()))
    }

    @Test
    fun nothingLearned() = composeRule.captureInTheme { Profile(TasteProfileUiState.Ready(0, null, emptyList(), emptyList())) }

    @Test
    fun startOverDialog() = composeRule.captureScreenInTheme { Profile(learned, confirm = true) }

    @Composable
    private fun Profile(state: TasteProfileUiState, confirm: Boolean = false) {
        TasteProfileContent(state, onBack = {}, onStartOver = {}, confirmInitially = confirm)
    }
}
