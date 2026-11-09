package corsica.universita.carnet.ui.trails

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import corsica.universita.carnet.data.remote.ApiClient
import corsica.universita.carnet.ui.components.SoundMap
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailDetailScreen(trailId: String, onBack: () -> Unit) {
    val viewModel: TrailDetailViewModel = viewModel(key = "trail-$trailId", factory = viewModelFactory {
        initializer {
            val handle = createSavedStateHandle()
            if (!handle.contains("trailId")) handle["trailId"] = trailId
            TrailDetailViewModel(handle)
        }
    })
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val playable = detail.steps.filter { soundUri(it.sound) != null }
    val mediaUris = playable.mapNotNull { soundUri(it.sound) }
    val currentPlayable by rememberUpdatedState(playable)
    var playing by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                playing = value
                if (value) currentPlayable.getOrNull(player.currentMediaItemIndex)?.sound?.let(viewModel::recordListen)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
                if (player.isPlaying) currentPlayable.getOrNull(currentIndex)?.sound?.let(viewModel::recordListen)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(mediaUris) {
        val items = mediaUris.map(MediaItem::fromUri)
        player.setMediaItems(items)
        if (items.isNotEmpty()) player.prepare()
    }
    Scaffold(topBar = { TopAppBar(title = { Text(detail.trail?.title ?: "Parcours") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            detail.trail?.let { Text("Par ${it.nickname} · ${it.description}", Modifier.padding(horizontal = 16.dp)) }
            SoundMap(detail.steps.map { it.sound }, Modifier.fillMaxWidth().height(280.dp), route = detail.steps.map { it.sound })
            Button(onClick = { if (playing) player.pause() else player.play() }, enabled = playable.isNotEmpty(), modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(if (playing) "Pause" else "Lire le parcours")
            }
            viewModel.error?.let { Text(it, Modifier.padding(horizontal = 16.dp)) }
            LazyColumn(Modifier.fillMaxSize()) {
                items(detail.steps, key = { it.position }) { step ->
                    val index = playable.indexOfFirst { it.position == step.position }
                    Button(onClick = { player.seekTo(index, 0); player.play() }, enabled = index >= 0, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text("${step.position}. ${step.sound.title}${if (step.note.isBlank()) "" else " — ${step.note}"}${if (currentIndex == index && playing) " ▶" else ""}")
                    }
                }
            }
        }
    }
}

private fun soundUri(sound: corsica.universita.carnet.data.local.SoundEntity): Uri? {
    sound.audioPath?.let { File(it) }?.takeIf { it.isFile }?.let { return Uri.fromFile(it) }
    return sound.audioUrl?.takeIf { it.startsWith("/uploads/") }?.let { Uri.parse(ApiClient.BASE_URL.trimEnd('/') + it) }
}
