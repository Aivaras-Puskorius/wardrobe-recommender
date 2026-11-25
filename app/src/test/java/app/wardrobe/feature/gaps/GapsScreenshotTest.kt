package app.wardrobe.feature.gaps

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.GapGroups
import app.wardrobe.domain.engine.WardrobeGaps
import app.wardrobe.testing.captureInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What to add, docs/ENGINE.md sections 14.5 and 16.3, for the sample wardrobe of section 16.5. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class GapsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample = GapsUiState.Ready(SampleData.items.size, WardrobeGaps.find(SampleData.items))

    @Test
    fun sampleWardrobe() = composeRule.captureInTheme { GapsContent(sample, onBack = {}) }

    @Test
    fun sampleWardrobeDark() = composeRule.captureInTheme(darkTheme = true) { GapsContent(sample, onBack = {}) }

    @Test
    fun working() = composeRule.captureInTheme { GapsContent(GapsUiState.Loading, onBack = {}) }

    @Test
    fun nothingToAdd() = composeRule.captureInTheme {
        GapsContent(GapsUiState.Ready(2, GapGroups(emptyList(), emptyList())), onBack = {})
    }

    /** Section 16.3: a group with no candidate says so in one line. */
    @Test
    fun noBoldPiece() = composeRule.captureInTheme {
        GapsContent(sample.copy(gaps = sample.gaps.copy(bold = emptyList())), onBack = {})
    }
}
