package app.wardrobe.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.wardrobe.R
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.ui.theme.WardrobeTheme

/**
 * Fixed light background behind garment images in both themes, like a studio backdrop.
 * Dark garments stay visible on it in dark theme.
 */
val GarmentBackdrop = Color(0xFFEFE9E2)

/**
 * Placeholder illustration of [garment] in [color], shown until items have photos.
 *
 * The drawables have a white fill and a dark outline. A Modulate tint multiplies them by [color],
 * so the fill takes the item's color and the outline stays dark.
 */
@Composable
fun GarmentImage(
    garment: Garment,
    color: ItemColor,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(garment.drawableRes()),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(Color(color.argb), BlendMode.Modulate),
    )
}

@DrawableRes
private fun Garment.drawableRes(): Int = when (this) {
    Garment.T_SHIRT -> R.drawable.garment_t_shirt
    Garment.LONG_SLEEVE -> R.drawable.garment_long_sleeve
    Garment.SHIRT -> R.drawable.garment_shirt
    Garment.SWEATER -> R.drawable.garment_sweater
    Garment.JEANS -> R.drawable.garment_jeans
    Garment.TROUSERS -> R.drawable.garment_trousers
    Garment.SWEATPANTS -> R.drawable.garment_sweatpants
    Garment.SHORTS -> R.drawable.garment_shorts
    Garment.SKIRT -> R.drawable.garment_skirt
    Garment.SNEAKERS -> R.drawable.garment_sneakers
    Garment.BOOTS -> R.drawable.garment_boots
    Garment.JACKET -> R.drawable.garment_jacket
    Garment.COAT -> R.drawable.garment_coat
}

@Preview(backgroundColor = 0xFFEFE9E2, showBackground = true)
@Composable
private fun GarmentImagePreview() {
    WardrobeTheme {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Garment.entries.forEachIndexed { index, garment ->
                GarmentImage(
                    garment = garment,
                    color = ItemColor.entries[index % ItemColor.entries.size],
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}
