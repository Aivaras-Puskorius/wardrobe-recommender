package app.wardrobe.feature.gaps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import app.wardrobe.domain.engine.Gap
import app.wardrobe.domain.engine.GapGroups
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.ui.theme.WardrobeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The two groups of What to add, docs/ENGINE.md section 16.3. */
@RunWith(RobolectricTestRunner::class)
class GapsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val whiteSneakers = Gap(Garment.SNEAKERS, listOf(ItemColor.WHITE), goesWith = 5, itemDays = 9)
    private val pinkJeans = Gap(Garment.JEANS, listOf(ItemColor.PINK), goesWith = 4, itemDays = 6)

    private fun show(groups: GapGroups) = composeRule.setContent {
        WardrobeTheme { GapsContent(GapsUiState.Ready(10, groups), onBack = {}) }
    }

    private fun scrollTo(text: String) = composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))

    @Test
    fun bothGroupsHaveAHeadingAndTheirRows() {
        show(GapGroups(neutral = listOf(whiteSneakers), bold = listOf(pinkJeans)))
        composeRule.onNodeWithText("For more neutral outfits").assertIsDisplayed()
        composeRule.onNodeWithText("Sneakers").assertIsDisplayed()
        scrollTo("For bolder outfits")
        composeRule.onNodeWithText("For bolder outfits").assertIsDisplayed()
        scrollTo("Jeans")
        composeRule.onNodeWithText("Jeans").assertIsDisplayed()
    }

    @Test
    fun aGroupWithNoPieceSaysSoInOneLine() {
        show(GapGroups(neutral = listOf(whiteSneakers), bold = emptyList()))
        scrollTo("For bolder outfits")
        scrollTo("Nothing in these colors goes with your items yet.")
        composeRule.onNodeWithText("Nothing in these colors goes with your items yet.").assertIsDisplayed()
    }
}
