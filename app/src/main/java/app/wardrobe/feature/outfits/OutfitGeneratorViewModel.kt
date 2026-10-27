package app.wardrobe.feature.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.WardrobeRepository
import app.wardrobe.data.Weather
import app.wardrobe.data.WeatherState
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Forecast
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OutfitGeneratorUiState {
    data object Loading : OutfitGeneratorUiState

    /** No forecast was saved and fetching one failed. */
    data object NoForecast : OutfitGeneratorUiState

    /**
     * @param items the wardrobe, grouped by category, to pick the item to build outfits around.
     * @param selectedItem the chosen item, or null if none is chosen or it was deleted.
     * @param offlineSince when the shown forecast was fetched, if fetching a newer one failed. Null otherwise.
     */
    data class Ready(
        val items: List<ClothingItem>,
        val selectedItem: ClothingItem?,
        val forecast: Forecast,
        val selectedDay: DayForecast,
        val offlineSince: LocalDateTime? = null,
    ) : OutfitGeneratorUiState {
        val canShowOutfits: Boolean get() = selectedItem != null
    }
}

/**
 * The Outfits tab: choose an item and a day of the forecast. Starts on the forecast's first day, and fetches a
 * newer forecast when the saved one is old.
 */
class OutfitGeneratorViewModel(
    repository: WardrobeRepository,
    private val weather: Weather,
) : ViewModel() {

    private val selectedItemId = MutableStateFlow<String?>(null)

    /** The day the user picked. Until then, or when a newer forecast lacks it, the forecast's first day is shown. */
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    init {
        viewModelScope.launch { weather.refresh() }
    }

    val uiState: StateFlow<OutfitGeneratorUiState> =
        combine(repository.items, weather.state, selectedItemId, selectedDate) { items, weatherState, itemId, date ->
            when (weatherState) {
                WeatherState.Loading -> OutfitGeneratorUiState.Loading
                WeatherState.Unavailable -> OutfitGeneratorUiState.NoForecast
                is WeatherState.Ready -> {
                    val days = weatherState.forecast.days
                    OutfitGeneratorUiState.Ready(
                        items = items.sortedWith(compareBy({ it.category }, { it.name })),
                        selectedItem = items.firstOrNull { it.id == itemId },
                        forecast = weatherState.forecast,
                        // A newer forecast may no longer have the chosen day.
                        selectedDay = days.firstOrNull { it.date == date } ?: days.first(),
                        offlineSince = weatherState.offlineSince,
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OutfitGeneratorUiState.Loading)

    /** Fetches the forecast again after it failed. */
    fun retry() {
        viewModelScope.launch { weather.refresh(force = true) }
    }

    fun selectItem(itemId: String) {
        selectedItemId.value = itemId
    }

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
    }
}
