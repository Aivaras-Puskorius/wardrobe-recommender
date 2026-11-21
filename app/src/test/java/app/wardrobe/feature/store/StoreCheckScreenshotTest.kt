package app.wardrobe.feature.store

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.StoreCheck
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.testing.captureInTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The store check of docs/ENGINE.md section 15.3, on the sample wardrobe. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class StoreCheckScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun checked(name: String, garment: Garment, color: ItemColor) = StoreCheckUiState.Ready(
        StoreCheck.check(
            SampleData.items,
            ClothingItem(
                "shop", name, garment, color, garment.defaultMaterial, Fit.REGULAR, garment.defaultWarmth, emptySet(),
                photo = null,
            ),
        ),
    )

    @Test
    fun navyCoat() = composeRule.captureInTheme { StoreCheckContent(checked("Navy coat", Garment.COAT, ItemColor.NAVY), onBack = {}) }

    @Test
    fun navyCoatDark() = composeRule.captureInTheme(darkTheme = true) {
        StoreCheckContent(checked("Navy coat", Garment.COAT, ItemColor.NAVY), onBack = {})
    }

    /** A near copy of the white tee. */
    @Test
    fun creamTShirt() = composeRule.captureInTheme {
        StoreCheckContent(checked("Cream T-shirt", Garment.T_SHIRT, ItemColor.CREAM), onBack = {})
    }

    @Test
    fun greenSneakers() = composeRule.captureInTheme {
        StoreCheckContent(checked("Green sneakers", Garment.SNEAKERS, ItemColor.GREEN), onBack = {})
    }

    @Test
    fun working() = composeRule.captureInTheme { StoreCheckContent(StoreCheckUiState.Loading, onBack = {}) }
}
