package corsica.universita.carnet.ui.sounds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.carnet.ui.components.CategoryFilter
import corsica.universita.carnet.ui.components.SoundItem
import corsica.universita.carnet.ui.components.SoundMap
import corsica.universita.carnet.data.local.SoundEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundListScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: SoundListViewModel = viewModel(),
) {
    val sounds by viewModel.filteredSounds.collectAsStateWithLifecycle()
    val likes by viewModel.likes.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val mapSounds = sounds.filter { it.syncState != "DELETING" }
    var pendingDelete by remember { mutableStateOf<SoundEntity?>(null) }
    val list: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            CategoryFilter(selected = selectedCategory, onSelect = viewModel::selectCategory)
            if (viewModel.isOffline || viewModel.isServerUnavailable) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        if (viewModel.isOffline) "Mode hors-ligne — données locales"
                        else "Serveur indisponible — données locales",
                        Modifier.fillMaxWidth().padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (viewModel.isLoading && sounds.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (sounds.isEmpty()) item { Text("Aucun son à afficher. Glissez vers le bas pour réessayer.") }
                        items(sounds, key = { it.id }) { sound ->
                            SoundItem(sound, onClick = { onNavigateToDetail(sound.id) },
                                liked = likes.any { it.soundId == sound.serverId && it.liked },
                                onLike = { viewModel.toggleLike(sound) },
                                onDelete = { pendingDelete = sound })
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            var fullMap by rememberSaveable { mutableStateOf(false) }
            val mapState = rememberSaveableStateHolder()
            val sheetHeight = maxHeight * 0.85f
            val mapContent: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize()) {
                    mapState.SaveableStateProvider("map") {
                        SoundMap(mapSounds, Modifier.fillMaxSize(), onSoundClick = onNavigateToDetail)
                    }
                    FilledTonalButton(
                        onClick = { fullMap = !fullMap },
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    ) { Text(if (fullMap) "Afficher les sons" else "Carte plein écran") }
                }
            }
            if (fullMap) mapContent()
            else BottomSheetScaffold(
                sheetPeekHeight = 220.dp,
                sheetContent = { list(Modifier.fillMaxWidth().height(sheetHeight)) },
            ) { mapContent() }
        } else {
            Column(Modifier.fillMaxSize()) {
                SoundMap(mapSounds, Modifier.fillMaxWidth().weight(1f), onSoundClick = onNavigateToDetail)
                list(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
    pendingDelete?.let { sound ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer ce son ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteSound(sound)
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            },
        )
    }
}
