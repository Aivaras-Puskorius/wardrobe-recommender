package app.wardrobe.feature.item

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.domain.model.Season
import app.wardrobe.ui.components.BottomActionBar
import app.wardrobe.ui.components.ColorSwatch
import app.wardrobe.ui.components.DestructiveButton
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.components.ItemImage
import app.wardrobe.ui.label
import app.wardrobe.ui.pluralLabel
import kotlinx.coroutines.launch

@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStyle: () -> Unit,
    onDeleted: (ClothingItem) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    ItemDetailContent(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onStyle = onStyle,
        onDelete = { scope.launch { viewModel.delete()?.let(onDeleted) } },
    )
}

/** Stateless item detail screen. [showDeleteDialogInitially] lets screenshot tests open the dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailContent(
    state: ItemDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStyle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDeleteDialogInitially: Boolean = false,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(showDeleteDialogInitially) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state is ItemDetailUiState.Loaded) {
                        IconButton(onClick = onEdit) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.edit_item_title))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.item_delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state is ItemDetailUiState.Loaded) {
                BottomActionBar(
                    label = stringResource(R.string.results_style_it),
                    onClick = onStyle,
                    icon = R.drawable.ic_auto_awesome,
                )
            }
        },
    ) { padding ->
        when (state) {
            ItemDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            ItemDetailUiState.NotFound -> EmptyState(
                icon = R.drawable.ic_checkroom,
                title = stringResource(R.string.item_not_found_title),
                body = stringResource(R.string.item_not_found_body),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
            )

            is ItemDetailUiState.Loaded -> ItemDetails(state.item, Modifier.padding(padding))
        }
    }

    if (showDeleteDialog && state is ItemDetailUiState.Loaded) {
        DeleteDialog(
            itemName = state.item.name,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun ItemDetails(item: ClothingItem, modifier: Modifier = Modifier) {
    val colorName = stringResource(item.color.label())
    val garmentName = stringResource(item.garment.label())
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ItemImage(
            item = item,
            shape = MaterialTheme.shapes.extraLarge,
            imagePadding = 56.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentDescription = stringResource(R.string.item_image_description, colorName, garmentName),
        )
        Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                Attribute(
                    icon = R.drawable.ic_category,
                    label = stringResource(R.string.item_type),
                    value = stringResource(R.string.item_type_value, garmentName, stringResource(item.category.pluralLabel())),
                )
                Attribute(icon = R.drawable.ic_palette, label = stringResource(R.string.item_color)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ColorSwatch(item.color, size = 16.dp)
                        Text(colorName)
                    }
                }
                Attribute(
                    icon = R.drawable.ic_texture,
                    label = stringResource(R.string.item_material),
                    value = stringResource(item.material.label()),
                )
                Attribute(
                    icon = R.drawable.ic_device_thermostat,
                    label = stringResource(R.string.item_warmth),
                    value = stringResource(item.warmth.label()),
                )
                Attribute(
                    icon = R.drawable.ic_calendar_month,
                    label = stringResource(R.string.item_seasons),
                    value = seasonsText(item.seasons),
                )
            }
        }
    }
}

@Composable
private fun seasonsText(seasons: Set<Season>): String =
    if (seasons.size == Season.entries.size) {
        stringResource(R.string.item_seasons_all)
    } else {
        seasons.sorted().map { stringResource(it.label()) }.joinToString(", ")
    }

@Composable
private fun Attribute(@DrawableRes icon: Int, label: String, value: String) {
    Attribute(icon, label) { Text(value) }
}

@Composable
private fun Attribute(@DrawableRes icon: Int, label: String, value: @Composable () -> Unit) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = value,
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun DeleteDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
        title = { Text(stringResource(R.string.item_delete_title, itemName)) },
        text = { Text(stringResource(R.string.item_delete_body)) },
        confirmButton = {
            DestructiveButton(stringResource(R.string.action_delete), onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
