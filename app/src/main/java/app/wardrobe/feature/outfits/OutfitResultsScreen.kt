package app.wardrobe.feature.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.DayForecast
import app.wardrobe.domain.model.Rating
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.ItemPicture
import app.wardrobe.ui.components.NoForecast
import app.wardrobe.ui.components.WeatherSummary
import app.wardrobe.ui.components.dayLabel
import app.wardrobe.ui.pluralLabel

@Composable
fun OutfitResultsScreen(viewModel: OutfitResultsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OutfitResultsContent(
        state = state,
        onBack = onBack,
        onRate = viewModel::rate,
        onShowMore = viewModel::showMore,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitResultsContent(
    state: OutfitResultsUiState,
    onBack: () -> Unit,
    onRate: (outfitId: String, Rating) -> Unit,
    onShowMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            OutfitResultsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            OutfitResultsUiState.NoForecast -> NoForecast(onRetry, Modifier.padding(padding))

            OutfitResultsUiState.NotFound -> EmptyState(
                icon = R.drawable.ic_checkroom,
                title = stringResource(R.string.item_not_found_title),
                body = stringResource(R.string.item_not_found_body),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
            )

            is OutfitResultsUiState.Ready -> if (state.outfits.isEmpty()) {
                Column(Modifier.padding(padding)) {
                    Header(state, Modifier.padding(16.dp))
                    EmptyState(
                        icon = R.drawable.ic_auto_awesome,
                        title = stringResource(R.string.results_none_title),
                        body = noOutfitsBody(state),
                        actionLabel = stringResource(R.string.action_back),
                        onAction = onBack,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Header(state) }
                    items(state.outfits, key = { it.id }) { outfit ->
                        OutfitCardView(outfit, anchorId = state.anchor.id, day = state.day, onRate = onRate)
                    }
                    if (state.canShowMore) {
                        item {
                            OutlinedButton(onClick = onShowMore, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.results_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun noOutfitsBody(state: OutfitResultsUiState.Ready): String = when (val missing = state.missing) {
    null -> stringResource(R.string.results_nothing_fits, state.day.minTemperatureC, state.day.maxTemperatureC)
    Category.OUTERWEAR -> stringResource(R.string.results_missing_outerwear)
    else -> stringResource(R.string.results_missing, stringResource(missing.pluralLabel()).lowercase())
}

@Composable
private fun Header(state: OutfitResultsUiState.Ready, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(GarmentBackdrop),
            ) {
                ItemPicture(state.anchor, illustrationPadding = 8.dp, contentDescription = null)
            }
            Column {
                Text(
                    text = stringResource(R.string.results_built_around),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(state.anchor.name, style = MaterialTheme.typography.titleMedium)
            }
        }
        WeatherSummary(
            location = state.location,
            dayLabel = dayLabel(state.day.date, state.today),
            day = state.day,
            offlineSince = state.offlineSince,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OutfitCardView(outfit: OutfitCard, anchorId: String, day: DayForecast, onRate: (String, Rating) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = outfit.title.text(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            OutfitItemsRow(outfit.items, anchorId)
            Text(
                text = stringResource(R.string.results_why),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            outfit.why.mapNotNull { it.whyLine(day) }.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.results_rate_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                RatingButtons(outfit.rating) { rating -> onRate(outfit.id, rating) }
            }
        }
    }
}
