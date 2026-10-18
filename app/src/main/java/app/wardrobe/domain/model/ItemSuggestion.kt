package app.wardrobe.domain.model

/**
 * What image recognition thinks a photographed item is. The user confirms or corrects it.
 * Each part is null when recognition can't tell, and the user picks it instead.
 */
data class ItemSuggestion(val garment: Garment?, val color: ItemColor?, val material: Material? = null)
