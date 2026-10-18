package app.wardrobe.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import app.wardrobe.AppContainer
import app.wardrobe.R
import app.wardrobe.domain.model.Garment
import app.wardrobe.domain.model.ItemColor
import app.wardrobe.ui.components.LocalEntryAnimatedScope
import app.wardrobe.ui.components.LocalPhotoStore
import app.wardrobe.ui.components.LocalSharedTransitionScope
import app.wardrobe.ui.label
import java.time.LocalDate
import kotlinx.coroutines.launch
import app.wardrobe.feature.item.AddItemScreen
import app.wardrobe.feature.item.AddItemViewModel
import app.wardrobe.feature.item.EditItemScreen
import app.wardrobe.feature.item.EditItemViewModel
import app.wardrobe.feature.item.ItemDetailScreen
import app.wardrobe.feature.item.ItemDetailViewModel
import app.wardrobe.feature.outfits.OutfitGeneratorScreen
import app.wardrobe.feature.outfits.OutfitGeneratorViewModel
import app.wardrobe.feature.outfits.OutfitResultsScreen
import app.wardrobe.feature.outfits.OutfitResultsViewModel
import app.wardrobe.feature.settings.SettingsScreen
import app.wardrobe.feature.wardrobe.WardrobeScreen
import app.wardrobe.feature.wardrobe.WardrobeViewModel

/** The app's root: the bottom bar and the screen of the selected tab. */
@Composable
fun WardrobeApp(container: AppContainer) {
    val navigationState = rememberNavigationState(startRoute = WardrobeRoute, topLevelRoutes = TopLevelRoutes)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    val appResources = context.applicationContext.resources
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    // Application resources, so view models holding this don't hold on to the activity.
    val defaultItemName: (Garment, ItemColor) -> String = remember(appResources) {
        { garment, color ->
            appResources.getString(
                R.string.item_default_name,
                appResources.getString(color.label()),
                appResources.getString(garment.label()).lowercase(),
            )
        }
    }

    /** Shows [message] on the Wardrobe screen and runs [onAction] if the user taps the action. */
    fun showMessage(message: String, actionLabel: String? = null, onAction: suspend () -> Unit = {}) {
        scope.launch {
            val duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            val result = snackbarHostState.showSnackbar(message, actionLabel, duration = duration)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    val entryProvider = entryProvider<NavKey> {
        entry<WardrobeRoute> {
            WithEntryAnimation {
                WardrobeScreen(
                    viewModel = viewModel { WardrobeViewModel(container.wardrobeRepository) },
                    snackbarHostState = snackbarHostState,
                    onItemClick = { navigator.navigate(ItemDetailRoute(it)) },
                    onAddItem = { navigator.navigate(AddItemRoute) },
                )
            }
        }
        entry<AddItemRoute> {
            AddItemScreen(
                viewModel = viewModel {
                    AddItemViewModel(
                        repository = container.wardrobeRepository,
                        recognize = container.recognizer::recognize,
                        defaultName = defaultItemName,
                        photoStore = container.photoStore,
                    )
                },
                onClose = navigator::goBack,
                onSaved = { item ->
                    navigator.goBack()
                    showMessage(resources.getString(R.string.item_added, item.name))
                },
            )
        }
        entry<ItemDetailRoute> { route ->
            WithEntryAnimation {
                ItemDetailScreen(
                    viewModel = viewModel { ItemDetailViewModel(route.itemId, container.wardrobeRepository) },
                    onBack = navigator::goBack,
                    onEdit = { navigator.navigate(EditItemRoute(route.itemId)) },
                    onStyle = {
                        val today = container.forecast().days.first().date
                        navigator.navigate(OutfitResultsRoute(route.itemId, today.toEpochDay()))
                    },
                    onDeleted = { item ->
                        navigator.goBack()
                        showMessage(
                            message = resources.getString(R.string.item_deleted, item.name),
                            actionLabel = resources.getString(R.string.action_undo),
                            onAction = { container.wardrobeRepository.add(item) },
                        )
                    },
                )
            }
        }
        entry<EditItemRoute> { route ->
            EditItemScreen(
                viewModel = viewModel {
                    EditItemViewModel(route.itemId, container.wardrobeRepository, defaultItemName)
                },
                onClose = navigator::goBack,
                onSaved = { navigator.goBack() },
            )
        }
        entry<OutfitsRoute> {
            OutfitGeneratorScreen(
                viewModel = viewModel { OutfitGeneratorViewModel(container.wardrobeRepository, container.forecast()) },
                onShowOutfits = { itemId, date -> navigator.navigate(OutfitResultsRoute(itemId, date.toEpochDay())) },
            )
        }
        entry<OutfitResultsRoute> { route ->
            OutfitResultsScreen(
                viewModel = viewModel {
                    OutfitResultsViewModel(
                        itemId = route.itemId,
                        date = LocalDate.ofEpochDay(route.epochDay),
                        repository = container.wardrobeRepository,
                        forecast = container.forecast(),
                    )
                },
                onBack = navigator::goBack,
            )
        }
        entry<SettingsRoute> {
            val themeMode by container.themeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = { container.themeMode.value = it },
                versionName = versionName,
            )
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = navigationState.isAtTabRoot,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                WardrobeNavigationBar(
                    selected = navigationState.topLevelRoute,
                    onSelect = navigator::navigate,
                )
            }
        },
        // Each screen handles the system bar insets that the bottom bar doesn't cover.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this,
                LocalPhotoStore provides container.photoStore,
            ) {
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = navigator::goBack,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                    sharedTransitionScope = this,
                    // Navigation 3's default predictive back animation shrinks the old screen to 70%
                    // without fading it, which looked like a frozen screenshot. Back by gesture or by
                    // the system back button now cross-fades like the app's own back arrow.
                    predictivePopTransitionSpec = { PopTransition() },
                )
            }
        }
    }
}

private val PopTransition = defaultPopTransitionSpec<NavKey>()

private enum class Tab(
    val route: NavKey,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    WARDROBE(WardrobeRoute, R.string.tab_wardrobe, R.drawable.ic_checkroom, R.drawable.ic_checkroom),
    OUTFITS(OutfitsRoute, R.string.tab_outfits, R.drawable.ic_auto_awesome, R.drawable.ic_auto_awesome_filled),
    SETTINGS(SettingsRoute, R.string.tab_settings, R.drawable.ic_settings, R.drawable.ic_settings_filled),
}

@Composable
private fun WardrobeNavigationBar(selected: NavKey, onSelect: (NavKey) -> Unit) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            val isSelected = tab.route == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/** Hands this entry's enter and exit animation to its screen, for shared element transitions. */
@Composable
private fun WithEntryAnimation(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEntryAnimatedScope provides LocalNavAnimatedContentScope.current, content = content)
}
