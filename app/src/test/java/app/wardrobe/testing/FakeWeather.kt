package app.wardrobe.testing

import app.wardrobe.data.SampleData
import app.wardrobe.data.Weather
import app.wardrobe.data.WeatherState
import app.wardrobe.domain.model.Place
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow

/** A [Weather] whose state a test sets, for the states the sample can't show: no forecast, or an old one. */
class FakeWeather(initial: WeatherState) : Weather {
    override val state = MutableStateFlow(initial)
    override val place = MutableStateFlow(SampleData.places.first())

    /** The force argument of each refresh. */
    val refreshes = mutableListOf<Boolean>()

    /** The state a refresh moves to, or null to leave it. */
    var afterRefresh: WeatherState? = null

    override suspend fun refresh(force: Boolean) {
        refreshes += force
        afterRefresh?.let { state.value = it }
    }

    /** True to make every search fail as it would offline. */
    var searchFails = false

    override suspend fun search(name: String): List<Place> =
        if (searchFails) throw IOException("offline") else emptyList()

    override fun choose(place: Place) {
        this.place.value = place
    }
}
