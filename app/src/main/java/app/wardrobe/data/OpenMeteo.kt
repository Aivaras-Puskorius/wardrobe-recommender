package app.wardrobe.data

import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate
import kotlin.math.roundToInt
import org.json.JSONObject

// Open-Meteo's free forecast API, documented at https://open-meteo.com/en/docs. It needs no key. Its data is
// CC BY 4.0, so every screen that shows it must link to Open-Meteo (https://open-meteo.com/en/licence).

/**
 * The days of a forecast response that asked for the daily temperature_2m_max, temperature_2m_min, weather_code
 * and precipitation_probability_max. A day without a rain chance gets 0, and its weather code still says if it rains.
 */
fun parseForecastDays(json: String): List<DayForecast> {
    val daily = JSONObject(json).getJSONObject("daily")
    val dates = daily.getJSONArray("time")
    val max = daily.getJSONArray("temperature_2m_max")
    val min = daily.getJSONArray("temperature_2m_min")
    val codes = daily.getJSONArray("weather_code")
    val rainChance = daily.getJSONArray("precipitation_probability_max")
    return List(dates.length()) { i ->
        DayForecast(
            date = LocalDate.parse(dates.getString(i)),
            minTemperatureC = min.getDouble(i).roundToInt(),
            maxTemperatureC = max.getDouble(i).roundToInt(),
            condition = conditionOf(codes.getInt(i)),
            precipitationChancePercent = rainChance.optInt(i, 0),
        )
    }
}

/** The app's condition for a WMO weather code, as listed in Open-Meteo's documentation. */
fun conditionOf(wmoCode: Int): WeatherCondition = when (wmoCode) {
    0, 1 -> WeatherCondition.CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    // Drizzle, rain and freezing rain, rain showers, thunderstorms.
    in 51..67, in 80..82, in 95..99 -> WeatherCondition.RAIN
    // Snowfall, snow grains, snow showers.
    in 71..77, 85, 86 -> WeatherCondition.SNOW
    // Overcast, fog, and any code added later.
    else -> WeatherCondition.CLOUDY
}
