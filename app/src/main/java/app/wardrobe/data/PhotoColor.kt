package app.wardrobe.data

import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Lab
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The app color that best matches the item in a photo of one item on a plain background.
 *
 * The photo's border is taken as the background. Pixels in the middle that stand out from it are the item,
 * and each votes for its nearest app color. The color with the most votes wins. When nothing in the middle
 * stands out, as with a white shirt on a white sheet, all the middle pixels vote.
 *
 * INFO(limit): each app color is a single swatch, so in-between shades go to a neighbor, such as dark jeans
 * to navy or burgundy to red. Give each color several swatches if that matters.
 *
 * @param pixels ARGB pixels, row by row, [width] by [height].
 */
fun dominantColor(pixels: IntArray, width: Int, height: Int): ItemColor? {
    if (width == 0 || height == 0) return null
    require(pixels.size == width * height) { "Expected ${width * height} pixels, got ${pixels.size}" }
    val labs = Array(pixels.size) { Lab.of(pixels[it].toLong()) }
    fun at(x: Int, y: Int) = labs[y * width + x]

    val edgeX = maxOf(1, width / 10)
    val edgeY = maxOf(1, height / 10)
    val border = buildList {
        for (y in 0 until height) for (x in 0 until width) {
            if (x < edgeX || x >= width - edgeX || y < edgeY || y >= height - edgeY) add(at(x, y))
        }
    }
    val background = Lab(border.map { it.l }.average(), border.map { it.a }.average(), border.map { it.b }.average())

    val middle = buildList {
        for (y in height / 4 until height - height / 4) for (x in width / 4 until width - width / 4) add(at(x, y))
    }
    val item = middle.filter { it.distanceTo(background) > BACKGROUND_DISTANCE }.ifEmpty { middle }
    return item.groupingBy(::nearestColor).eachCount().maxByOrNull { it.value }?.key
}

/** Pixels closer than this to the background color, in CIE76 units, count as background. */
private const val BACKGROUND_DISTANCE = 12.0

private val swatches = ItemColor.entries.associateWith { Lab.of(it.argb) }

/** The app color nearest to [lab]. Lightness counts half, because lighting moves it more than hue. */
private fun nearestColor(lab: Lab): ItemColor = swatches.minBy { (_, swatch) ->
    sqrt(((lab.l - swatch.l) / 2).pow(2) + (lab.a - swatch.a).pow(2) + (lab.b - swatch.b).pow(2))
}.key

/**
 * The app color that best matches a cut-out item: each pixel at least half opaque votes for its nearest app color.
 * Null when none is.
 *
 * @param pixels ARGB pixels of the cut-out, transparent where the background was.
 */
fun cutoutColor(pixels: IntArray): ItemColor? =
    pixels.filter { it ushr 24 >= OPAQUE_ALPHA }
        .groupingBy { nearestColor(Lab.of(it.toLong() or 0xFF000000)) }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

/** A pixel this opaque or more belongs to the item. Softer edge pixels are left out. */
private const val OPAQUE_ALPHA = 128

/** [pixels] laid over white, fully opaque, for models that take no transparency. */
fun onWhite(pixels: IntArray): IntArray = IntArray(pixels.size) { i ->
    val pixel = pixels[i]
    val alpha = pixel ushr 24
    fun channel(shift: Int) = (((pixel shr shift) and 0xFF) * alpha + 0xFF * (0xFF - alpha)) / 0xFF
    (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
