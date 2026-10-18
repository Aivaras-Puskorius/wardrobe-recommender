package app.wardrobe.feature.wardrobe

import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeFilterTest {

    private val navyCoat = ClothingItem(
        id = "coat",
        name = "Navy coat",
        garment = Garment.COAT,
        color = ItemColor.NAVY,
        material = Material.WOOL,
        fit = Fit.REGULAR,
        warmth = Warmth.WARM,
        seasons = setOf(Season.AUTUMN, Season.WINTER),
        photo = null,
    )

    @Test
    fun emptyFilterMatchesEverything() {
        assertTrue(WardrobeFilter().matches(navyCoat))
        assertFalse(WardrobeFilter().isActive)
    }

    @Test
    fun categoryMustMatch() {
        assertTrue(WardrobeFilter(category = Category.OUTERWEAR).matches(navyCoat))
        assertFalse(WardrobeFilter(category = Category.TOP).matches(navyCoat))
    }

    @Test
    fun anySelectedColorMatches() {
        assertTrue(WardrobeFilter(colors = setOf(ItemColor.BLACK, ItemColor.NAVY)).matches(navyCoat))
        assertFalse(WardrobeFilter(colors = setOf(ItemColor.BLACK)).matches(navyCoat))
    }

    @Test
    fun anySharedSeasonMatches() {
        assertTrue(WardrobeFilter(seasons = setOf(Season.WINTER, Season.SUMMER)).matches(navyCoat))
        assertFalse(WardrobeFilter(seasons = setOf(Season.SUMMER)).matches(navyCoat))
    }

    @Test
    fun allConditionsMustHold() {
        val filter = WardrobeFilter(category = Category.OUTERWEAR, colors = setOf(ItemColor.BLACK))
        assertFalse(filter.matches(navyCoat))
    }

    @Test
    fun sheetFiltersMakeTheFilterActiveButCategoryAloneDoesNot() {
        assertFalse(WardrobeFilter(category = Category.TOP).hasSheetFilters)
        assertTrue(WardrobeFilter(colors = setOf(ItemColor.NAVY)).hasSheetFilters)
        assertTrue(WardrobeFilter(seasons = setOf(Season.WINTER)).hasSheetFilters)
        assertTrue(WardrobeFilter(category = Category.TOP).isActive)
    }
}
