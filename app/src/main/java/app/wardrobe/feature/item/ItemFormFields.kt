package app.wardrobe.feature.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.wardrobe.R
import app.wardrobe.domain.model.Category
import app.wardrobe.domain.model.Fit
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.domain.model.Material
import app.wardrobe.domain.model.Season
import app.wardrobe.domain.model.Warmth
import app.wardrobe.ui.components.ColorSwatch
import app.wardrobe.ui.components.GarmentBackdrop
import app.wardrobe.ui.components.GarmentImage
import app.wardrobe.ui.label

/** The item fields shared by the add and edit screens, with a live preview of the garment. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemFormFields(
    form: ItemForm,
    onChange: (ItemFormChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(GarmentBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            GarmentImage(form.garment, form.color, contentDescription = null, modifier = Modifier.size(150.dp))
        }

        if (form.suggestion != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auto_awesome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.form_suggestion_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val nameError = form.showErrors && form.nameError
        OutlinedTextField(
            value = form.name,
            onValueChange = { onChange(ItemFormChange.SetName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.form_name)) },
            isError = nameError,
            supportingText = if (nameError) {
                { Text(stringResource(R.string.form_name_error)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )

        Section(stringResource(R.string.item_type)) {
            Garment.entries.forEach { garment ->
                val selected = garment == form.garment
                FilterChip(
                    selected = selected,
                    onClick = { onChange(ItemFormChange.SetGarment(garment)) },
                    label = { Text(stringResource(garment.label())) },
                    leadingIcon = when {
                        selected -> chipIcon(R.drawable.ic_check)
                        garment == form.suggestion?.garment -> chipIcon(R.drawable.ic_auto_awesome)
                        else -> null
                    },
                )
            }
        }

        Section(stringResource(R.string.item_color)) {
            ItemColor.entries.forEach { color ->
                FilterChip(
                    selected = color == form.color,
                    onClick = { onChange(ItemFormChange.SetColor(color)) },
                    label = { Text(stringResource(color.label())) },
                    leadingIcon = { ColorSwatch(color) },
                    trailingIcon = if (color == form.suggestion?.color) chipIcon(R.drawable.ic_auto_awesome) else null,
                )
            }
        }

        Section(stringResource(R.string.item_material)) {
            Material.entries.forEach { material ->
                FilterChip(
                    selected = material == form.material,
                    onClick = { onChange(ItemFormChange.SetMaterial(material)) },
                    label = { Text(stringResource(material.label())) },
                )
            }
        }

        // Footwear has no fit, so the row goes away while shoes are picked.
        if (form.garment.category != Category.FOOTWEAR) {
            Section(stringResource(R.string.item_fit)) {
                Fit.entries.forEach { fit ->
                    FilterChip(
                        selected = fit == form.fit,
                        onClick = { onChange(ItemFormChange.SetFit(fit)) },
                        label = { Text(stringResource(fit.label())) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.item_warmth),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Warmth.entries.forEachIndexed { index, warmth ->
                SegmentedButton(
                    selected = warmth == form.warmth,
                    onClick = { onChange(ItemFormChange.SetWarmth(warmth)) },
                    shape = SegmentedButtonDefaults.itemShape(index, Warmth.entries.size),
                ) {
                    Text(stringResource(warmth.label()))
                }
            }
        }

        Section(stringResource(R.string.item_seasons)) {
            Season.entries.forEach { season ->
                FilterChip(
                    selected = season in form.seasons,
                    onClick = { onChange(ItemFormChange.ToggleSeason(season)) },
                    label = { Text(stringResource(season.label())) },
                )
            }
        }
        if (form.showErrors && form.seasonsError) {
            Text(
                text = stringResource(R.string.form_seasons_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

private fun chipIcon(icon: Int): @Composable () -> Unit = {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
}
