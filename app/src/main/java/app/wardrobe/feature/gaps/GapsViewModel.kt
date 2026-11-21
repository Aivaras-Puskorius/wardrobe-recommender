package app.wardrobe.feature.gaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.engine.Gap
import app.wardrobe.domain.engine.WardrobeGaps
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface GapsUiState {
    data object Loading : GapsUiState

    /** @param gaps empty when no new item would go with anything. */
    data class Ready(val itemCount: Int, val gaps: List<Gap>) : GapsUiState
}

/**
 * What to add to the wardrobe, worked out off the main thread whenever the items change.
 *
 * @param computeOn where the work runs. Tests pass their own dispatcher, so no thread outlives the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GapsViewModel(
    wardrobe: WardrobeRepository,
    computeOn: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    // It runs the engine thousands of times, which takes seconds for a wardrobe of 17 items.
    val uiState: StateFlow<GapsUiState> = wardrobe.items
        .mapLatest<_, GapsUiState> { items -> GapsUiState.Ready(items.size, WardrobeGaps.find(items)) }
        .flowOn(computeOn)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GapsUiState.Loading)
}
