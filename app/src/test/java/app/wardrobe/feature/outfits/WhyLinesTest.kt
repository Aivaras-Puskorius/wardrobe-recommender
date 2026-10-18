package app.wardrobe.feature.outfits

import androidx.compose.ui.test.junit4.createComposeRule
import app.wardrobe.data.SampleData
import app.wardrobe.domain.engine.Reason
import app.wardrobe.domain.engine.ScoreTerm
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.WeatherCondition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The sentences of the material, fit and outdoor rules, as docs/ENGINE.md section 4 words them. */
@RunWith(RobolectricTestRunner::class)
class WhyLinesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun items(vararg ids: String) = ids.map { id -> SampleData.items.first { it.id == id } }

    private fun day(low: Int, high: Int, condition: WeatherCondition, chance: Int) =
        DayForecast(LocalDate.of(2026, 10, 15), low, high, condition, chance)

    private fun ScoreTerm.line(day: DayForecast): String? {
        var line: String? = null
        composeRule.setContent { line = whyLine(day) }
        composeRule.waitForIdle()
        return line
    }

    @Test
    fun rainLineNamesTheMaterials() {
        val term = ScoreTerm(Reason.RAIN_READY, 2, items("olive-rain-jacket", "brown-boots"))
        assertEquals(
            "The olive jacket is waterproof and the brown boots are leather, so both are ready for rain (80% chance).",
            term.line(day(5, 11, WeatherCondition.RAIN, 80)),
        )
    }

    @Test
    fun snowLineNamesTheMaterials() {
        val term = ScoreTerm(Reason.RAIN_READY, 2, items("olive-rain-jacket", "brown-boots"))
        assertEquals(
            "The olive jacket is waterproof and the brown boots are leather, so both are ready for snow.",
            term.line(day(-1, 4, WeatherCondition.SNOW, 60)),
        )
    }

    @Test
    fun woolLineNamesEveryWoolItem() {
        val term = ScoreTerm(Reason.WARM_WOOL, 1, items("camel-coat", "cream-knit"))
        assertEquals(
            "Wool in the camel coat and cream sweater keeps you warm on a 3° morning.",
            term.line(day(3, 9, WeatherCondition.CLOUDY, 30)),
        )
    }

    @Test
    fun outdoorLineNamesTheOuterwear() {
        val term = ScoreTerm(Reason.OUTDOOR_HOLDS, 1, items("olive-rain-jacket"))
        assertEquals(
            "The olive jacket goes with the rest, so the look holds outdoors too.",
            term.line(day(4, 12, WeatherCondition.CLEAR, 5)),
        )
    }

    @Test
    fun fitLineNamesBothFits() {
        val term = ScoreTerm(Reason.BALANCED_FIT, 1, items("olive-overshirt", "dark-jeans"))
        assertEquals(
            "The loose olive shirt with the fitted denim jeans keeps the proportions balanced.",
            term.line(day(7, 14, WeatherCondition.PARTLY_CLOUDY, 10)),
        )
    }

    @Test
    fun coolFabricLineNamesTheMaterial() {
        val term = ScoreTerm(Reason.COOL_FABRIC, 1, items("white-tee"))
        assertEquals(
            "Cotton breathes, so the white t-shirt keeps you cool at 26°.",
            term.line(day(19, 26, WeatherCondition.CLEAR, 0)),
        )
    }
}
