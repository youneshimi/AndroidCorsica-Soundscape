package corsica.universita.carnet.ui.popular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import corsica.universita.carnet.ui.components.SoundItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularScreen(onNavigateToDetail: (Long) -> Unit, viewModel: PopularViewModel = viewModel()) {
    val sounds by viewModel.sounds.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val likes by viewModel.likes.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                Button(onClick = { viewModel.setSort("likes") }, enabled = sort != "likes") { Text("Likes") }
                Button(onClick = { viewModel.setSort("listens") }, enabled = sort != "listens") { Text("Écoutes") }
            }
            viewModel.error?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sounds.isEmpty()) item { Text("Aucun son populaire pour le moment.") }
                items(sounds, key = { it.id }) { sound ->
                    SoundItem(sound, onClick = { onNavigateToDetail(sound.id) }, liked = likes.any { it.soundId == sound.serverId && it.liked }, onLike = { viewModel.toggleLike(sound) })
                }
            }
        }
    }
}
