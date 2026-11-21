package app.wardrobe.feature.gaps

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.engine.Gap
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.ui.components.ColorSwatch
import app.wardrobe.ui.components.EmptyState
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.GarmentImage
import app.wardrobe.ui.label

@Composable
fun GapsScreen(viewModel: GapsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GapsContent(state, onBack = onBack)
}

/** The garments and colors that would go into outfit ideas with the most of the user's items. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GapsContent(state: GapsUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gaps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state is GapsUiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.gaps_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state is GapsUiState.Ready && state.gaps.isEmpty() -> EmptyState(
                icon = R.drawable.ic_lightbulb,
                title = stringResource(R.string.gaps_empty_title),
                body = stringResource(R.string.gaps_empty_body),
                modifier = Modifier.padding(padding),
            )

            state is GapsUiState.Ready -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.gaps_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.gaps, key = { it.garment }) { gap -> GapCard(gap, state.itemCount) }
            }
        }
    }
}

@Composable
private fun GapCard(gap: Gap, itemCount: Int) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(GarmentBackdrop)
                    .padding(8.dp),
            ) {
                GarmentImage(gap.garment, gap.colors.first(), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(gap.garment.label()), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    gap.colors.take(SHOWN_COLORS).forEach { ColorSwatch(it, size = 14.dp) }
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = colorList(gap.colors),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.gaps_goes_with, gap.goesWith, itemCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** "Beige", "Brown or navy", "White, cream or beige", "White, cream, beige or 12 more". */
@Composable
private fun colorList(colors: List<ItemColor>): String {
    val names = colors.take(SHOWN_COLORS).mapIndexed { index, color ->
        val name = stringResource(color.label())
        if (index == 0) name else name.lowercase()
    }
    val more = colors.size - SHOWN_COLORS
    return when {
        more > 0 -> stringResource(R.string.gaps_colors_more, names.joinToString(", "), more)
        names.size == 1 -> names.single()
        else -> stringResource(R.string.list_or, names.dropLast(1).joinToString(", "), names.last())
    }
}

private const val SHOWN_COLORS = 3
