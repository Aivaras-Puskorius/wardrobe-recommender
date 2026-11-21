package app.wardrobe.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.wardrobe.R
import app.wardrobe.domain.model.Place
import app.wardrobe.ui.components.DestructiveButton
import app.wardrobe.ui.theme.ThemeMode

/**
 * The Settings tab: theme choice, the forecast's place, the taste profile, removing the sample items while any are
 * left ([sampleItemCount]), and app information. [confirmRemovalInitially] lets screenshot tests open the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    place: Place,
    onOpenLocation: () -> Unit,
    onOpenTasteProfile: () -> Unit,
    sampleItemCount: Int,
    onRemoveSampleItems: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
    confirmRemovalInitially: Boolean = false,
) {
    var confirmingRemoval by rememberSaveable { mutableStateOf(confirmRemovalInitially) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.tab_settings)) }, scrollBehavior = scrollBehavior)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section(stringResource(R.string.settings_appearance)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_dark_mode), contentDescription = null)
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = mode == themeMode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            ) {
                                Text(stringResource(mode.label()))
                            }
                        }
                    }
                }
            }
            Section(stringResource(R.string.settings_weather)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.location_title)) },
                    supportingContent = { Text(place.name) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_location_on), contentDescription = null) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenLocation),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }
            Section(stringResource(R.string.settings_outfit_ideas)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_taste_profile)) },
                    supportingContent = { Text(stringResource(R.string.settings_taste_profile_body)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_thumb_up), contentDescription = null) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenTasteProfile),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }
            if (sampleItemCount > 0) {
                Section(stringResource(R.string.tab_wardrobe)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_remove_samples)) },
                        supportingContent = {
                            Text(pluralStringResource(R.plurals.settings_remove_samples_body, sampleItemCount, sampleItemCount))
                        },
                        leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                        modifier = Modifier.clickable { confirmingRemoval = true },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                }
            }
            Section(stringResource(R.string.settings_about)) {
                InfoRow(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.app_name),
                    body = stringResource(R.string.settings_about_body, versionName),
                )
            }
        }
    }

    if (confirmingRemoval) {
        AlertDialog(
            onDismissRequest = { confirmingRemoval = false },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(R.string.settings_remove_samples_title)) },
            text = {
                Text(pluralStringResource(R.plurals.settings_remove_samples_confirm, sampleItemCount, sampleItemCount))
            },
            confirmButton = {
                DestructiveButton(stringResource(R.string.settings_remove_samples_action)) {
                    confirmingRemoval = false
                    onRemoveSampleItems()
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemoval = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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
            content()
        }
    }
}

@Composable
private fun InfoRow(@DrawableRes icon: Int, title: String, body: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
