package app.wardrobe.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wardrobe.data.Weather
import app.wardrobe.domain.model.Place
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

sealed interface LocationSearch {
    /** Fewer than two letters typed. */
    data object Idle : LocationSearch
    data object Searching : LocationSearch
    data class Results(val places: List<Place>) : LocationSearch
    data object NoMatches : LocationSearch
    data object Failed : LocationSearch
}

/** Typing stops for this long before a search, so each letter doesn't send one to Open-Meteo. */
internal const val SEARCH_DELAY_MILLIS = 300L

/** The place search behind the forecast's location in Settings. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LocationViewModel(private val weather: Weather) : ViewModel() {

    val place: StateFlow<Place> = weather.place

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val search: StateFlow<LocationSearch> = _query
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce(SEARCH_DELAY_MILLIS)
        .transformLatest { name ->
            if (name.length < 2) {
                emit(LocationSearch.Idle)
                return@transformLatest
            }
            emit(LocationSearch.Searching)
            val result = try {
                weather.search(name).let { if (it.isEmpty()) LocationSearch.NoMatches else LocationSearch.Results(it) }
            } catch (e: IOException) {
                LocationSearch.Failed
            }
            emit(result)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationSearch.Idle)

    fun setQuery(query: String) {
        _query.value = query
    }

    fun choose(place: Place) = weather.choose(place)
}
