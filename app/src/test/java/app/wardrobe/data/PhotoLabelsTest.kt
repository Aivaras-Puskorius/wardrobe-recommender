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
        assertEquals(Garment.JACKET, garment(standard = labels("Jacket" to 0.92f)))
        assertEquals(Garment.SWEATER, garment(imageNet = labels("cardigan" to 0.6f)))
        assertEquals(Garment.BOOTS, garment(imageNet = labels("cowboy boot" to 0.55f)))
        assertEquals(Garment.SKIRT, garment(imageNet = labels("miniskirt" to 0.5f)))
        assertEquals(Garment.COAT, garment(imageNet = labels("trench coat" to 0.5f)))
    }

    /** Photo 4 of the S25 test: light grey sweatpants. The standard model tags jeans, jacket and shorts on almost every photo. */
    @Test
    fun theStandardModelsUsualClothingTagsSuggestNothing() {
        val sweatpants = labels("Jeans" to 0.83f, "Jacket" to 0.69f, "Shorts" to 0.64f, "Denim" to 0.55f)
        assertEquals(null, garment(standard = sweatpants, imageNet = labels("cloak" to 0.42f)))
        assertEquals(null, material(standard = sweatpants))
    }

    /** Photo 7 of the S25 test: black faux-leather boots. */
    @Test
    fun aConfidentShoeLabelBeatsTheJeansTag() {
        val boots = labels("Shoe" to 0.94f, "Jeans" to 0.64f, "Sneakers" to 0.59f, "Leather" to 0.51f)
        assertEquals(Garment.SNEAKERS, garment(standard = boots))
        assertEquals(null, material(standard = boots))
    }

    private fun classified(
        classifier: List<PhotoLabel>,
        standard: List<PhotoLabel> = emptyList(),
        imageNet: List<PhotoLabel> = emptyList(),
    ) = garmentFor(classifier, standard, imageNet)

    @Test
    fun theClothesClassifierNamesWhatTheOtherModelsCannot() {
        assertEquals(Garment.SHIRT, classified(labels("shirt" to 0.8f)))
        assertEquals(Garment.TROUSERS, classified(labels("pants" to 0.8f)))
        assertEquals(Garment.SWEATER, classified(labels("sweater" to 0.8f)))
        assertEquals(Garment.SKIRT, classified(labels("skirt" to 0.8f)))
        assertEquals(Garment.BOOTS, classified(labels("boots" to 0.8f)))
    }

    @Test
    fun theOtherModelsSplitTheClassifiersBroadClasses() {
        assertEquals(Garment.JEANS, classified(labels("pants" to 0.8f), imageNet = labels("jean" to 0.6f)))
        assertEquals(Garment.COAT, classified(labels("outerwear" to 0.8f), imageNet = labels("trench coat" to 0.55f)))
        assertEquals(Garment.JACKET, classified(labels("outerwear" to 0.8f)))
        assertEquals(Garment.BOOTS, classified(labels("shoes" to 0.8f), imageNet = labels("cowboy boot" to 0.55f)))
        assertEquals(Garment.SNEAKERS, classified(labels("shoes" to 0.8f)))
    }

    /**
     * Second S25 test (2026-10-27): the standard model gave Denim 75% to 80% on sweatpants and a navy shirt,
     * Jeans 88% on the sweatpants, and Denim 60% at most on denim shorts. So neither tag counts.
     */
    @Test
    fun theStandardModelsDenimAndJeansTagsAreIgnored() {
        val sweatpants = labels("Jeans" to 0.88f, "Outerwear" to 0.87f, "Denim" to 0.8f)
        assertEquals(Garment.TROUSERS, classified(labels("pants" to 0.99f), standard = sweatpants))
        assertEquals(Garment.TROUSERS, classified(labels("pants" to 0.8f), standard = labels("Jeans" to 0.95f)))
        assertEquals(null, material(standard = labels("Denim" to 0.78f)))
        assertEquals(null, garment(standard = labels("Jeans" to 0.95f)))
    }

    /** The black nylon jacket: Leather 79% in the second S25 test, and leather in the first test too. */
    @Test
    fun theStandardModelsLeatherTagIsIgnored() {
        assertEquals(null, material(standard = labels("Leather" to 0.79f)))
        assertEquals(Material.WOOL, material(standard = labels("Leather" to 0.9f, "Wool" to 0.72f)))
    }

    /** Light grey sweatpants on the S25: the usual Jeans and Denim tags don't make them jeans. */
    @Test
    fun weakDenimTagsLeaveTrousersAsTrousers() {
        val sweatpants = labels("Jeans" to 0.83f, "Denim" to 0.55f)
        assertEquals(Garment.TROUSERS, classified(labels("pants" to 0.8f), standard = sweatpants))
    }

    @Test
    fun aConfidentOtherSuggestsNothing() {
        assertEquals(null, classified(labels("other" to 0.9f), standard = labels("Jacket" to 0.95f)))
    }

    @Test
    fun anUnsureClassifierLeavesItToTheOtherModels() {
        assertEquals(Garment.T_SHIRT, classified(labels("shirt" to 0.3f), imageNet = labels("jersey" to 0.77f)))
        assertEquals(null, classified(emptyList()))
    }

    @Test
    fun theDogBreedCardiganIsNotASweater() {
        assertEquals(null, garment(imageNet = labels("Cardigan" to 0.9f)))
    }

    @Test
    fun labelsBelowTheThresholdAreIgnored() {
        assertEquals(null, garment(standard = labels("Jacket" to 0.89f), imageNet = labels("jersey" to 0.49f)))
    }

    @Test
    fun theMostConfidentLabelWins() {
        assertEquals(
            Garment.SWEATER,
            garment(standard = labels("Jersey" to 0.95f), imageNet = labels("sweatshirt" to 0.97f)),
        )
    }

    @Test
    fun genericLabelsCountOnlyWithoutASpecificOne() {
        assertEquals(Garment.BOOTS, garment(standard = labels("Shoe" to 0.9f), imageNet = labels("cowboy boot" to 0.55f)))
        assertEquals(Garment.SNEAKERS, garment(standard = labels("Shoe" to 0.6f)))
        assertEquals(Garment.JACKET, garment(standard = labels("Outerwear" to 0.6f)))
        assertEquals(null, garment(standard = labels("Shoe" to 0.59f)))
    }

    @Test
    fun unrelatedLabelsSuggestNothing() {
        assertEquals(null, garment(standard = labels("Dog" to 0.95f), imageNet = labels("golden retriever" to 0.9f)))
        assertEquals(null, material(standard = labels("Dog" to 0.95f)))
    }

    @Test
    fun theMostConfidentMaterialWins() {
        assertEquals(Material.WOOL, material(standard = labels("Wool" to 0.76f, "Leather" to 0.7f)))
        assertEquals(null, material(standard = labels("Wool" to 0.69f)))
        assertEquals(Material.WOOL, material(imageNet = labels("wool" to 0.5f)))
    }
}
