package app.wardrobe.feature.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.ui.components.BottomActionBar
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.ItemPicture
import app.wardrobe.ui.components.WeatherSummary
import app.wardrobe.ui.components.dayLabel
import app.wardrobe.ui.components.icon
import app.wardrobe.ui.label
import java.time.LocalDate

@Composable
fun OutfitGeneratorScreen(
    viewModel: OutfitGeneratorViewModel,
    onShowOutfits: (itemId: String, date: LocalDate) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OutfitGeneratorContent(
        state = state,
        onSelectItem = viewModel::selectItem,
        onSelectDay = viewModel::selectDay,
        onShowOutfits = onShowOutfits,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitGeneratorContent(
    state: OutfitGeneratorUiState,
    onSelectItem: (itemId: String) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onShowOutfits: (itemId: String, date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.tab_outfits)) }, scrollBehavior = scrollBehavior)
        },
        bottomBar = {
            if (state is OutfitGeneratorUiState.Ready) {
                BottomActionBar(
                    label = stringResource(R.string.outfits_show),
                    enabled = state.canShowOutfits,
                    onClick = { state.selectedItem?.let { onShowOutfits(it.id, state.selectedDay.date) } },
                )
            }
        },
    ) { padding ->
        when (state) {
            OutfitGeneratorUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is OutfitGeneratorUiState.Ready -> Generator(state, onSelectItem, onSelectDay, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Generator(
    state: OutfitGeneratorUiState.Ready,
    onSelectItem: (itemId: String) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = state.forecast.days.first().date
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            WeatherSummary(
                location = state.forecast.location,
                dayLabel = dayLabel(state.selectedDay.date, today),
                day = state.selectedDay,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.outfits_day),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.forecast.days.forEach { day ->
                        FilterChip(
                            selected = day.date == state.selectedDay.date,
                            onClick = { onSelectDay(day.date) },
                            label = { Text(dayLabel(day.date, today)) },
                            leadingIcon = {
                                // Described, so TalkBack reads each day's weather with its name.
                                Icon(
                                    painter = painterResource(day.condition.icon()),
                                    contentDescription = stringResource(day.condition.label()),
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.outfits_start_from),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.outfits_start_from_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.items, key = { it.id }) { item ->
            SelectableItemTile(
                item = item,
                selected = item.id == state.selectedItem?.id,
                onClick = { onSelectItem(item.id) },
            )
        }
    }
}

@Composable
private fun SelectableItemTile(item: ClothingItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(GarmentBackdrop)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    } else {
                        Modifier
                    },
                ),
        ) {
            ItemPicture(item, illustrationPadding = 14.dp, contentDescription = null)
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
