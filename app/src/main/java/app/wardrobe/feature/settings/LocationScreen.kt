package app.wardrobe.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.domain.model.Place

@Composable
fun LocationScreen(viewModel: LocationViewModel, onBack: () -> Unit) {
    val place by viewModel.place.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    LocationContent(
        place = place,
        query = query,
        search = search,
        onQueryChange = viewModel::setQuery,
        onChoose = { chosen ->
            viewModel.choose(chosen)
            onBack()
        },
        onBack = onBack,
    )
}

/** Search for the place the forecast is for. Choosing one goes back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationContent(
    place: Place,
    query: String,
    search: LocationSearch,
    onQueryChange: (String) -> Unit,
    onChoose: (Place) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text(stringResource(R.string.location_search_label)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search,
                ),
            )
            when (search) {
                LocationSearch.Idle -> Message(
                    stringResource(R.string.location_current, place.name) + "\n" + stringResource(R.string.location_hint),
                )
                LocationSearch.Searching -> LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                LocationSearch.NoMatches -> Message(stringResource(R.string.location_no_matches, query.trim()))
                LocationSearch.Failed -> Message(stringResource(R.string.location_failed))
                is LocationSearch.Results -> LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(search.places) { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            supportingContent = { Text(result.detail) },
                            leadingContent = { Icon(painterResource(R.drawable.ic_location_on), contentDescription = null) },
                            trailingContent = if (result == place) {
                                { Icon(painterResource(R.drawable.ic_check), stringResource(R.string.location_chosen)) }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable { onChoose(result) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
