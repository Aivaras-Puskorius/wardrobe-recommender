package app.wardrobe.data

import android.content.Context
import app.wardrobe.domain.model.Place
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Runs with Robolectric for SharedPreferences and org.json. Open-Meteo is replaced by canned responses. */
@RunWith(RobolectricTestRunner::class)
class OpenMeteoWeatherTest {

    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("weather", Context.MODE_PRIVATE)
    private var now = ZonedDateTime.of(2026, 10, 27, 12, 0, 0, 0, ZoneId.of("Europe/Vilnius"))
    private val requests = mutableListOf<String>()

    /** What Open-Meteo answers. Throw IOException to act offline. */
    private var respond: (url: String) -> String = { url -> if ("geocoding" in url) SEARCH_RESPONSE else KAUNAS_RESPONSE }

    private fun TestScope.weather() =
        OpenMeteoWeather(prefs, scope = this, fetch = { url -> requests += url; respond(url) }, now = { now })

    private fun offline() {
        respond = { throw IOException("offline") }
    }

    private fun ready(weather: OpenMeteoWeather) = weather.state.value as WeatherState.Ready

    @Test
    fun withoutASavedForecastItLoadsAndThenShowsTheFetchedOne() = runTest {
        val weather = weather()
        assertEquals(WeatherState.Loading, weather.state.value)
        weather.refresh()

        val state = ready(weather)
        assertEquals("Kaunas", state.forecast.location)
        assertEquals(7, state.forecast.days.size)
        assertEquals(now.toInstant(), state.updatedAt)
        assertEquals(false, state.refreshFailed)
        assertTrue(requests.single().startsWith("https://api.open-meteo.com/v1/forecast?latitude=54.90156&longitude=23.90909"))
    }

    @Test
    fun aForecastLessThanAnHourOldIsNotFetchedAgain() = runTest {
        val weather = weather()
        weather.refresh()
        now = now.plusMinutes(59)
        weather.refresh()
        assertEquals(1, requests.size)
        now = now.plusMinutes(2)
        weather.refresh()
        assertEquals(2, requests.size)
    }

    @Test
    fun forceFetchesEvenAFreshForecast() = runTest {
        val weather = weather()
        weather.refresh()
        weather.refresh(force = true)
        assertEquals(2, requests.size)
    }

    @Test
    fun theSavedForecastShowsAtTheNextStart() = runTest {
        weather().refresh()
        val restarted = weather()
        assertEquals(7, ready(restarted).forecast.days.size)
    }

    @Test
    fun offlineTheSavedForecastStaysAndSaysTheRefreshFailed() = runTest {
        val weather = weather()
        weather.refresh()
        val fetchedAt = now.toInstant()
        now = now.plusHours(2)
        offline()
        weather.refresh()

        val state = ready(weather)
        assertEquals(true, state.refreshFailed)
        assertEquals(fetchedAt, state.updatedAt)
    }

    @Test
    fun offlineWithoutASavedForecastThereIsNone() = runTest {
        val weather = weather()
        offline()
        weather.refresh()
        assertEquals(WeatherState.Unavailable, weather.state.value)

        respond = { KAUNAS_RESPONSE }
        weather.refresh()
        assertEquals(false, ready(weather).refreshFailed)
    }

    @Test
    fun aBrokenResponseCountsAsAFailedRefresh() = runTest {
        val weather = weather()
        respond = { "<html>Bad gateway</html>" }
        weather.refresh()
        assertEquals(WeatherState.Unavailable, weather.state.value)
    }

    @Test
    fun daysThatHavePassedAreDropped() = runTest {
        weather().refresh()
        now = now.plusDays(2)
        offline()
        val restarted = weather()
        assertEquals(LocalDate.of(2026, 10, 29), ready(restarted).forecast.days.first().date)
        assertEquals(5, ready(restarted).forecast.days.size)
    }

    @Test
    fun aForecastWithOnlyPastDaysIsNoForecast() = runTest {
        weather().refresh()
        now = now.plusDays(7)
        offline()
        val restarted = weather()
        restarted.refresh()
        assertEquals(WeatherState.Unavailable, restarted.state.value)
    }

    @Test
    fun searchFindsPlacesWithTheirRegionAndCountry() = runTest {
        val places = weather().search("Kaun")
        assertEquals(
            listOf(
                Place("Kaunas", "Kaunas, Lithuania", 54.90156, 23.90909),
                Place("Kaunas Airport", "Kaunas, Lithuania", 54.96392, 24.08478),
                Place("Kaunata", "Latvia", 56.32, 27.55),
            ),
            places,
        )
        assertTrue(requests.single().startsWith("https://geocoding-api.open-meteo.com/v1/search?name=Kaun&"))
    }

    @Test
    fun searchEncodesTheName() = runTest {
        weather().search("Klaipėda ")
        assertTrue("name=Klaip%C4%97da&" in requests.single())
    }

    @Test
    fun searchWithFewerThanTwoLettersAsksNothing() = runTest {
        assertEquals(emptyList<Place>(), weather().search(" K "))
        assertEquals(emptyList<String>(), requests)
    }

    @Test
    fun searchWithNoMatchesFindsNothing() = runTest {
        respond = { """{"generationtime_ms": 0.1}""" }
        assertEquals(emptyList<Place>(), weather().search("Xqzzv"))
    }

    @Test
    fun aChosenPlaceGetsItsOwnForecastAndIsRemembered() = runTest {
        val weather = weather()
        weather.refresh()
        respond = { VILNIUS_RESPONSE }
        weather.choose(VILNIUS)
        assertEquals(VILNIUS, weather.place.value)
        assertEquals(WeatherState.Loading, weather.state.value)
        advanceUntilIdle()

        assertEquals("Vilnius", ready(weather).forecast.location)
        assertTrue("latitude=54.68916&longitude=25.2798" in requests.last())
        assertEquals(VILNIUS, weather().place.value)
    }

    @Test
    fun offlineAChosenPlaceDoesNotShowTheOldPlacesForecast() = runTest {
        val weather = weather()
        weather.refresh()
        offline()
        weather.choose(VILNIUS)
        advanceUntilIdle()
        assertEquals(WeatherState.Unavailable, weather.state.value)
    }

    /** Kaunas's forecast arrives after Vilnius was chosen, and fetching Vilnius's fails. */
    @Test
    fun aForecastForThePreviousPlaceIsDropped() = runTest {
        val kaunasArrives = CompletableDeferred<Unit>()
        respond = { url -> if ("23.90909" in url) KAUNAS_RESPONSE else throw IOException("offline") }
        val weather = OpenMeteoWeather(
            prefs,
            scope = this,
            fetch = { url -> if ("23.90909" in url) kaunasArrives.await(); respond(url) },
            now = { now },
        )
        launch { weather.refresh() }
        runCurrent()
        weather.choose(VILNIUS)
        kaunasArrives.complete(Unit)
        advanceUntilIdle()
        assertEquals(WeatherState.Unavailable, weather.state.value)
    }

    private companion object {
        val VILNIUS = Place("Vilnius", "Vilnius, Lithuania", 54.68916, 25.2798)

        /** Trimmed from a real response for Kaunas on 2026-10-27. */
        const val KAUNAS_RESPONSE = """{"daily": {
            "time": ["2026-10-27", "2026-10-28", "2026-10-29", "2026-10-30", "2026-10-31", "2026-11-01", "2026-11-02"],
            "temperature_2m_max": [11.2, 10.6, 10.7, 12.1, 9.7, 8.1, 7.3],
            "temperature_2m_min": [3.1, 3.2, 7.5, 5.6, 6.4, 5.1, 3.7],
            "weather_code": [3, 3, 3, 45, 51, 3, 51],
            "precipitation_probability_max": [0, 0, 2, 10, 18, 6, 16]}}"""

        const val VILNIUS_RESPONSE = """{"daily": {
            "time": ["2026-10-27"], "temperature_2m_max": [9.0], "temperature_2m_min": [2.0],
            "weather_code": [61], "precipitation_probability_max": [80]}}"""

        /** Trimmed from a real search for "Kaunas" on 2026-10-27, plus a place without a region. */
        const val SEARCH_RESPONSE = """{"results": [
            {"id": 598316, "name": "Kaunas", "latitude": 54.90156, "longitude": 23.90909,
                "country_code": "LT", "country": "Lithuania", "admin1": "Kaunas"},
            {"id": 598317, "name": "Kaunas Airport", "latitude": 54.96392, "longitude": 24.08478,
                "country_code": "LT", "country": "Lithuania", "admin1": "Kaunas"},
            {"id": 1, "name": "Kaunata", "latitude": 56.32, "longitude": 27.55, "country": "Latvia"}
        ]}"""
    }
}
