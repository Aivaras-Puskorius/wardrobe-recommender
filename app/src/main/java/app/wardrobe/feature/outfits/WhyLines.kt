package app.wardrobe.feature.outfits

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.wardrobe.R
import app.wardrobe.domain.engine.Reason
import app.wardrobe.domain.engine.ScoreTerm
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.WeatherCondition
import app.wardrobe.ui.label

// Turns the engine's titles and score terms into English sentences, like Labels.kt does for single values.

@Composable
fun OutfitTitle.text(): String {
    val base = when (story) {
        ColorStory.ALL_NEUTRALS -> stringResource(R.string.title_all_neutrals)
        ColorStory.ACCENT -> stringResource(R.string.title_accent, colorName(capitalized = true))
        ColorStory.TONAL -> stringResource(R.string.title_tonal, colorName(capitalized = false))
        ColorStory.NEIGHBORING -> stringResource(R.string.title_neighboring)
        ColorStory.OPPOSITE -> stringResource(R.string.title_opposite)
        ColorStory.MIXED -> stringResource(R.string.title_mixed)
    }
    return number?.let { stringResource(R.string.title_numbered, base, it) } ?: base
}

@Composable
private fun OutfitTitle.colorName(capitalized: Boolean): String {
    val name = color?.let { stringResource(it.label()) }.orEmpty()
    return if (capitalized) name else name.lowercase()
}

/** The sentence for one reason, or null for the reasons that only lower the score. */
@Composable
fun ScoreTerm.whyLine(day: DayForecast): String? = when (reason) {
    Reason.LAYERS_FIT -> {
        val outerwear = items.firstOrNull { it.category == Category.OUTERWEAR }
        val top = items.first { it.category == Category.TOP }
        if (outerwear != null) {
            stringResource(R.string.why_layers_outerwear, outerwear.describe(), top.describe(), day.minTemperatureC)
        } else {
            stringResource(R.string.why_layers_no_outerwear, top.describe(), day.minTemperatureC, day.maxTemperatureC)
        }
    }
    // WARN: the rain and snow lines say "is" for the outerwear and "are" for the footwear. That fits
    // jackets, coats, sneakers and boots. A new garment that breaks this needs its own sentence.
    Reason.RAIN_READY -> {
        val (outerwear, footwear) = items
        val material = stringResource(footwear.material.label()).lowercase()
        if (day.condition == WeatherCondition.SNOW) {
            stringResource(R.string.why_snow, outerwear.describe(), footwear.describe(), material)
        } else {
            stringResource(
                R.string.why_rain,
                outerwear.describe(),
                footwear.describe(),
                material,
                day.precipitationChancePercent,
            )
        }
    }
    Reason.WARM_WOOL -> stringResource(R.string.why_warm_wool, joinWithAnd(items.map { it.describe() }), day.minTemperatureC)
    Reason.COOL_FABRIC -> {
        val top = items.single()
        stringResource(R.string.why_cool_fabric, stringResource(top.material.label()), top.describe(), day.maxTemperatureC)
    }
    Reason.NEUTRAL_BASE -> {
        val colors = items.map { stringResource(it.color.label()).lowercase() }.distinct()
        if (colors.size == 1) {
            stringResource(R.string.why_neutral_one_color, colors.single().capitalized())
        } else {
            stringResource(R.string.why_neutral_colors, joinWithAnd(colors).capitalized())
        }
    }
    Reason.SINGLE_ACCENT -> if (items.size == 1) {
        stringResource(R.string.why_single_accent_item, items.single().describe())
    } else {
        stringResource(R.string.why_single_accent_color, stringResource(items.first().color.label()))
    }
    Reason.TONAL -> stringResource(R.string.why_tonal, items[0].describe(), items[1].describe())
    Reason.ANALOGOUS -> stringResource(R.string.why_analogous, items[0].describe(), items[1].describe())
    Reason.COMPLEMENTARY -> stringResource(R.string.why_complementary, items[0].describe(), items[1].describe())
    Reason.BALANCED_FIT -> {
        val (top, bottom) = items
        stringResource(
            R.string.why_balanced_fit,
            stringResource(top.fit.label()).lowercase(),
            top.describe(),
            stringResource(bottom.fit.label()).lowercase(),
            bottom.describe(),
        )
    }
    Reason.LIGHT_DARK -> stringResource(R.string.why_light_dark, items.single().describe())
    Reason.OUTDOOR_HOLDS -> stringResource(R.string.why_outdoor_holds, items.single().describe())
    Reason.OUT_OF_SEASON, Reason.RECENT_REPEAT -> null
}

/** An item as a sentence refers to it: its color and garment in lower case, such as "light blue shirt". */
@Composable
private fun ClothingItem.describe(): String =
    "${stringResource(color.label())} ${stringResource(garment.label())}".lowercase()

/** "a", "a and b", "a, b and c". */
@Composable
private fun joinWithAnd(parts: List<String>): String =
    if (parts.size == 1) parts.single() else stringResource(R.string.list_and, parts.dropLast(1).joinToString(", "), parts.last())

private fun String.capitalized() = replaceFirstChar { it.uppercase() }
