package app.wardrobe.testing

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.InMemoryWardrobeRepository
import app.wardrobe.data.SampleData
import app.wardrobe.data.SampleWeather
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Warmth
import app.wardrobe.feature.item.AddItemContent
import app.wardrobe.feature.item.AddItemStep
import app.wardrobe.feature.item.ItemDetailContent
import app.wardrobe.feature.item.ItemDetailUiState
import app.wardrobe.feature.item.ItemForm
import app.wardrobe.feature.outfits.OutfitGeneratorContent
import app.wardrobe.feature.outfits.OutfitGeneratorUiState
import app.wardrobe.feature.outfits.OutfitResultsContent
import app.wardrobe.feature.outfits.OutfitResultsUiState
import app.wardrobe.feature.outfits.OutfitResultsViewModel
import app.wardrobe.feature.settings.SettingsScreen
import app.wardrobe.feature.wardrobe.WardrobeContent
import app.wardrobe.feature.wardrobe.WardrobeFilter
import app.wardrobe.feature.wardrobe.WardrobeUiState
import app.wardrobe.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The main screens with the system font size at 200%, to catch clipped or overlapping text. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LargeFontScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 8)
    private val forecast = SampleData.forecast(today)
    private val items = SampleData.items.sortedWith(compareBy({ it.category }, { it.name }))

    @Test
    fun wardrobe() = composeRule.captureInTheme(fontScale = LARGE) {
        WardrobeContent(
            state = WardrobeUiState.Loaded(items, WardrobeFilter(), hasAnyItems = true, availableColors = emptyList()),
            onItemClick = {},
            onAddItem = {},
            onSelectCategory = {},
            onToggleColor = {},
            onToggleSeason = {},
            onClearFilters = {},
        )
    }

    @Test
    fun itemDetail() = composeRule.captureInTheme(fontScale = LARGE) {
        ItemDetailContent(
            state = ItemDetailUiState.Loaded(items.first { it.id == "olive-rain-jacket" }),
            onBack = {},
            onEdit = {},
            onStyle = {},
            onDelete = {},
        )
    }

    @Test
    fun addItemForm() = composeRule.captureInTheme(fontScale = LARGE) {
        val form = ItemForm(
            name = "Green sweater",
            nameEditedByUser = false,
            garment = Garment.SWEATER,
            color = ItemColor.GREEN,
            material = Material.WOOL,
            materialEditedByUser = false,
            fit = Fit.REGULAR,
            photo = null,
            warmth = Warmth.MEDIUM,
            warmthEditedByUser = false,
            seasons = emptySet(),
            suggestion = ItemSuggestion(Garment.SWEATER, ItemColor.GREEN),
        )
        AddItemContent(
            step = AddItemStep.Details(form),
            onClose = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onFormChange = {},
            onSave = {},
        )
    }

    @Test
    fun outfitGenerator() = composeRule.captureInTheme(fontScale = LARGE) {
        OutfitGeneratorContent(
            state = OutfitGeneratorUiState.Ready(items, items.first { it.id == "dark-jeans" }, forecast, forecast.days[1]),
            onSelectItem = {},
            onSelectDay = {},
            onShowOutfits = { _, _ -> },
            onRetry = {},
        )
    }

    @Test
    fun outfitResults() {
        val state = runBlocking {
            OutfitResultsViewModel("dark-jeans", today, InMemoryWardrobeRepository(SampleData.items), SampleWeather { today })
                .uiState.first { it is OutfitResultsUiState.Ready }
        }
        composeRule.captureInTheme(fontScale = LARGE) { OutfitResultsContent(state, onBack = {}, onRate = { _, _ -> }, onRetry = {}) }
    }

    @Test
    fun settings() = composeRule.captureInTheme(fontScale = LARGE) {
        SettingsScreen(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}, versionName = "0.1.0")
    }

    private companion object {
        const val LARGE = 2f
    }
}
