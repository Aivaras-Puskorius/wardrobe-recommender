package app.wardrobe.feature.wardrobe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Season
import app.wardrobe.ui.components.ColorSwatch
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.components.ItemCard
import app.wardrobe.ui.label
import app.wardrobe.ui.pluralLabel

/** @param snackbarHostState shows messages from other screens, such as undo after a delete. */
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel,
    snackbarHostState: SnackbarHostState,
    onItemClick: (itemId: String) -> Unit,
    onAddItem: () -> Unit,
    onOpenGaps: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WardrobeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onItemClick = onItemClick,
        onAddItem = onAddItem,
        onOpenGaps = onOpenGaps,
        onSelectCategory = viewModel::selectCategory,
        onToggleColor = viewModel::toggleColor,
        onToggleSeason = viewModel::toggleSeason,
        onClearFilters = viewModel::clearFilters,
    )
}

/** Stateless wardrobe screen. [showFiltersInitially] lets screenshot tests open the filter sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeContent(
    state: WardrobeUiState,
    onItemClick: (itemId: String) -> Unit,
    onAddItem: () -> Unit,
    onOpenGaps: () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onToggleColor: (ItemColor) -> Unit,
    onToggleSeason: (Season) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showFiltersInitially: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilters by rememberSaveable { mutableStateOf(showFiltersInitially) }
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }

    // A filter change should show the new results from the top. The grid would otherwise keep
    // the previous first item in view by its key. Navigating back keeps the scroll position.
    var scrollToTopOnNextItems by rememberSaveable { mutableStateOf(false) }
    if (state is WardrobeUiState.Loaded) {
        LaunchedEffect(state.items) {
            if (scrollToTopOnNextItems) {
                gridState.scrollToItem(0)
                scrollToTopOnNextItems = false
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tab_wardrobe)) },
                actions = {
                    if (state is WardrobeUiState.Loaded && state.hasAnyItems) {
                        IconButton(onClick = onOpenGaps) {
                            Icon(painterResource(R.drawable.ic_lightbulb), stringResource(R.string.gaps_title))
                        }
                        FilterButton(active = state.filter.hasSheetFilters, onClick = { showFilters = true })
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.wardrobe_add_item)) },
                // The extended button hides its text from accessibility services, so the icon carries the label.
                icon = { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.wardrobe_add_item)) },
                onClick = onAddItem,
                expanded = fabExpanded,
            )
        },
    ) { padding ->
        when (state) {
            WardrobeUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is WardrobeUiState.Loaded -> Column(Modifier.padding(padding)) {
                if (state.hasAnyItems) {
                    CategoryChips(selected = state.filter.category, onSelect = {
                        scrollToTopOnNextItems = true
                        onSelectCategory(it)
                    })
                }
                when {
                    !state.hasAnyItems -> EmptyState(
                        icon = R.drawable.ic_checkroom,
                        title = stringResource(R.string.wardrobe_empty_title),
                        body = stringResource(R.string.wardrobe_empty_body),
                        actionLabel = stringResource(R.string.wardrobe_add_item),
                        onAction = onAddItem,
                    )

                    state.items.isEmpty() -> EmptyState(
                        icon = R.drawable.ic_tune,
                        title = stringResource(R.string.wardrobe_no_match_title),
                        body = stringResource(R.string.wardrobe_no_match_body),
                        actionLabel = stringResource(R.string.wardrobe_clear_filters),
                        onAction = {
                            scrollToTopOnNextItems = true
                            onClearFilters()
                        },
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        state = gridState,
                        // The bottom padding keeps the last row clear of the floating button.
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters && state is WardrobeUiState.Loaded) {
        FilterSheet(
            state = state,
            onToggleColor = {
                scrollToTopOnNextItems = true
                onToggleColor(it)
            },
            onToggleSeason = {
                scrollToTopOnNextItems = true
                onToggleSeason(it)
            },
            onClearFilters = {
                scrollToTopOnNextItems = true
                onClearFilters()
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (active) Badge() }) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = stringResource(
                    if (active) R.string.wardrobe_filters_active else R.string.wardrobe_filters,
                ),
            )
        }
    }
}

@Composable
private fun CategoryChips(selected: Category?, onSelect: (Category?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(stringResource(R.string.wardrobe_all), selected = selected == null) { onSelect(null) }
        Category.entries.forEach { category ->
            CategoryChip(stringResource(category.pluralLabel()), selected = selected == category) {
                onSelect(category)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    state: WardrobeUiState.Loaded,
    onToggleColor: (ItemColor) -> Unit,
    onToggleSeason: (Season) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.wardrobe_filters), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.wardrobe_filter_color), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.availableColors.forEach { color ->
                    FilterChip(
                        selected = color in state.filter.colors,
                        onClick = { onToggleColor(color) },
                        label = { Text(stringResource(color.label())) },
                        leadingIcon = { ColorSwatch(color) },
                    )
                }
            }
            Text(stringResource(R.string.wardrobe_filter_season), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Season.entries.forEach { season ->
                    FilterChip(
                        selected = season in state.filter.seasons,
                        onClick = { onToggleSeason(season) },
                        label = { Text(stringResource(season.label())) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onClearFilters, enabled = state.filter.isActive) {
                    Text(stringResource(R.string.wardrobe_clear_all))
                }
                Button(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            }
        }
    }
}
