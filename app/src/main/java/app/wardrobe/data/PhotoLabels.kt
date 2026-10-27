package app.wardrobe.data

import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.Material

/** One label an image model gave a photo, such as "Jeans" with confidence 0.9. */
data class PhotoLabel(val text: String, val confidence: Float)

// Turns the labels of ML Kit's standard model and of the ImageNet model into app values.
// Label texts are matched exactly, because ImageNet has both "cardigan" (the sweater) and "Cardigan" (the dog).
// The thresholds come from 12 photos of the user's own clothes on the S25 (2026-10-22). A second test with 16 photos
// (2026-10-27) showed the standard model's Denim tag at 75% to 80% on sweatpants and a navy shirt, Jeans at 88% on the
// sweatpants, and Denim at 60% at most on denim shorts. Neither tag tells denim apart, so neither is used.
// INFO(limit): 12 photos is a small sample. Retune when more photos show a pattern.

/**
 * ML Kit's standard model tagged Jeans, Jacket and Shorts on almost every photo, at up to 86%, whatever the
 * garment was. Only garment labels above that tell anything.
 */
private const val STANDARD_GARMENT_MIN = 0.9f

/** "Shoe" scored 67% and more only on footwear, and "Outerwear" appeared only on outerwear. */
private const val STANDARD_GENERIC_MIN = 0.6f

/**
 * In the first test, Wool and Leather were right at 70% and more, and wrong below. In the second, Leather scored 79%
 * on a nylon jacket and 57% on faux-leather boots.
 */
private const val STANDARD_MATERIAL_MIN = 0.7f

/** The ImageNet model's right answers scored 50% and more. */
private const val IMAGENET_MIN = 0.5f

private val standardGarments = mapOf(
    "Jersey" to Garment.T_SHIRT,
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

private val standardMaterials = mapOf("Wool" to Material.WOOL, "Leather" to Material.LEATHER)
private val imageNetMaterials = mapOf("wool" to Material.WOOL)

/** The garment named by the most confident label above its threshold, or null if none names one. */
fun garmentFromLabels(standard: List<PhotoLabel>, imageNet: List<PhotoLabel>): Garment? =
    mostConfident(standard, standardGarments, STANDARD_GARMENT_MIN, imageNet, imageNetGarments)
        ?: mostConfident(standard, standardGenericGarments, STANDARD_GENERIC_MIN, emptyList(), emptyMap())

/** The material named by the most confident label above its threshold, or null. */
fun materialFromLabels(standard: List<PhotoLabel>, imageNet: List<PhotoLabel>): Material? =
    mostConfident(standard, standardMaterials, STANDARD_MATERIAL_MIN, imageNet, imageNetMaterials)

private fun <T> mostConfident(
    standard: List<PhotoLabel>,
    standardMap: Map<String, T>,
    standardMin: Float,
    imageNet: List<PhotoLabel>,
    imageNetMap: Map<String, T>,
): T? {
    val candidates = standard.filter { it.confidence >= standardMin }.mapNotNull { label ->
        standardMap[label.text]?.let { it to label.confidence }
    } + imageNet.filter { it.confidence >= IMAGENET_MIN }.mapNotNull { label ->
        imageNetMap[label.text]?.let { it to label.confidence }
    }
    return candidates.maxByOrNull { it.second }?.first
}

/** The clothes classifier's top class counts from this confidence. */
private const val CLASSIFIER_MIN = 0.6f

/**
 * The garment for a photo. The clothes classifier decides, and the ImageNet model splits its broad classes:
 * pants into jeans or trousers, other shoes into sneakers or boots, outerwear into jacket or coat. When the classifier
 * is unsure, the other models decide alone. When it is sure the photo shows something else, such as a dress or
 * a hat, nothing is suggested.
 *
 * @param classifier labels of the model trained by tools/train_garment_model.py, named as in garments_labels.txt.
 */
fun garmentFor(classifier: List<PhotoLabel>, standard: List<PhotoLabel>, imageNet: List<PhotoLabel>): Garment? {
    val top = classifier.maxByOrNull { it.confidence }
    if (top == null || top.confidence < CLASSIFIER_MIN) return garmentFromLabels(standard, imageNet)
    fun imageNetSays(vararg texts: String) = imageNet.any { it.text in texts && it.confidence >= IMAGENET_MIN }
    return when (top.text) {
        "t_shirt" -> Garment.T_SHIRT
        "shirt" -> Garment.SHIRT
        "sweater" -> Garment.SWEATER
        "shorts" -> Garment.SHORTS
        "skirt" -> Garment.SKIRT
        "pants" -> if (imageNetSays("jean")) Garment.JEANS else Garment.TROUSERS
        "boots" -> Garment.BOOTS
        "shoes" -> if (imageNetSays("cowboy boot")) Garment.BOOTS else Garment.SNEAKERS
        "outerwear" -> if (imageNetSays("trench coat", "fur coat", "lab coat")) Garment.COAT else Garment.JACKET
        else -> null
    }
}
