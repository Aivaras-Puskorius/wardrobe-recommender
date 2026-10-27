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
 * @param garment null while recognition couldn't tell the type and the user hasn't picked one. [material] and [warmth]
 *   are null then too, unless recognition or the user gave them, and picking a type fills them in.
 * @param nameEditedByUser false while [name] is the generated default, which then follows garment and color changes.
 * @param materialEditedByUser false while [material] is the garment's default, which then follows garment changes.
 * @param warmthEditedByUser the same for [warmth].
 * @param suggestion what recognition proposed, shown next to the matching choices.
 * @param showErrors becomes true after a failed save, so errors don't show before the user tries.
 */
data class ItemForm(
    val name: String,
    val nameEditedByUser: Boolean,
    val garment: Garment?,
    val color: ItemColor,
    val material: Material?,
    val materialEditedByUser: Boolean,
    val fit: Fit,
    val warmth: Warmth?,
    val warmthEditedByUser: Boolean,
    val seasons: Set<Season>,
    val photo: String?,
    val suggestion: ItemSuggestion? = null,
    val showErrors: Boolean = false,
) {
    /** Without a type, a blank generated name is no error yet: picking the type fills it in. */
    val nameError: Boolean get() = name.isBlank() && (nameEditedByUser || garment != null)
    val garmentError: Boolean get() = garment == null
    val seasonsError: Boolean get() = seasons.isEmpty()
    val isValid: Boolean get() = !nameError && !garmentError && !seasonsError

    /**
     * The item to save, from a valid form. Footwear has no fit, so a fit picked before switching to shoes is dropped.
     * A picked type always comes with a material and warmth, because picking it fills in the ones still missing.
     */
    fun toItem(id: String): ClothingItem {
        val garment = checkNotNull(garment) { "Only a valid form becomes an item" }
        return ClothingItem(
            id, name.trim(), garment, color, checkNotNull(material),
            if (garment.category == Category.FOOTWEAR) Fit.REGULAR else fit,
            checkNotNull(warmth), seasons, photo,
        )
    }
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
    warmthEditedByUser = true,
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
 * whenever its garment or color changes. While the user hasn't picked a material or warmth, they follow the garment.
 */
fun ItemForm.apply(change: ItemFormChange, defaultName: (Garment, ItemColor) -> String): ItemForm =
    when (change) {
        is ItemFormChange.SetName -> copy(name = change.name, nameEditedByUser = true)
        is ItemFormChange.SetGarment -> copy(
            garment = change.garment,
            material = if (materialEditedByUser) material else change.garment.defaultMaterial,
            warmth = if (warmthEditedByUser) warmth else change.garment.defaultWarmth,
        ).renamed(defaultName)
        is ItemFormChange.SetColor -> copy(color = change.color).renamed(defaultName)
        is ItemFormChange.SetMaterial -> copy(material = change.material, materialEditedByUser = true)
        is ItemFormChange.SetFit -> copy(fit = change.fit)
        is ItemFormChange.SetWarmth -> copy(warmth = change.warmth, warmthEditedByUser = true)
        is ItemFormChange.ToggleSeason ->
            copy(seasons = if (change.season in seasons) seasons - change.season else seasons + change.season)
    }

private fun ItemForm.renamed(defaultName: (Garment, ItemColor) -> String): ItemForm =
    if (nameEditedByUser || garment == null) this else copy(name = defaultName(garment, color))
