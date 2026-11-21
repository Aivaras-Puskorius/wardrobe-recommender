package app.wardrobe.feature.taste

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.wardrobe.ui.theme.WardrobeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** T43 of docs/ENGINE.md section 13.10, the screen part: Start over asks first. */
@RunWith(RobolectricTestRunner::class)
class TasteProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var startedOver = 0

    private fun show() {
        composeRule.setContent {
            WardrobeTheme {
                TasteProfileContent(
                    TasteProfileUiState.Ready(1, null, emptyList(), emptyList()),
                    onBack = {},
                    onStartOver = { startedOver++ },
                )
            }
        }
        composeRule.onNodeWithText("Start over").performClick()
        composeRule.onNodeWithText("Start over?").assertExists()
    }

    @Test
    fun cancelChangesNothing() {
        show()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Start over?").assertDoesNotExist()
        assertEquals(0, startedOver)
    }

    @Test
    fun confirmingStartsOver() {
        show()
        composeRule.onNode(hasText("Start over") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNodeWithText("Start over?").assertDoesNotExist()
        assertEquals(1, startedOver)
    }
}
