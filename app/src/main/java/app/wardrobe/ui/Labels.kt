package app.wardrobe.ui

import androidx.annotation.StringRes
import app.wardrobe.R
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import app.wardrobe.domain.model.WeatherCondition

// String resources for the domain enums, so the domain layer has no Android dependencies.

@StringRes
fun Category.pluralLabel(): Int = when (this) {
    Category.TOP -> R.string.category_tops
    Category.BOTTOM -> R.string.category_bottoms
    Category.FOOTWEAR -> R.string.category_footwear
    Category.OUTERWEAR -> R.string.category_outerwear
}

@StringRes
fun Garment.label(): Int = when (this) {
    Garment.T_SHIRT -> R.string.garment_t_shirt
    Garment.SHIRT -> R.string.garment_shirt
    Garment.SWEATER -> R.string.garment_sweater
    Garment.JEANS -> R.string.garment_jeans
    Garment.TROUSERS -> R.string.garment_trousers
    Garment.SHORTS -> R.string.garment_shorts
    Garment.SKIRT -> R.string.garment_skirt
    Garment.SNEAKERS -> R.string.garment_sneakers
    Garment.BOOTS -> R.string.garment_boots
    Garment.JACKET -> R.string.garment_jacket
    Garment.COAT -> R.string.garment_coat
}

@StringRes
fun ItemColor.label(): Int = when (this) {
    ItemColor.WHITE -> R.string.color_white
    ItemColor.CREAM -> R.string.color_cream
    ItemColor.BEIGE -> R.string.color_beige
    ItemColor.CAMEL -> R.string.color_camel
    ItemColor.BROWN -> R.string.color_brown
    ItemColor.GREY -> R.string.color_grey
    ItemColor.BLACK -> R.string.color_black
    ItemColor.NAVY -> R.string.color_navy
    ItemColor.DENIM -> R.string.color_denim
    ItemColor.LIGHT_BLUE -> R.string.color_light_blue
    ItemColor.BLUE -> R.string.color_blue
    ItemColor.GREEN -> R.string.color_green
    ItemColor.OLIVE -> R.string.color_olive
    ItemColor.YELLOW -> R.string.color_yellow
    ItemColor.ORANGE -> R.string.color_orange
    ItemColor.RED -> R.string.color_red
    ItemColor.BURGUNDY -> R.string.color_burgundy
    ItemColor.PINK -> R.string.color_pink
    ItemColor.PURPLE -> R.string.color_purple
}

@StringRes
fun Season.label(): Int = when (this) {
    Season.SPRING -> R.string.season_spring
    Season.SUMMER -> R.string.season_summer
    Season.AUTUMN -> R.string.season_autumn
    Season.WINTER -> R.string.season_winter
}

@StringRes
fun Warmth.label(): Int = when (this) {
    Warmth.LIGHT -> R.string.warmth_light
    Warmth.MEDIUM -> R.string.warmth_medium
    Warmth.WARM -> R.string.warmth_warm
}

@StringRes
fun Fit.label(): Int = when (this) {
    Fit.FITTED -> R.string.fit_fitted
    Fit.REGULAR -> R.string.fit_regular
    Fit.LOOSE -> R.string.fit_loose
    Fit.OVERSIZED -> R.string.fit_oversized
}

@StringRes
fun Material.label(): Int = when (this) {
    Material.COTTON -> R.string.material_cotton
    Material.LINEN -> R.string.material_linen
    Material.WOOL -> R.string.material_wool
    Material.DENIM -> R.string.material_denim
    Material.LEATHER -> R.string.material_leather
    Material.SYNTHETIC -> R.string.material_synthetic
    Material.WATERPROOF -> R.string.material_waterproof
}

@StringRes
fun WeatherCondition.label(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.weather_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.weather_cloudy
    WeatherCondition.RAIN -> R.string.weather_rain
    WeatherCondition.SNOW -> R.string.weather_snow
}
