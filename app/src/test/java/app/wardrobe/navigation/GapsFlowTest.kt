package app.wardrobe.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.wardrobe.AppContainer
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opens What to add from the Wardrobe tab and waits for the suggestions. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class GapsFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theLightbulbShowsWhatToAdd() {
        composeRule.setContent { WardrobeTheme { WardrobeApp(AppContainer()) } }
        composeRule.onNodeWithContentDescription("What to add").performClick()
        // The suggestions take a few seconds to work out.
        composeRule.waitUntil(30_000) {
            composeRule.onAllNodes(hasText("Goes with 11 of your 17 items")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Long-sleeve top").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("What to add").assertIsDisplayed()
    }
}
