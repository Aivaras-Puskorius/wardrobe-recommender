package app.wardrobe.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithContentDescription
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

/** Gets outfit ideas from the Outfits tab and from an item's Style it button. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class OutfitsFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent { WardrobeTheme { WardrobeApp(AppContainer()) } }
    }

    @Test
    fun outfitsTabShowsIdeasForTheChosenItem() {
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNodeWithText("Show outfits").assertIsNotEnabled()

        composeRule.onNodeWithText("Tomorrow").performClick()
        clickInLazyGrid("Dark jeans")
        composeRule.onNodeWithText("Show outfits").performClick()

        composeRule.onNodeWithText("Smart casual").assertIsDisplayed()
        composeRule.onNodeWithText("Kaunas · Tomorrow").assertIsDisplayed()

        composeRule.onAllNodesWithContentDescription("Like this outfit").onFirst().performClick()
        composeRule.onAllNodesWithContentDescription("Like this outfit").onFirst().assertIsOn()
    }

    @Test
    fun styleItShowsIdeasForThatItemToday() {
        clickInLazyGrid("Dark jeans")
        composeRule.onNodeWithText("Style it").performClick()

        composeRule.onNodeWithText("Smart casual").assertIsDisplayed()
        composeRule.onNodeWithText("Kaunas · Today").assertIsDisplayed()
    }

    @Test
    fun otherItemsExplainThatThereAreNoIdeasYet() {
        composeRule.onNodeWithText("Black tee").performClick()
        composeRule.onNodeWithText("Style it").performClick()

        composeRule.onNodeWithText("No outfit ideas yet").assertIsDisplayed()
    }

    /** Lazy grids compose only visible items, so scroll the grid to the item before clicking it. */
    private fun clickInLazyGrid(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).performClick()
    }
}
