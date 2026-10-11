package app.wardrobe.feature.item

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.wardrobe.data.SampleData
import app.wardrobe.ui.theme.WardrobeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ItemDetailLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** The main action stays on screen however tall the details get, so nobody has to scroll for it. */
    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun styleItIsVisibleWithoutScrollingOnASmallPhone() {
        val jeans = ItemDetailUiState.Loaded(SampleData.items.first { it.id == "dark-jeans" })
        composeRule.setContent {
            WardrobeTheme { ItemDetailContent(jeans, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {}) }
        }

        composeRule.onNodeWithText("Style it").assertIsDisplayed()
    }
}
