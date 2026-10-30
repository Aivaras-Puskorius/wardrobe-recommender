package app.wardrobe.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.wardrobe.AppContainer
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Likes an outfit, finds it in the history and the gallery, and unlikes it there. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class HistoryFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent { WardrobeTheme { WardrobeApp(AppContainer()) } }
    }

    @Test
    fun aLikedOutfitShowsInTheHistoryAndTheGallery() {
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Dark jeans"))
        composeRule.onNodeWithText("Dark jeans").performClick()
        composeRule.onNodeWithText("Show outfits").performClick()
        composeRule.onAllNodesWithContentDescription("Like this outfit").onFirst().performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("History").performClick()
        composeRule.onNodeWithText("Rated", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Liked").performClick()
        composeRule.onNodeWithText("Rated", substring = true).performClick()
        composeRule.onNodeWithContentDescription("Like this outfit").performClick()
        composeRule.onNodeWithText("No liked outfits yet").assertIsDisplayed()
    }

    @Test
    fun withNothingRatedTheHistorySaysHowToFillIt() {
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNodeWithContentDescription("History").performClick()
        composeRule.onNodeWithText("No rated outfits yet").assertIsDisplayed()
    }
}
