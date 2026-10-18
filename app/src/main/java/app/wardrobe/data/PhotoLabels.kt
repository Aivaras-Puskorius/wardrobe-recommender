package app.wardrobe.data

import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.Material

/** One label an image model gave a photo, such as "Jeans" with confidence 0.9. */
data class PhotoLabel(val text: String, val confidence: Float)

// Turns the labels of ML Kit's standard model and of the ImageNet model into app values.
// Label texts are matched exactly, because ImageNet has both "cardigan" (the sweater) and "Cardigan" (the dog).
// INFO(limit): the two models score on different scales, and the thresholds are first guesses.
// Tune them on photos of real clothes.

/** ML Kit's standard model reports labels it is at least this sure of. */
private const val STANDARD_MIN = 0.5f

/** The ImageNet model spreads its confidence over 1,000 classes, so its useful scores are lower. */
private const val IMAGENET_MIN = 0.25f

private val standardGarments = mapOf(
    "Jersey" to Garment.T_SHIRT,
    "Jeans" to Garment.JEANS,
    "Shorts" to Garment.SHORTS,
    "Sneakers" to Garment.SNEAKERS,
    "Jacket" to Garment.JACKET,
)

/** Labels too broad to trust over a specific one: a shoe may be a boot, outerwear may be a coat. */
private val standardGenericGarments = mapOf(
    "Shoe" to Garment.SNEAKERS,
    "Outerwear" to Garment.JACKET,
)

private val imageNetGarments = mapOf(
    "jersey" to Garment.T_SHIRT,
    "sweatshirt" to Garment.SWEATER,
    "cardigan" to Garment.SWEATER,
    "jean" to Garment.JEANS,
    "swimming trunks" to Garment.SHORTS,
    "miniskirt" to Garment.SKIRT,
    "overskirt" to Garment.SKIRT,
    "hoopskirt" to Garment.SKIRT,
    "running shoe" to Garment.SNEAKERS,
    "Loafer" to Garment.SNEAKERS,
    "cowboy boot" to Garment.BOOTS,
    "trench coat" to Garment.COAT,
    "fur coat" to Garment.COAT,
    "lab coat" to Garment.COAT,
    "suit" to Garment.JACKET,
)

private val standardMaterials = mapOf("Denim" to Material.DENIM, "Wool" to Material.WOOL, "Leather" to Material.LEATHER)
private val imageNetMaterials = mapOf("wool" to Material.WOOL)

/** The garment named by the most confident label above its model's threshold, or null if none names one. */
fun garmentFromLabels(standard: List<PhotoLabel>, imageNet: List<PhotoLabel>): Garment? =
    mostConfident(standard, standardGarments, imageNet, imageNetGarments)
        ?: mostConfident(standard, standardGenericGarments, emptyList(), emptyMap())

/** The material named by the most confident label above its model's threshold, or null. */
fun materialFromLabels(standard: List<PhotoLabel>, imageNet: List<PhotoLabel>): Material? =
    mostConfident(standard, standardMaterials, imageNet, imageNetMaterials)

private fun <T> mostConfident(
    standard: List<PhotoLabel>,
    standardMap: Map<String, T>,
    imageNet: List<PhotoLabel>,
    imageNetMap: Map<String, T>,
): T? {
    val candidates = standard.filter { it.confidence >= STANDARD_MIN }.mapNotNull { label ->
        standardMap[label.text]?.let { it to label.confidence }
    } + imageNet.filter { it.confidence >= IMAGENET_MIN }.mapNotNull { label ->
        imageNetMap[label.text]?.let { it to label.confidence }
    }
    return candidates.maxByOrNull { it.second }?.first
}
