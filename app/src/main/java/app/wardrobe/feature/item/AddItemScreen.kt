package app.wardrobe.feature.item

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wardrobe.R
import app.wardrobe.ui.components.BottomActionBar
import app.wardrobe.domain.model.ClothingItem
import app.wardrobe.ui.components.GarmentBackdrop
import java.io.FileNotFoundException
import kotlinx.coroutines.launch

@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel,
    onClose: () -> Unit,
    onSaved: (ClothingItem) -> Unit,
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val photoError by viewModel.photoError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val noCamera = stringResource(R.string.add_item_no_camera)
    val photoFailed = stringResource(R.string.add_item_photo_failed)

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraFinished)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.usePickedPhoto {
                context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
            }
        }
    }
    LaunchedEffect(photoError) {
        if (photoError) {
            snackbarHostState.showSnackbar(photoFailed)
            viewModel.photoErrorShown()
        }
    }

    AddItemContent(
        step = step,
        onClose = onClose,
        onTakePhoto = {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", viewModel.startCamera())
            try {
                takePicture.launch(uri)
            } catch (e: ActivityNotFoundException) {
                viewModel.cameraFinished(saved = false)
                scope.launch { snackbarHostState.showSnackbar(noCamera) }
            }
        },
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onFormChange = viewModel::update,
        onSave = { scope.launch { viewModel.save()?.let(onSaved) } },
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless add item screen. [showDiscardDialogInitially] lets screenshot tests open the dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemContent(
    step: AddItemStep,
    onClose: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onFormChange: (ItemFormChange) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showDiscardDialogInitially: Boolean = false,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(showDiscardDialogInitially) }
    val hasInput = step is AddItemStep.Details
    BackHandler(enabled = hasInput) { showDiscardDialog = true }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_item_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (hasInput) showDiscardDialog = true else onClose() }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close))
                    }
                },
            )
        },
        bottomBar = {
            if (step is AddItemStep.Details) BottomActionBar(stringResource(R.string.add_item_save), onSave)
        },
    ) { padding ->
        // Cross-fades between steps. Keyed by step type, so editing the form doesn't animate.
        AnimatedContent(
            targetState = step,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "add item step",
        ) { current ->
            when (current) {
                AddItemStep.ChooseSource -> SourceChoice(onTakePhoto, onPickPhoto, Modifier.padding(padding))
                AddItemStep.Analyzing -> Analyzing(Modifier.padding(padding))
                is AddItemStep.Details -> ItemFormFields(
                    form = current.form,
                    onChange = onFormChange,
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (showDiscardDialog) {
        DiscardDialog(
            title = stringResource(R.string.add_item_discard_title),
            body = stringResource(R.string.add_item_discard_body),
            onDiscard = {
                showDiscardDialog = false
                onClose()
            },
            onKeepEditing = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun SourceChoice(onTakePhoto: () -> Unit, onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SourceCard(
            icon = R.drawable.ic_photo_camera,
            title = stringResource(R.string.add_item_camera),
            body = stringResource(R.string.add_item_camera_body),
            onClick = onTakePhoto,
        )
        SourceCard(
            icon = R.drawable.ic_photo_library,
            title = stringResource(R.string.add_item_gallery),
            body = stringResource(R.string.add_item_gallery_body),
            onClick = onPickPhoto,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lightbulb),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.add_item_tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceCard(@DrawableRes icon: Int, title: String, body: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Analyzing(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(GarmentBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(stringResource(R.string.add_item_analyzing), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.add_item_analyzing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
