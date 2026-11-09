package corsica.universita.carnet.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import corsica.universita.carnet.ui.record.RecordScreen
import corsica.universita.carnet.ui.sounds.SoundDetailScreen
import corsica.universita.carnet.ui.sounds.SoundListScreen
import corsica.universita.carnet.ui.profile.ProfileScreen
import corsica.universita.carnet.ui.profile.ProfileViewModel
import corsica.universita.carnet.ui.popular.PopularScreen
import corsica.universita.carnet.ui.trails.TrailListScreen
import corsica.universita.carnet.ui.trails.TrailDetailScreen
import corsica.universita.carnet.ui.trails.TrailCreateScreen
import corsica.universita.carnet.ui.theme.MicrophoneRed
import corsica.universita.carnet.ui.UiFeedback
import kotlinx.serialization.Serializable

@Serializable data object SoundList : NavKey
@Serializable data class SoundDetail(val id: Long) : NavKey
@Serializable data object Record : NavKey
@Serializable data object Popular : NavKey
@Serializable data object TrailList : NavKey
@Serializable data class TrailDetail(val id: String) : NavKey
@Serializable data object TrailCreate : NavKey
@Serializable data object Profile : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundscapeNavigation(
    notificationSoundId: Long? = null,
    onNotificationHandled: () -> Unit = {},
    openMainScreen: Boolean = false,
    onAppLinkHandled: () -> Unit = {},
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(SoundList)
    val profileViewModel: ProfileViewModel = viewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var snackbarError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        UiFeedback.messages.collect { message ->
            snackbarError = message.error
            snackbarHost.showSnackbar(message.text)
        }
    }
    val current = backStack.lastOrNull()
    val mainTab = current == SoundList || current == Popular || current == TrailList || current == Profile
    val goBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
    val openTab: (NavKey) -> Unit = { tab ->
        if (backStack.lastOrNull() != tab) {
            backStack.clear()
            backStack.add(tab)
        }
    }

    LaunchedEffect(openMainScreen, notificationSoundId) {
        if (openMainScreen) {
            backStack.clear()
            backStack.add(SoundList)
            onAppLinkHandled()
            if (notificationSoundId != null) onNotificationHandled()
        } else if (notificationSoundId != null && notificationSoundId > 0) {
            backStack.add(SoundDetail(notificationSoundId))
            onNotificationHandled()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(data, containerColor = if (snackbarError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = if (snackbarError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary)
            }
        },
        topBar = {
            if (mainTab) TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("◉", color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Corsica Soundscape", style = MaterialTheme.typography.titleMedium)
                            Text("La Corse, à écouter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkTheme) "Activer le thème clair" else "Activer le thème sombre")
                    }
                },
            )
        },
        bottomBar = {
            if (mainTab) NavigationBar {
                NavigationBarItem(current == SoundList, { openTab(SoundList) },
                    icon = { Icon(Icons.Default.Map, null) }, label = { Text("Carte") })
                NavigationBarItem(current == Popular, { openTab(Popular) },
                    icon = { Icon(Icons.Default.TrendingUp, null) }, label = { Text("Populaires") })
                NavigationBarItem(current == TrailList, { openTab(TrailList) },
                    icon = { Icon(Icons.Default.Route, null) }, label = { Text("Parcours") })
                NavigationBarItem(current == Profile, { openTab(Profile) },
                    icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profil") })
            }
        },
        floatingActionButton = {
            if (mainTab) FloatingActionButton(
                onClick = { backStack.add(Record) },
                shape = CircleShape,
                containerColor = MicrophoneRed,
                contentColor = Color.White,
            ) { Icon(Icons.Default.Mic, contentDescription = "Enregistrer un son", modifier = Modifier.size(24.dp)) }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize().padding(padding),
            onBack = goBack,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<SoundList> {
                    SoundListScreen(onNavigateToDetail = { id -> backStack.add(SoundDetail(id)) })
                }
                entry<SoundDetail> { route -> SoundDetailScreen(route.id, onBack = goBack) }
                entry<Record> { RecordScreen(onNavigateBack = goBack) }
                entry<Popular> { PopularScreen(onNavigateToDetail = { id -> backStack.add(SoundDetail(id)) }) }
                entry<TrailList> { TrailListScreen(onOpenTrail = { id -> backStack.add(TrailDetail(id)) }, onCreate = { backStack.add(TrailCreate) }) }
                entry<TrailDetail> { route -> TrailDetailScreen(route.id, onBack = goBack) }
                entry<TrailCreate> { TrailCreateScreen(onBack = goBack, onCreated = { id ->
                    backStack.removeLastOrNull()
                    backStack.add(TrailDetail(id))
                }) }
                entry<Profile> {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Mon profil", style = MaterialTheme.typography.headlineMedium)
                        Text(profile?.nickname ?: "Explorateur", style = MaterialTheme.typography.titleLarge)
                        Text("Membre de la carte sonore", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
    }

    if (profileViewModel.profileReady && profile == null && !profileViewModel.hasStoredProfile) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Bienvenue en Corse") },
            text = { ProfileScreen(profileViewModel) },
            confirmButton = {},
        )
    }
}
