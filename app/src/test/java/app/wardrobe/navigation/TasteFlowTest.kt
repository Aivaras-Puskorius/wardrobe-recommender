package app.wardrobe.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
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

/** Likes an outfit, finds what it taught on the Taste profile, and starts over. History keeps the like. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TasteFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent { WardrobeTheme { WardrobeApp(AppContainer()) } }
    }

    private fun openTasteProfile() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Taste profile").performClick()
    }

    @Test
    fun aLikeTeachesTheTasteAndStartOverForgetsIt() {
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Dark jeans"))
        composeRule.onNodeWithText("Dark jeans").performClick()
        composeRule.onNodeWithText("Show outfits").performClick()
        composeRule.onAllNodesWithContentDescription("Like this outfit").onFirst().performClick()
        composeRule.onNodeWithText("You liked this outfit before.").assertIsDisplayed()
        // The results screen covers the tabs.
        composeRule.onNodeWithContentDescription("Back").performClick()

        openTasteProfile()
        composeRule.onNodeWithText("Learned from 1 rated outfit.").assertIsDisplayed()
        composeRule.onNodeWithText("You like").assertIsDisplayed()

        composeRule.onNodeWithText("Start over").performClick()
        composeRule.onNode(hasText("Start over") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNodeWithText("No taste learned yet").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNodeWithContentDescription("History").performClick()
        composeRule.onNodeWithText("Rated", substring = true).assertIsDisplayed()
    }

    @Test
    fun withoutRatingsTheProfileSaysHowToTeachIt() {
        openTasteProfile()
        composeRule.onNodeWithText("No taste learned yet").assertIsDisplayed()
    }
}
