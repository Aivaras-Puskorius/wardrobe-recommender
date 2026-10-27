package app.wardrobe.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Forecast
import app.wardrobe.domain.model.Place
import app.wardrobe.domain.model.WeatherCondition
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

// Open-Meteo's free forecast API, documented at https://open-meteo.com/en/docs. It needs no key. Its data is
// CC BY 4.0, so every screen that shows it must link to Open-Meteo (https://open-meteo.com/en/licence).

/**
 * The forecast from Open-Meteo for the chosen place, Kaunas until the user picks another. The place and the last
 * forecast are saved in [prefs], so the forecast shows offline and right away at the next start.
 *
 * @param scope runs the fetch that [choose] starts, so leaving the screen that chose doesn't cancel it.
 * @param fetch returns the body of a GET request, or throws [IOException].
 * @param now the current time in the phone's time zone.
 */
class OpenMeteoWeather(
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val fetch: suspend (url: String) -> String = ::httpGet,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : Weather {

    private val _place = MutableStateFlow(savedPlace() ?: KAUNAS)
    override val place: StateFlow<Place> = _place.asStateFlow()

    private val _state = MutableStateFlow(savedForecast() ?: WeatherState.Loading)
    override val state: StateFlow<WeatherState> = _state.asStateFlow()

    /** One fetch at a time, so a refresh and a new place can't save each other's forecast. */
    private val mutex = Mutex()

    override suspend fun refresh(force: Boolean) = mutex.withLock {
        val saved = savedForecast()
        if (!force && saved != null && Duration.between(saved.updatedAt, now().toInstant()) < MAX_AGE) {
            // Still drops the days that passed since the last read.
            _state.value = saved
            return@withLock
        }
        if (saved == null) _state.value = WeatherState.Loading
        val place = _place.value
        _state.value = try {
            val json = fetch(forecastUrl(place))
            // A place chosen during the fetch has its own fetch waiting, and this forecast isn't for it.
            if (_place.value != place) return@withLock
            // Parsed before saving, so the saved forecast is always readable.
            parseForecastDays(json)
            prefs.edit {
                putString(KEY_FORECAST, json)
                putLong(KEY_UPDATED_AT, now().toInstant().toEpochMilli())
            }
            savedForecast() ?: WeatherState.Unavailable
        } catch (e: IOException) {
            failed(saved, e)
        } catch (e: JSONException) {
            failed(saved, e)
        } catch (e: DateTimeParseException) {
            failed(saved, e)
        }
    }

    override suspend fun search(name: String): List<Place> {
        val query = name.trim()
        // Open-Meteo finds nothing for a single letter.
        if (query.length < 2) return emptyList()
        val json = fetch(
            "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query, "UTF-8")}" +
                "&count=10&language=en&format=json",
        )
        return try {
            parsePlaces(json)
        } catch (e: JSONException) {
            throw IOException("Unreadable search response", e)
        }
    }

    override fun choose(place: Place) {
        val saved = JSONObject()
            .put("name", place.name)
            .put("detail", place.detail)
            .put("latitude", place.latitude)
            .put("longitude", place.longitude)
        prefs.edit {
            putString(KEY_PLACE, saved.toString())
            remove(KEY_FORECAST)
            remove(KEY_UPDATED_AT)
        }
        _place.value = place
        _state.value = WeatherState.Loading
        scope.launch { refresh(force = true) }
    }

    private fun failed(saved: WeatherState.Ready?, e: Exception): WeatherState {
        Log.w(TAG, "Forecast refresh failed: $e")
        return saved?.copy(refreshFailed = true) ?: WeatherState.Unavailable
    }

    private fun savedPlace(): Place? {
        val json = JSONObject(prefs.getString(KEY_PLACE, null) ?: return null)
        return Place(
            json.getString("name"),
            json.getString("detail"),
            json.getDouble("latitude"),
            json.getDouble("longitude"),
        )
    }

    /** The saved forecast from today on, or null when there is none or all its days have passed. */
    private fun savedForecast(): WeatherState.Ready? {
        val json = prefs.getString(KEY_FORECAST, null) ?: return null
        val today = now().toLocalDate()
        val days = parseForecastDays(json).filter { it.date >= today }
        if (days.isEmpty()) return null
        val updatedAt = Instant.ofEpochMilli(prefs.getLong(KEY_UPDATED_AT, 0))
        return WeatherState.Ready(Forecast(_place.value.name, days), updatedAt, refreshFailed = false)
    }

    private companion object {
        const val TAG = "OpenMeteoWeather"
        const val KEY_PLACE = "place"
        const val KEY_FORECAST = "forecast"
        const val KEY_UPDATED_AT = "forecast_updated_at"

        /** A forecast younger than this is not fetched again. */
        val MAX_AGE: Duration = Duration.ofHours(1)

        /** From Open-Meteo's place search. */
        val KAUNAS = Place("Kaunas", "Kaunas, Lithuania", 54.90156, 23.90909)
    }
}

/** A week of daily weather at [place], with dates in the place's own time zone. */
private fun forecastUrl(place: Place) =
    "https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}" +
        "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,precipitation_sum" +
        "&timezone=auto&forecast_days=7"

/** The places in a search response. A search with no matches has no results array. */
private fun parsePlaces(json: String): List<Place> {
    val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
    return List(results.length()) { i ->
        val result = results.getJSONObject(i)
        Place(
            name = result.getString("name"),
            detail = listOf(result.optString("admin1"), result.optString("country"))
                .filter { it.isNotEmpty() }
                .joinToString(", "),
            latitude = result.getDouble("latitude"),
            longitude = result.getDouble("longitude"),
        )
    }
}

/** The body of a GET request to [url]. Throws [IOException] when the connection or the server fails. */
private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/**
 * The days of a forecast response that asked for the daily temperature_2m_max, temperature_2m_min, weather_code,
 * precipitation_probability_max and precipitation_sum. A day without a rain chance gets 0, and its weather code still
 * says if it rains. precipitation_sum may be missing, as in forecasts saved before the app asked for it.
 */
fun parseForecastDays(json: String): List<DayForecast> {
    val daily = JSONObject(json).getJSONObject("daily")
    val dates = daily.getJSONArray("time")
    val max = daily.getJSONArray("temperature_2m_max")
    val min = daily.getJSONArray("temperature_2m_min")
    val codes = daily.getJSONArray("weather_code")
    val rainChance = daily.getJSONArray("precipitation_probability_max")
    val rainMm = daily.optJSONArray("precipitation_sum")
    return List(dates.length()) { i ->
        val code = codes.getInt(i)
        DayForecast(
            date = LocalDate.parse(dates.getString(i)),
            minTemperatureC = min.getDouble(i).roundToInt(),
            maxTemperatureC = max.getDouble(i).roundToInt(),
            condition = conditionOf(code, rainMm?.optDouble(i)?.takeUnless { it.isNaN() }),
            precipitationChancePercent = rainChance.optInt(i, 0),
            drizzle = code in DRIZZLE_CODES,
        )
    }
}

/** Drizzle counts as rain from this much in a day. Below it, the day is cloudy. */
private const val DRIZZLE_RAIN_MM = 1.0

/** Drizzle and freezing drizzle. */
private val DRIZZLE_CODES = 51..57

/**
 * The app's condition for a WMO weather code, as listed in Open-Meteo's documentation.
 *
 * @param rainMm the day's rain and snow in millimetres, or null if unknown. Open-Meteo gave light drizzle on days
 *   with a 0% rain chance and 0.1 mm (Vilnius, 2026-10-27), so drizzle is rain only from [DRIZZLE_RAIN_MM].
 *   Without an amount, drizzle stays rain.
 */
fun conditionOf(wmoCode: Int, rainMm: Double? = null): WeatherCondition = when (wmoCode) {
    0, 1 -> WeatherCondition.CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    in DRIZZLE_CODES -> if (rainMm != null && rainMm < DRIZZLE_RAIN_MM) WeatherCondition.CLOUDY else WeatherCondition.RAIN
    // Rain and freezing rain, rain showers, thunderstorms.
    in 61..67, in 80..82, in 95..99 -> WeatherCondition.RAIN
    // Snowfall, snow grains, snow showers.
    in 71..77, 85, 86 -> WeatherCondition.SNOW
    // Overcast, fog, and any code added later.
    else -> WeatherCondition.CLOUDY
}
