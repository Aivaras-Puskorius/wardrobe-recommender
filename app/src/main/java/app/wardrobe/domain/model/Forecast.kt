package app.wardrobe.domain.model

import java.time.LocalDate

/** A place to get the forecast for. [detail] tells places with the same name apart, such as "Kaunas, Lithuania". */
data class Place(val name: String, val detail: String, val latitude: Double, val longitude: Double)

/** Daily forecast for the user's chosen location. */
data class Forecast(
    val location: String,
    val days: List<DayForecast>,
)

/**
 * @param drizzle the forecast says drizzle, whatever the amount. Light drizzle makes [condition] cloudy, and the
 *   engine still keeps wool under a jacket.
 */
data class DayForecast(
    val date: LocalDate,
    val minTemperatureC: Int,
    val maxTemperatureC: Int,
    val condition: WeatherCondition,
    val precipitationChancePercent: Int,
    val drizzle: Boolean = false,
)

enum class WeatherCondition { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, SNOW }
