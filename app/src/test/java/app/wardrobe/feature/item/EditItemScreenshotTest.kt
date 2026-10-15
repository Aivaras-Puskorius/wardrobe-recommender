package app.wardrobe.feature.item

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import app.wardrobe.testing.captureScreenInTheme
import app.wardrobe.ui.theme.WardrobeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class EditItemScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val editing = EditItemUiState.Editing(
        form = SampleData.items.first { it.id == "denim-jacket" }.toForm(),
        hasChanges = true,
    )

    @Test
    fun editing() = composeRule.captureInTheme {
        EditItemContent(editing, onClose = {}, onFormChange = {}, onSave = {})
    }

    /** The lower part of the form: material, warmth and seasons. */
    @Test
    fun editingScrolledDown() {
        composeRule.setContent {
            WardrobeTheme { EditItemContent(editing, onClose = {}, onFormChange = {}, onSave = {}) }
        }
        composeRule.onNodeWithText("Winter").performScrollTo()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun discardDialog() = composeRule.captureScreenInTheme {
        EditItemContent(editing, onClose = {}, onFormChange = {}, onSave = {}, showDiscardDialogInitially = true)
    }
}
