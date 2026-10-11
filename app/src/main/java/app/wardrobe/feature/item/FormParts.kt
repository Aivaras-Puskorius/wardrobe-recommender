package app.wardrobe.feature.item

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.wardrobe.R
import app.wardrobe.ui.components.DestructiveButton

// Shared by the add and edit item screens.

/** Asks before leaving a form with unsaved input. */
@Composable
fun DiscardDialog(title: String, body: String, onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            DestructiveButton(stringResource(R.string.action_discard), onDiscard)
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.add_item_keep_editing)) }
        },
    )
}
