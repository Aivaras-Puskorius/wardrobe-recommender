package app.wardrobe.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.RatingRepository
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.data.itemIdsOf
import app.wardrobe.data.outfitKey
import app.wardrobe.data.toggle
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Rating
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, LIKED }

/** A rated outfit as the history shows it, with [items] in display order: top, bottom, footwear, outerwear. */
data class RatedOutfit(val id: String, val items: List<ClothingItem>, val rating: Rating, val ratedOn: LocalDate)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /** @param outfits the rated outfits that pass [filter], the newest rating first. */
    data class Ready(val filter: HistoryFilter, val outfits: List<RatedOutfit>) : HistoryUiState
}

/**
 * The outfits the user rated, all of them or the liked ones. An outfit with a deleted item drops out,
 * and comes back if the delete is undone, because its rating stays saved.
 *
 * @param zone the time zone the rating dates are shown in.
 */
class HistoryViewModel(
    wardrobe: WardrobeRepository,
    private val ratings: RatingRepository,
    zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> =
        combine(wardrobe.items, ratings.ratings, filter) { items, rated, filter ->
            val byId = items.associateBy { it.id }
            val outfits = rated
                .filter { filter == HistoryFilter.ALL || it.rating == Rating.LIKED }
                .mapNotNull { outfit ->
                    val outfitItems = outfit.itemIds.map { id -> byId[id] ?: return@mapNotNull null }
                    RatedOutfit(
                        id = outfitKey(outfit.itemIds),
                        items = outfitItems.sortedBy { it.category },
                        rating = outfit.rating,
                        ratedOn = outfit.ratedAt.atZone(zone()).toLocalDate(),
                    )
                }
            HistoryUiState.Ready(filter, outfits)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun setFilter(filter: HistoryFilter) {
        this.filter.value = filter
    }

    /** Sets [rating] on the outfit, or clears it if the outfit already has that rating. */
    fun rate(outfitId: String, rating: Rating) {
        viewModelScope.launch { ratings.toggle(itemIdsOf(outfitId), rating) }
    }
}
