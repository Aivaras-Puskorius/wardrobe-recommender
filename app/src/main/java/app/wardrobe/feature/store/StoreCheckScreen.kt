package app.wardrobe.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.engine.BandOutfit
import app.wardrobe.domain.engine.DayBand
import app.wardrobe.domain.engine.StoreCheckResult
import app.wardrobe.feature.outfits.OutfitItemsRow
import app.wardrobe.feature.outfits.outfitTitles
import app.wardrobe.feature.outfits.text
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.ItemPicture

@Composable
fun StoreCheckScreen(viewModel: StoreCheckViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StoreCheckContent(state, onBack = onBack)
}

/** What an item seen in a shop would do for the wardrobe. The rules are in docs/ENGINE.md section 15. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCheckContent(state: StoreCheckUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.store_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            StoreCheckUiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.store_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is StoreCheckUiState.Ready -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Summary(state.result) }
                if (state.result.similar.isNotEmpty()) item { SimilarNote(state.result) }
                item {
                    Text(
                        text = stringResource(R.string.store_outfits),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.result.outfits.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.store_nothing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.result.outfits, key = { it.band }) { BandCard(it, state.result.checked.id) }
            }
        }
    }
}

/** The item's picture and name, and how many of the user's items it would join in the top three outfits. */
@Composable
private fun Summary(result: StoreCheckResult) {
    val item = result.checked
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(GarmentBackdrop),
        ) {
            ItemPicture(item, illustrationPadding = 12.dp, contentDescription = null)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (result.goesWith == 0) {
                    stringResource(R.string.store_count_none)
                } else {
                    stringResource(R.string.store_count, result.goesWith, result.itemCount)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Names the items the user has that this one nearly repeats. */
@Composable
private fun SimilarNote(result: StoreCheckResult) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null)
            Text(
                text = stringResource(R.string.store_similar, result.similar.joinToString(", ") { it.name }),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BandCard(bandOutfit: BandOutfit, anchorId: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(bandOutfit.band.label()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = outfitTitles(listOf(bandOutfit.outfit)).single().text(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            OutfitItemsRow(bandOutfit.outfit.items, anchorId = anchorId)
        }
    }
}

private fun DayBand.label(): Int = when (this) {
    DayBand.HOT -> R.string.store_band_hot
    DayBand.MILD -> R.string.store_band_mild
    DayBand.COOL -> R.string.store_band_cool
    DayBand.COLD -> R.string.store_band_cold
}
