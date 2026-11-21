package app.wardrobe.feature.taste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.engine.PairTaste
import app.wardrobe.domain.engine.TastePair
import app.wardrobe.feature.history.shortDate
import app.wardrobe.ui.components.ColorSwatch
import app.wardrobe.ui.components.DestructiveButton
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.label
import app.wardrobe.ui.pluralLabel

@Composable
fun TasteProfileScreen(viewModel: TasteProfileViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TasteProfileContent(state, onBack = onBack, onStartOver = viewModel::startOver)
}

/**
 * The pairs the ratings taught, liked and avoided, and Start over. [confirmInitially] lets screenshot tests open
 * the Start over dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteProfileContent(
    state: TasteProfileUiState,
    onBack: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
    confirmInitially: Boolean = false,
) {
    var confirming by rememberSaveable { mutableStateOf(confirmInitially) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_taste_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state is TasteProfileUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state is TasteProfileUiState.Ready && state.ratingCount == 0 -> EmptyState(
                icon = R.drawable.ic_thumb_up,
                title = stringResource(R.string.taste_empty_title),
                body = stringResource(R.string.taste_empty_body),
                modifier = Modifier.padding(padding),
            )

            state is TasteProfileUiState.Ready -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = state.summary(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.liked.isNotEmpty()) PairSection(stringResource(R.string.taste_liked), state.liked)
                if (state.avoided.isNotEmpty()) PairSection(stringResource(R.string.taste_avoided), state.avoided)
                OutlinedButton(onClick = { confirming = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.taste_start_over))
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.taste_start_over_title)) },
            text = { Text(stringResource(R.string.taste_start_over_body)) },
            confirmButton = {
                DestructiveButton(stringResource(R.string.taste_start_over)) {
                    confirming = false
                    onStartOver()
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun TasteProfileUiState.Ready.summary(): String = when (since) {
    null -> pluralStringResource(R.plurals.taste_summary, ratingCount, ratingCount)
    else -> pluralStringResource(R.plurals.taste_summary_since, ratingCount, ratingCount, since.shortDate())
}

@Composable
private fun PairSection(title: String, pairs: List<PairTaste>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier
                .padding(start = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                pairs.forEach { PairRow(it) }
            }
        }
    }
}

/** A pair by name, such as "Black with light blue", with the ratings behind it. */
@Composable
private fun PairRow(taste: PairTaste) {
    val pair = taste.pair
    ListItem(
        headlineContent = { Text(pair.text()) },
        supportingContent = { Text(taste.counts()) },
        leadingContent = {
            val icon = if (pair is TastePair.Colors) R.drawable.ic_palette else R.drawable.ic_checkroom
            Icon(painterResource(icon), contentDescription = null)
        },
        trailingContent = if (pair is TastePair.Colors) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ColorSwatch(pair.first, size = 20.dp)
                    ColorSwatch(pair.second, size = 20.dp)
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun TastePair.text(): String {
    val (first, second) = when (this) {
        is TastePair.Colors -> stringResource(first.label()) to stringResource(second.label())
        is TastePair.Garments -> stringResource(first.pluralLabel()) to stringResource(second.pluralLabel())
    }
    return stringResource(R.string.taste_pair, first, second.lowercase())
}

/** "2 likes", "1 dislike" or "2 likes, 1 dislike". */
@Composable
private fun PairTaste.counts(): String {
    val likeText = pluralStringResource(R.plurals.taste_likes, likes, likes)
    val dislikeText = pluralStringResource(R.plurals.taste_dislikes, dislikes, dislikes)
    return when {
        dislikes == 0 -> likeText
        likes == 0 -> dislikeText
        else -> stringResource(R.string.taste_counts, likeText, dislikeText)
    }
}
