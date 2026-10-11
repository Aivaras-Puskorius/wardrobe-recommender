package app.wardrobe.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Filled error-colored button for the confirm action of a dialog that deletes or discards something.
 * A filled shape keeps it apart from the text button next to it in both themes. In dark theme the
 * error and primary text colors are too close to tell apart.
 */
@Composable
fun DestructiveButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Text(label)
    }
}
