package app.wardrobe.data

import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.Material
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoLabelsTest {

    private fun labels(vararg pairs: Pair<String, Float>) = pairs.map { (text, confidence) -> PhotoLabel(text, confidence) }

    private fun garment(standard: List<PhotoLabel> = emptyList(), imageNet: List<PhotoLabel> = emptyList()) =
        garmentFromLabels(standard, imageNet)

    private fun material(standard: List<PhotoLabel> = emptyList(), imageNet: List<PhotoLabel> = emptyList()) =
        materialFromLabels(standard, imageNet)

    @Test
    fun eachModelNamesGarmentsTheOtherDoesNot() {
        assertEquals(Garment.JACKET, garment(standard = labels("Jacket" to 0.8f)))
        assertEquals(Garment.SWEATER, garment(imageNet = labels("cardigan" to 0.6f)))
        assertEquals(Garment.BOOTS, garment(imageNet = labels("cowboy boot" to 0.4f)))
        assertEquals(Garment.SKIRT, garment(imageNet = labels("miniskirt" to 0.5f)))
        assertEquals(Garment.COAT, garment(imageNet = labels("trench coat" to 0.5f)))
    }

    @Test
    fun theDogBreedCardiganIsNotASweater() {
        assertEquals(null, garment(imageNet = labels("Cardigan" to 0.9f)))
    }

    @Test
    fun labelsBelowTheThresholdAreIgnored() {
        assertEquals(null, garment(standard = labels("Jeans" to 0.4f), imageNet = labels("jersey" to 0.2f)))
    }

    @Test
    fun theMostConfidentLabelWins() {
        assertEquals(
            Garment.SWEATER,
            garment(standard = labels("Jersey" to 0.6f), imageNet = labels("sweatshirt" to 0.7f)),
        )
    }

    @Test
    fun genericLabelsCountOnlyWithoutASpecificOne() {
        assertEquals(Garment.BOOTS, garment(standard = labels("Shoe" to 0.9f), imageNet = labels("cowboy boot" to 0.3f)))
        assertEquals(Garment.SNEAKERS, garment(standard = labels("Shoe" to 0.9f)))
        assertEquals(Garment.JACKET, garment(standard = labels("Outerwear" to 0.8f)))
    }

    @Test
    fun unrelatedLabelsSuggestNothing() {
        assertEquals(null, garment(standard = labels("Dog" to 0.95f), imageNet = labels("golden retriever" to 0.9f)))
        assertEquals(null, material(standard = labels("Dog" to 0.95f)))
    }

    @Test
    fun theMostConfidentMaterialWins() {
        assertEquals(Material.DENIM, material(standard = labels("Denim" to 0.8f)))
        assertEquals(Material.LEATHER, material(standard = labels("Leather" to 0.7f, "Wool" to 0.6f)))
        assertEquals(Material.WOOL, material(imageNet = labels("wool" to 0.3f)))
    }
}
