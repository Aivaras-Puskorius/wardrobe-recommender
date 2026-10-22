package app.wardrobe.feature.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Warmth
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
class AddItemScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val suggested = ItemForm(
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

    @Test
    fun chooseSource() = composeRule.captureInTheme { AddItem(AddItemStep.ChooseSource) }

    @Test
    fun analyzing() = composeRule.captureInTheme { AddItem(AddItemStep.Analyzing) }

    @Test
    fun suggestion() = composeRule.captureInTheme { AddItem(AddItemStep.Details(suggested)) }

    /** Recognition found only the color, so the type has no suggestion mark. */
    @Test
    fun colorOnlySuggestion() = composeRule.captureInTheme {
        val colorOnly = suggested.copy(
            name = "Green T-shirt",
            garment = Garment.T_SHIRT,
            material = Material.COTTON,
            suggestion = ItemSuggestion(garment = null, color = ItemColor.GREEN),
        )
        AddItem(AddItemStep.Details(colorOnly))
    }

    @Test
    fun suggestionDark() = composeRule.captureInTheme(darkTheme = true) {
        AddItem(AddItemStep.Details(suggested))
    }

    @Test
    fun validationErrors() = composeRule.captureInTheme {
        AddItem(AddItemStep.Details(suggested.copy(name = "", nameEditedByUser = true, showErrors = true)))
    }

    @Test
    fun discardDialog() = composeRule.captureScreenInTheme {
        AddItem(AddItemStep.Details(suggested), showDiscardDialog = true)
    }

    @Composable
    private fun AddItem(step: AddItemStep, showDiscardDialog: Boolean = false) {
        AddItemContent(
            step = step,
            onClose = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onFormChange = {},
            onSave = {},
            showDiscardDialogInitially = showDiscardDialog,
        )
    }
}
