package app.wardrobe.data

import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Forecast
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.ItemSuggestion
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Season.AUTUMN
import app.wardrobe.domain.model.Season.SPRING
import app.wardrobe.domain.model.Season.SUMMER
import app.wardrobe.domain.model.Season.WINTER
import app.wardrobe.domain.model.Warmth
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate

/** Fixed wardrobe and forecast for milestone 1, before photos, storage and the weather API exist. */
object SampleData {

    val items: List<ClothingItem> = listOf(
        item("white-tee", "White tee", Garment.T_SHIRT, ItemColor.WHITE, Warmth.LIGHT, SPRING, SUMMER),
        item("black-tee", "Black tee", Garment.T_SHIRT, ItemColor.BLACK, Warmth.LIGHT, SPRING, SUMMER, AUTUMN, fit = Fit.FITTED),
        item("oxford-shirt", "Oxford shirt", Garment.SHIRT, ItemColor.LIGHT_BLUE, Warmth.MEDIUM, SPRING, AUTUMN),
        item("olive-overshirt", "Olive overshirt", Garment.SHIRT, ItemColor.OLIVE, Warmth.MEDIUM, SPRING, AUTUMN, fit = Fit.LOOSE),
        item("cream-knit", "Cream knit", Garment.SWEATER, ItemColor.CREAM, Warmth.WARM, AUTUMN, WINTER),
        item("burgundy-sweater", "Burgundy sweater", Garment.SWEATER, ItemColor.BURGUNDY, Warmth.WARM, AUTUMN, WINTER),
        item(
            "dark-jeans", "Dark jeans", Garment.JEANS, ItemColor.DENIM, Warmth.MEDIUM, SPRING, SUMMER, AUTUMN, WINTER,
            fit = Fit.FITTED,
        ),
        item("beige-chinos", "Beige chinos", Garment.TROUSERS, ItemColor.BEIGE, Warmth.MEDIUM, SPRING, SUMMER, AUTUMN),
        item("black-trousers", "Black trousers", Garment.TROUSERS, ItemColor.BLACK, Warmth.MEDIUM, SPRING, SUMMER, AUTUMN, WINTER),
        item("khaki-shorts", "Khaki shorts", Garment.SHORTS, ItemColor.BEIGE, Warmth.LIGHT, SUMMER),
        item("grey-skirt", "Pleated skirt", Garment.SKIRT, ItemColor.GREY, Warmth.LIGHT, SPRING, SUMMER, AUTUMN, fit = Fit.LOOSE),
        item("white-sneakers", "White sneakers", Garment.SNEAKERS, ItemColor.WHITE, Warmth.LIGHT, SPRING, SUMMER, AUTUMN),
        item("black-sneakers", "Black sneakers", Garment.SNEAKERS, ItemColor.BLACK, Warmth.LIGHT, SPRING, SUMMER, AUTUMN),
        item("brown-boots", "Brown boots", Garment.BOOTS, ItemColor.BROWN, Warmth.WARM, AUTUMN, WINTER),
        item(
            "denim-jacket", "Denim jacket", Garment.JACKET, ItemColor.BLUE, Warmth.MEDIUM, SPRING, AUTUMN,
            material = Material.DENIM, fit = Fit.FITTED,
        ),
        item(
            "olive-rain-jacket", "Olive rain jacket", Garment.JACKET, ItemColor.OLIVE, Warmth.MEDIUM, SPRING, AUTUMN,
            material = Material.WATERPROOF,
        ),
        item("camel-coat", "Camel coat", Garment.COAT, ItemColor.CAMEL, Warmth.WARM, AUTUMN, WINTER),
    )

    /** What the stand-in recognizer "sees" in each new photo, in turn. None of them are in [items]. */
    val photoSuggestions: List<ItemSuggestion> = listOf(
        ItemSuggestion(Garment.SWEATER, ItemColor.GREEN),
        ItemSuggestion(Garment.SNEAKERS, ItemColor.RED),
        ItemSuggestion(Garment.COAT, ItemColor.NAVY),
    )

    /** A week of autumn weather in Kaunas, starting on [today], so the demo always shows current dates. */
    fun forecast(today: LocalDate): Forecast {
        val days = listOf(
            Triple(7 to 14, WeatherCondition.PARTLY_CLOUDY, 10),
            Triple(5 to 11, WeatherCondition.RAIN, 80),
            Triple(3 to 9, WeatherCondition.CLOUDY, 30),
            Triple(4 to 12, WeatherCondition.CLEAR, 5),
            Triple(6 to 15, WeatherCondition.CLEAR, 0),
            Triple(2 to 8, WeatherCondition.RAIN, 70),
            Triple(-1 to 4, WeatherCondition.SNOW, 60),
        ).mapIndexed { index, (range, condition, precipitation) ->
            DayForecast(
                date = today.plusDays(index.toLong()),
                minTemperatureC = range.first,
                maxTemperatureC = range.second,
                condition = condition,
                precipitationChancePercent = precipitation,
            )
        }
        return Forecast(location = "Kaunas", days = days)
    }

    private fun item(
        id: String,
        name: String,
        garment: Garment,
        color: ItemColor,
        warmth: Warmth,
        vararg seasons: Season,
        material: Material = garment.defaultMaterial,
        fit: Fit = Fit.REGULAR,
    ) = ClothingItem(id, name, garment, color, material, fit, warmth, seasons.toSet())
}
