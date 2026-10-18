package app.wardrobe.feature.item

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth

/**
 * The editable fields of an item.
 *
 * @param nameEditedByUser false while [name] is the generated default, which then follows garment and color changes.
 * @param materialEditedByUser false while [material] is the garment's default, which then follows garment changes.
 * @param suggestion what recognition proposed, shown next to the matching choices.
 * @param showErrors becomes true after a failed save, so errors don't show before the user tries.
 */
data class ItemForm(
    val name: String,
    val nameEditedByUser: Boolean,
    val garment: Garment,
    val color: ItemColor,
    val material: Material,
    val materialEditedByUser: Boolean,
    val fit: Fit,
    val warmth: Warmth,
    val seasons: Set<Season>,
    val photo: String?,
    val suggestion: ItemSuggestion? = null,
    val showErrors: Boolean = false,
) {
    val nameError: Boolean get() = name.isBlank()
    val seasonsError: Boolean get() = seasons.isEmpty()
    val isValid: Boolean get() = !nameError && !seasonsError

    /** The item to save. Footwear has no fit, so a fit picked before switching to shoes is dropped. */
    fun toItem(id: String) = ClothingItem(
        id, name.trim(), garment, color, material,
        if (garment.category == Category.FOOTWEAR) Fit.REGULAR else fit,
        warmth, seasons, photo,
    )
}

/** A form holding an existing item's values, as the edit screen starts with. */
fun ClothingItem.toForm() = ItemForm(
    name = name,
    nameEditedByUser = true,
    garment = garment,
    color = color,
    material = material,
    materialEditedByUser = true,
    fit = fit,
    warmth = warmth,
    seasons = seasons,
    photo = photo,
)

/** One edit the user makes in the item form. */
sealed interface ItemFormChange {
    data class SetName(val name: String) : ItemFormChange
    data class SetGarment(val garment: Garment) : ItemFormChange
    data class SetColor(val color: ItemColor) : ItemFormChange
    data class SetMaterial(val material: Material) : ItemFormChange
    data class SetFit(val fit: Fit) : ItemFormChange
    data class SetWarmth(val warmth: Warmth) : ItemFormChange
    data class ToggleSeason(val season: Season) : ItemFormChange
}

/**
 * Applies [change]. While the user hasn't typed a name, [defaultName] renames the item
 * whenever its garment or color changes. While the user hasn't picked a material, it follows the garment.
 */
fun ItemForm.apply(change: ItemFormChange, defaultName: (Garment, ItemColor) -> String): ItemForm =
    when (change) {
        is ItemFormChange.SetName -> copy(name = change.name, nameEditedByUser = true)
        is ItemFormChange.SetGarment -> renamed(change.garment, color, defaultName)
        is ItemFormChange.SetColor -> renamed(garment, change.color, defaultName)
        is ItemFormChange.SetMaterial -> copy(material = change.material, materialEditedByUser = true)
        is ItemFormChange.SetFit -> copy(fit = change.fit)
        is ItemFormChange.SetWarmth -> copy(warmth = change.warmth)
        is ItemFormChange.ToggleSeason ->
            copy(seasons = if (change.season in seasons) seasons - change.season else seasons + change.season)
    }

private fun ItemForm.renamed(
    garment: Garment,
    color: ItemColor,
    defaultName: (Garment, ItemColor) -> String,
): ItemForm = copy(
    garment = garment,
    color = color,
    name = if (nameEditedByUser) name else defaultName(garment, color),
    material = if (materialEditedByUser) material else garment.defaultMaterial,
)
