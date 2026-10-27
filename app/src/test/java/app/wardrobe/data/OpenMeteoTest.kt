package app.wardrobe.data

import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs with Robolectric because org.json is part of Android. */
@RunWith(RobolectricTestRunner::class)
class OpenMeteoTest {

    @Test
    fun readsEveryDayOfTheResponse() {
        val days = parseForecastDays(KAUNAS_RESPONSE)
        assertEquals(7, days.size)
        assertEquals(
            DayForecast(LocalDate.of(2026, 10, 27), 3, 11, WeatherCondition.CLOUDY, precipitationChancePercent = 0),
            days.first(),
        )
        assertEquals(
            DayForecast(LocalDate.of(2026, 10, 31), 6, 10, WeatherCondition.RAIN, 18, drizzle = true),
            days[4],
        )
        assertEquals(LocalDate.of(2026, 11, 2), days.last().date)
    }

    @Test
    fun aMissingRainChanceCountsAsNone() {
        val days = parseForecastDays(
            """{"daily": {"time": ["2026-10-27"], "temperature_2m_max": [11.2], "temperature_2m_min": [3.1],
                "weather_code": [61], "precipitation_probability_max": [null]}}""",
        )
        assertEquals(0, days.single().precipitationChancePercent)
        assertEquals(WeatherCondition.RAIN, days.single().condition)
    }

    /** Vilnius on 2026-10-27: light drizzle on days with a 0% to 16% rain chance and 0.1 to 1 mm of rain. */
    @Test
    fun drizzleIsRainOnlyFromOneMillimetre() {
        val days = parseForecastDays(
            """{"daily": {"time": ["2026-10-27", "2026-10-30", "2026-10-31", "2026-11-01"],
                "temperature_2m_max": [11.1, 9.5, 8.4, 7.1], "temperature_2m_min": [6.8, 5.4, 5.7, 3.1],
                "weather_code": [51, 53, 51, 61], "precipitation_probability_max": [0, 16, 16, 6],
                "precipitation_sum": [0.1, 0.33, 1.0, 0.2]}}""",
        )
        assertEquals(
            listOf(WeatherCondition.CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.RAIN, WeatherCondition.RAIN),
            days.map { it.condition },
        )
        // The engine still keeps wool covered on drizzle days that count as cloudy.
        assertEquals(listOf(true, true, true, false), days.map { it.drizzle })
    }

    /** Forecasts saved before the app asked for the amount, and days Open-Meteo has no amount for. */
    @Test
    fun drizzleWithoutAnAmountStaysRain() {
        assertEquals(WeatherCondition.RAIN, conditionOf(51, rainMm = null))
        assertEquals(WeatherCondition.RAIN, parseForecastDays(KAUNAS_RESPONSE)[4].condition)
    }

    @Test
    fun weatherCodesBecomeTheAppsConditions() {
        val expected = mapOf(
            0 to WeatherCondition.CLEAR,
            1 to WeatherCondition.CLEAR,
            2 to WeatherCondition.PARTLY_CLOUDY,
            3 to WeatherCondition.CLOUDY,
            45 to WeatherCondition.CLOUDY,
            48 to WeatherCondition.CLOUDY,
            51 to WeatherCondition.RAIN,
            57 to WeatherCondition.RAIN,
            61 to WeatherCondition.RAIN,
            67 to WeatherCondition.RAIN,
            71 to WeatherCondition.SNOW,
            77 to WeatherCondition.SNOW,
            80 to WeatherCondition.RAIN,
            82 to WeatherCondition.RAIN,
            85 to WeatherCondition.SNOW,
            86 to WeatherCondition.SNOW,
            95 to WeatherCondition.RAIN,
            99 to WeatherCondition.RAIN,
            // A code Open-Meteo might add later.
            100 to WeatherCondition.CLOUDY,
        )
        assertEquals(expected, expected.mapValues { (code, _) -> conditionOf(code) })
    }

    private companion object {
        /** Trimmed from a real response for Kaunas on 2026-10-27. */
        const val KAUNAS_RESPONSE = """{
            "latitude": 54.895668, "longitude": 23.899567, "timezone": "Europe/Vilnius",
            "daily_units": {"time": "iso8601", "temperature_2m_max": "°C", "temperature_2m_min": "°C",
                "weather_code": "wmo code", "precipitation_probability_max": "%"},
            "daily": {
                "time": ["2026-10-27", "2026-10-28", "2026-10-29", "2026-10-30", "2026-10-31", "2026-11-01", "2026-11-02"],
                "temperature_2m_max": [11.2, 10.6, 10.7, 12.1, 9.7, 8.1, 7.3],
                "temperature_2m_min": [3.1, 3.2, 7.5, 5.6, 6.4, 5.1, 3.7],
                "weather_code": [3, 3, 3, 45, 51, 3, 51],
                "precipitation_probability_max": [0, 0, 2, 10, 18, 6, 16]
            }
        }"""
    }
}
