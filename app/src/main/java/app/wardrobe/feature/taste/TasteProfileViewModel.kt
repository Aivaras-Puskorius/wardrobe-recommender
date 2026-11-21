package app.wardrobe.feature.taste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.RatingRepository
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.engine.PairTaste
import app.wardrobe.domain.engine.Taste
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TasteProfileUiState {
    data object Loading : TasteProfileUiState

    /**
     * @param ratingCount how many ratings teach the taste.
     * @param since the day of the last Start over, or null.
     * @param liked the pairs with the highest weights, at most [MAX_PAIRS].
     * @param avoided the pairs with the lowest weights, at most [MAX_PAIRS].
     */
    data class Ready(
        val ratingCount: Int,
        val since: LocalDate?,
        val liked: List<PairTaste>,
        val avoided: List<PairTaste>,
    ) : TasteProfileUiState
}

/** What the ratings teach about the user's taste, and Start over. */
class TasteProfileViewModel(
    wardrobe: WardrobeRepository,
    private val ratings: RatingRepository,
    zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    val uiState: StateFlow<TasteProfileUiState> =
        combine(wardrobe.items, ratings.ratings, ratings.tasteSince) { items, rated, since ->
            val taste = Taste.of(rated, items, since)
            TasteProfileUiState.Ready(
                ratingCount = taste.ratingCount,
                since = since?.atZone(zone())?.toLocalDate(),
                liked = taste.liked.take(MAX_PAIRS),
                avoided = taste.avoided.take(MAX_PAIRS),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasteProfileUiState.Loading)

    /** Makes the ratings so far stop teaching the taste. History keeps them. */
    fun startOver() {
        viewModelScope.launch { ratings.startTasteOver() }
    }
}

private const val MAX_PAIRS = 5
