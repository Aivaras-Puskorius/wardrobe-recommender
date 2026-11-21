package app.wardrobe.navigation

import androidx.navigation3.runtime.NavKey
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Warmth
import kotlinx.serialization.Serializable

@Serializable
data object WardrobeRoute : NavKey

@Serializable
data object OutfitsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class ItemDetailRoute(val itemId: String) : NavKey

@Serializable
data object AddItemRoute : NavKey

@Serializable
data class EditItemRoute(val itemId: String) : NavKey

/** Outfits built around one item for one forecast day, given as [java.time.LocalDate.toEpochDay]. */
@Serializable
data class OutfitResultsRoute(val itemId: String, val epochDay: Long) : NavKey

/** The rated outfits, from the Outfits tab. */
@Serializable
data object HistoryRoute : NavKey

/** The place search for the forecast, from Settings. */
@Serializable
data object LocationRoute : NavKey

/** An item seen in a shop, as the Add item form describes it, tried with the wardrobe without adding it. */
@Serializable
data class StoreCheckRoute(
    val name: String,
    val garment: Garment,
    val color: ItemColor,
    val material: Material,
    val fit: Fit,
    val warmth: Warmth,
    val photo: String?,
) : NavKey

/** The garments and colors that would widen the wardrobe, from the Wardrobe tab. */
@Serializable
data object GapsRoute : NavKey

/** What the ratings taught about the user's taste, from Settings. */
@Serializable
data object TasteProfileRoute : NavKey

/** Bottom bar destinations in display order. Each one keeps its own back stack. */
val TopLevelRoutes: List<NavKey> = listOf(WardrobeRoute, OutfitsRoute, SettingsRoute)
