package app.wardrobe.feature.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wardrobe.R
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Rating
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.ItemPicture

/**
 * An outfit's items side by side with their names. The item with [anchorId], if any, is outlined.
 * There are four slots in every row, so tiles line up between outfits of three and four items.
 */
@Composable
fun OutfitItemsRow(items: List<ClothingItem>, anchorId: String?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            OutfitItemTile(item, isAnchor = item.id == anchorId, modifier = Modifier.weight(1f))
        }
        repeat(MAX_ITEMS - items.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** Like and dislike toggles for an outfit with [rating]. */
@Composable
fun RatingButtons(rating: Rating?, onRate: (Rating) -> Unit) {
    RatingButton(
        checked = rating == Rating.LIKED,
        onClick = { onRate(Rating.LIKED) },
        icon = R.drawable.ic_thumb_up,
        checkedIcon = R.drawable.ic_thumb_up_filled,
        description = stringResource(R.string.results_like),
    )
    RatingButton(
        checked = rating == Rating.DISLIKED,
        onClick = { onRate(Rating.DISLIKED) },
        icon = R.drawable.ic_thumb_down,
        checkedIcon = R.drawable.ic_thumb_down_filled,
        description = stringResource(R.string.results_dislike),
    )
}

@Composable
private fun RatingButton(checked: Boolean, onClick: () -> Unit, icon: Int, checkedIcon: Int, description: String) {
    IconToggleButton(checked = checked, onCheckedChange = { onClick() }) {
        Icon(painterResource(if (checked) checkedIcon else icon), contentDescription = description)
    }
}

@Composable
private fun OutfitItemTile(item: ClothingItem, isAnchor: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(GarmentBackdrop)
                .then(
                    if (isAnchor) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    } else {
                        Modifier
                    },
                ),
        ) {
            ItemPicture(item, illustrationPadding = 8.dp, contentDescription = null)
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val MAX_ITEMS = 4
