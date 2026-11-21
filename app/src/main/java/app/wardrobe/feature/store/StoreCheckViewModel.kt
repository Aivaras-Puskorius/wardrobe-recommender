package app.wardrobe.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.domain.engine.StoreCheck
import app.wardrobe.domain.engine.StoreCheckResult
import app.wardrobe.domain.model.ClothingItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface StoreCheckUiState {
    data object Loading : StoreCheckUiState

    data class Ready(val result: StoreCheckResult) : StoreCheckUiState
}

/**
 * The store check of an [item] the user hasn't added, against the wardrobe, worked out off the main thread.
 *
 * @param computeOn where the work runs. Tests pass their own dispatcher, so no thread outlives the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreCheckViewModel(
    wardrobe: WardrobeRepository,
    item: ClothingItem,
    computeOn: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    val uiState: StateFlow<StoreCheckUiState> = wardrobe.items
        .mapLatest<_, StoreCheckUiState> { items -> StoreCheckUiState.Ready(StoreCheck.check(items, item)) }
        .flowOn(computeOn)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreCheckUiState.Loading)
}
