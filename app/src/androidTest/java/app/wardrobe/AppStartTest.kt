package app.wardrobe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real app on a device: Room, ML Kit and the forecast as WardrobeApplication sets them up. */
@RunWith(AndroidJUnit4::class)
class AppStartTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** A fresh install starts with the 17 sample items, and the Outfits tab opens. */
    @Test
    fun theWardrobeOpensWithTheSampleItems() {
        // The sample items are written to the database on its first start.
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasText("White tee")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("White tee").assertIsDisplayed()
        composeRule.onNodeWithText("Outfits").performClick()
        composeRule.onNodeWithText("Start from").assertIsDisplayed()
    }
}
