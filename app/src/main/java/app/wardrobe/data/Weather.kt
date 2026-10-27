package app.wardrobe.data

import app.wardrobe.domain.model.Forecast
import app.wardrobe.domain.model.Place
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

sealed interface WeatherState {
    /** No forecast yet, and one is being fetched. */
    data object Loading : WeatherState

    /**
     * @param updatedAt when the forecast was fetched.
     * @param refreshFailed the last try to fetch a newer one failed, so this one may be old.
     */
    data class Ready(val forecast: Forecast, val updatedAt: Instant, val refreshFailed: Boolean) : WeatherState

    /** No forecast was saved and fetching one failed. */
    data object Unavailable : WeatherState
}

/** The forecast for the chosen place, and the choice of place. */
interface Weather {
    val state: StateFlow<WeatherState>
    val place: StateFlow<Place>

    /** Fetches a newer forecast unless the saved one is recent, or always when [force] is true. */
    suspend fun refresh(force: Boolean = false)

    /** Places whose name starts with [name]. Throws [java.io.IOException] when the search fails. */
    suspend fun search(name: String): List<Place>

    /** Makes [place] the forecast's place and fetches its forecast. */
    suspend fun choose(place: Place)
}
