package app.wardrobe.feature.item

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.testing.captureInTheme
import app.wardrobe.testing.captureScreenInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ItemDetailScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val coat = ItemDetailUiState.Loaded(SampleData.items.first { it.id == "camel-coat" })
    private val jeans = ItemDetailUiState.Loaded(SampleData.items.first { it.id == "dark-jeans" })

    @Test
    fun loaded() = composeRule.captureInTheme { ItemDetailContent(coat, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {}) }

    @Test
    fun loadedDark() = composeRule.captureInTheme(darkTheme = true) {
        ItemDetailContent(jeans, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {})
    }

    @Test
    fun deleteDialog() = composeRule.captureScreenInTheme {
        ItemDetailContent(coat, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {}, showDeleteDialogInitially = true)
    }

    @Test
    fun deleteDialogDark() = composeRule.captureScreenInTheme(darkTheme = true) {
        ItemDetailContent(jeans, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {}, showDeleteDialogInitially = true)
    }

    @Test
    fun notFound() = composeRule.captureInTheme {
        ItemDetailContent(ItemDetailUiState.NotFound, onBack = {}, onEdit = {}, onStyle = {}, onDelete = {})
    }
}
