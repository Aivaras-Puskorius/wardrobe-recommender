package app.wardrobe.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LocationScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val kaunas = SampleData.places.first()

    @Test
    fun nothingTyped() = composeRule.captureInTheme { Location("", LocationSearch.Idle) }

    /** The chosen place has a check mark. */
    @Test
    fun results() = composeRule.captureInTheme { Location("Ka", LocationSearch.Results(SampleData.places.take(2))) }

    @Test
    fun noMatches() = composeRule.captureInTheme { Location("Xqzzv", LocationSearch.NoMatches) }

    @Test
    fun searchFailed() = composeRule.captureInTheme { Location("Vilnius", LocationSearch.Failed) }

    @Composable
    private fun Location(query: String, search: LocationSearch) {
        LocationContent(kaunas, query, search, onQueryChange = {}, onChoose = {}, onBack = {})
    }
}
