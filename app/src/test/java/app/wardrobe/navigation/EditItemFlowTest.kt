package app.wardrobe.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import app.wardrobe.AppContainer
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Edits an item from its detail screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class EditItemFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.setContent { WardrobeTheme { WardrobeApp(AppContainer()) } }
        composeRule.onNodeWithText("Black tee").performClick()
        composeRule.onNodeWithContentDescription("Edit item").performClick()
    }

    @Test
    fun savedChangesShowOnTheDetailScreenAndInTheGrid() {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Favorite black tee")
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.onNodeWithText("Favorite black tee").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Favorite black tee").assertIsDisplayed()
        composeRule.onNodeWithText("Black tee").assertDoesNotExist()
    }

    @Test
    fun aPickedMaterialShowsOnTheDetailScreen() {
        composeRule.onNodeWithText("Linen").performScrollTo().performClick()
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.onNodeWithText("Material").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Linen").assertIsDisplayed()
    }

    @Test
    fun aPickedFitShowsOnTheDetailScreen() {
        composeRule.onNodeWithText("Loose").performScrollTo().performClick()
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.onNodeWithText("Fit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Loose").assertIsDisplayed()
    }

    @Test
    fun footwearHasNoFitRow() {
        composeRule.onNodeWithText("Fit").assertExists()
        composeRule.onNodeWithText("Sneakers").performScrollTo().performClick()
        composeRule.onNodeWithText("Fit").assertDoesNotExist()
    }

    @Test
    fun closingWithChangesAsksFirst() {
        composeRule.onNodeWithText("Navy").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Close").performClick()

        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.onNodeWithText("T-shirt in Tops").assertIsDisplayed()
        composeRule.onNodeWithText("Black").assertIsDisplayed()
    }

    @Test
    fun closingWithoutChangesLeavesAtOnce() {
        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeRule.onNodeWithText("T-shirt in Tops").assertIsDisplayed()
    }
}
