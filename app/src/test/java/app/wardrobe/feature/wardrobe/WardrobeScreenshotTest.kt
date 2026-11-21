package app.wardrobe.feature.wardrobe

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ItemColor
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
class WardrobeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val allItems = WardrobeUiState.Loaded(
        items = SampleData.items.sortedWith(compareBy({ it.category }, { it.name })),
        filter = WardrobeFilter(),
        hasAnyItems = true,
        availableColors = SampleData.items.map { it.color }.distinct().sorted(),
    )

    @Test
    fun grid() = composeRule.captureInTheme { Wardrobe(allItems) }

    @Test
    fun gridDark() = composeRule.captureInTheme(darkTheme = true) { Wardrobe(allItems) }

    @Test
    fun categorySelected() = composeRule.captureInTheme {
        val filter = WardrobeFilter(category = Category.FOOTWEAR)
        Wardrobe(allItems.copy(items = allItems.items.filter(filter::matches), filter = filter))
    }

    @Test
    fun emptyWardrobe() = composeRule.captureInTheme {
        Wardrobe(allItems.copy(items = emptyList(), hasAnyItems = false, availableColors = emptyList()))
    }

    @Test
    fun noMatches() = composeRule.captureInTheme {
        Wardrobe(allItems.copy(items = emptyList(), filter = WardrobeFilter(colors = setOf(ItemColor.PURPLE))))
    }

    @Test
    fun filterSheet() = composeRule.captureScreenInTheme {
        Wardrobe(allItems.copy(filter = WardrobeFilter(colors = setOf(ItemColor.BLACK))), showFilters = true)
    }

    @Test
    fun undoSnackbar() = composeRule.captureInTheme {
        val hostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { hostState.showSnackbar("Black tee deleted", "Undo", duration = SnackbarDuration.Indefinite) }
        Wardrobe(allItems, snackbarHostState = hostState)
    }

    @Composable
    private fun Wardrobe(
        state: WardrobeUiState,
        showFilters: Boolean = false,
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    ) {
        WardrobeContent(
            state = state,
            onItemClick = {},
            onAddItem = {},
            onOpenGaps = {},
            onSelectCategory = {},
            onToggleColor = {},
            onToggleSeason = {},
            onClearFilters = {},
            snackbarHostState = snackbarHostState,
            showFiltersInitially = showFilters,
        )
    }
}
