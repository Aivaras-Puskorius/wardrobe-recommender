package app.wardrobe.data

import app.wardrobe.domain.model.Place
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stands in for Open-Meteo in tests: the sample week from [today], for whichever of [SampleData.places] is chosen.
 * The sample week is the same for every place.
 */
class SampleWeather(private val today: () -> LocalDate = LocalDate::now) : Weather {

    private val _place = MutableStateFlow(SampleData.places.first())
    override val place: StateFlow<Place> = _place.asStateFlow()

    private val _state = MutableStateFlow<WeatherState>(ready(_place.value))
    override val state: StateFlow<WeatherState> = _state.asStateFlow()

    /** Moves the week to start on today, as a fetched forecast would. */
    override suspend fun refresh(force: Boolean) {
        _state.value = ready(_place.value)
    }

    override suspend fun search(name: String): List<Place> {
        val query = name.trim()
        if (query.length < 2) return emptyList()
        return SampleData.places.filter { it.name.startsWith(query, ignoreCase = true) }
    }

    override fun choose(place: Place) {
        _place.value = place
        _state.value = ready(place)
    }

    private fun ready(place: Place) = WeatherState.Ready(
        forecast = SampleData.forecast(today(), place.name),
        updatedAt = today().atStartOfDay(ZoneId.systemDefault()).toInstant(),
        refreshFailed = false,
    )
}
