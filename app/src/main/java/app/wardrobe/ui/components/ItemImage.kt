package app.wardrobe.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import app.wardrobe.data.PhotoStore
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import coil3.compose.AsyncImage

/** Provided by the app's navigation. Null in previews and screenshot tests, where nothing animates. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The enter and exit animation of the current navigation entry. Null outside navigation. */
val LocalEntryAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Where item photos live. Provided by the app. Null in screenshot tests without photos, which show illustrations. */
val LocalPhotoStore = staticCompositionLocalOf<PhotoStore?> { null }

/**
 * An item's image on the garment backdrop. The image travels between the screens that show it,
 * such as from a grid card to the detail screen, when navigation provides the animation scopes.
 */
@Composable
fun ItemImage(
    item: ClothingItem,
    shape: Shape,
    imagePadding: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalEntryAnimatedScope.current
    val shared = if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "item-image-${item.id}"),
                animatedVisibilityScope = animatedScope,
            )
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .then(shared)
            .clip(shape)
            .background(GarmentBackdrop),
    ) {
        ItemPicture(item, illustrationPadding = imagePadding, contentDescription = contentDescription)
    }
}

/**
 * Fills its parent with the item's photo, cropped to fit, or with the garment illustration
 * inset by [illustrationPadding] when there is no photo or it fails to load. Without a [garment] there is no illustration.
 */
@Composable
fun ItemPicture(
    photo: String?,
    garment: Garment?,
    color: ItemColor,
    illustrationPadding: Dp,
    contentDescription: String?,
) {
    val file = photo?.let { LocalPhotoStore.current?.file(it) }
    var failed by remember(file) { mutableStateOf(false) }
    if (file != null && !failed) {
        AsyncImage(
            model = file,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { failed = true },
        )
    } else if (garment != null) {
        GarmentImage(
            garment = garment,
            color = color,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(illustrationPadding),
        )
    }
}

@Composable
fun ItemPicture(item: ClothingItem, illustrationPadding: Dp, contentDescription: String?) =
    ItemPicture(item.photo, item.garment, item.color, illustrationPadding, contentDescription)
