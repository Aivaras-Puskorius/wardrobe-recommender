package app.wardrobe.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.model.Rating
import app.wardrobe.feature.outfits.OutfitItemsRow
import app.wardrobe.feature.outfits.RatingButtons
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.ItemPicture
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state, onBack = onBack, onFilterChange = viewModel::setFilter, onRate = viewModel::rate)
}

/**
 * The rated outfits: a list of all of them, or a gallery of the liked ones. A gallery tile opens the outfit in a sheet,
 * where its rating can be changed. [openOutfitInitially] lets screenshot tests open the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onRate: (outfitId: String, Rating) -> Unit,
    modifier: Modifier = Modifier,
    openOutfitInitially: String? = null,
) {
    var openOutfitId by rememberSaveable { mutableStateOf(openOutfitInitially) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            HistoryUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is HistoryUiState.Ready -> Column(Modifier.padding(padding)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filter == state.filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(stringResource(filter.label())) },
                        )
                    }
                }
                when {
                    state.outfits.isEmpty() && state.filter == HistoryFilter.ALL -> EmptyState(
                        icon = R.drawable.ic_history,
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                    )
                    state.outfits.isEmpty() -> EmptyState(
                        icon = R.drawable.ic_thumb_up,
                        title = stringResource(R.string.history_liked_empty_title),
                        body = stringResource(R.string.history_liked_empty_body),
                    )
                    state.filter == HistoryFilter.ALL -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.outfits, key = { it.id }) { outfit -> RatedOutfitCard(outfit, onRate) }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.outfits, key = { it.id }) { outfit ->
                            GalleryTile(outfit, onOpen = { openOutfitId = outfit.id })
                        }
                    }
                }
            }
        }
    }

    val openOutfit = (state as? HistoryUiState.Ready)?.outfits?.firstOrNull { it.id == openOutfitId }
    // A rating changed in the sheet can take the outfit out of the gallery. Forget it, so it doesn't open again later.
    LaunchedEffect(openOutfit == null) { if (openOutfit == null) openOutfitId = null }
    if (openOutfit != null) {
        ModalBottomSheet(onDismissRequest = { openOutfitId = null }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RatedOutfitDetails(openOutfit, onRate)
            }
        }
    }
}

@Composable
private fun RatedOutfitCard(outfit: RatedOutfit, onRate: (String, Rating) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RatedOutfitDetails(outfit, onRate)
        }
    }
}

/** The date, the items and the rating buttons of one outfit. */
@Composable
private fun RatedOutfitDetails(outfit: RatedOutfit, onRate: (String, Rating) -> Unit) {
    OutfitItemsRow(outfit.items, anchorId = null)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.history_rated_on, outfit.ratedOn.shortDate()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        RatingButtons(outfit.rating) { rating -> onRate(outfit.id, rating) }
    }
}

/** A liked outfit as up to four pictures in a square, with the date below. */
@Composable
private fun GalleryTile(outfit: RatedOutfit, onOpen: () -> Unit) {
    val names = outfit.items.joinToString(", ") { it.name }
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = names },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(GarmentBackdrop)
                .padding(4.dp),
        ) {
            outfit.items.chunked(2).forEach { pair ->
                Row(Modifier.weight(1f)) {
                    pair.forEach { item ->
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            ItemPicture(item, illustrationPadding = 8.dp, contentDescription = null)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(
            text = stringResource(R.string.history_rated_on, outfit.ratedOn.shortDate()),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// INFO(limit): English month names to match the English-only UI. Use the app locale once translations exist.
private fun LocalDate.shortDate(): String = format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

private fun HistoryFilter.label(): Int = when (this) {
    HistoryFilter.ALL -> R.string.history_all
    HistoryFilter.LIKED -> R.string.history_liked
}
