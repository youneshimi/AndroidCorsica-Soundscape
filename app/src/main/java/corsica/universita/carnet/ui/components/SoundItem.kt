package corsica.universita.carnet.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import corsica.universita.carnet.data.local.SoundEntity
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.remote.ApiClient
import corsica.universita.carnet.ui.theme.categoryColor
import corsica.universita.carnet.ui.theme.categoryEmoji
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SoundItem(sound: SoundEntity, onClick: () -> Unit, liked: Boolean = false, onLike: () -> Unit = {}, onDelete: (() -> Unit)? = null) {
    val deleting = sound.syncState == "DELETING"
    val context = LocalContext.current
    val player = remember(context, sound.id) { ExoPlayer.Builder(context).build() }
    val scope = rememberCoroutineScope()
    var playing by remember(sound.id) { mutableStateOf(false) }
    val source = remember(sound.audioPath, sound.audioUrl) {
        sound.audioPath?.takeIf { File(it).isFile }?.let { Uri.fromFile(File(it)) }
            ?: sound.audioUrl?.takeIf { it.startsWith("/uploads/") }?.let {
                Uri.parse(ApiClient.BASE_URL.trimEnd('/') + it)
            }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (isPlaying) sound.serverId?.let { serverId ->
                    scope.launch { runCatching { SoundscapeContainer.socialRepository.recordListen(serverId) } }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(deleting) { if (deleting) player.pause() }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "cardScale")
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val deleteInteraction = remember { MutableInteractionSource() }
    val deletePressed by deleteInteraction.collectIsPressedAsState()
    val deleteColor by animateColorAsState(
        if (deletePressed) MaterialTheme.colorScheme.error else muted,
        label = "deleteColor",
    )
    val date = remember(sound.createdAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRANCE).format(Date(sound.createdAt))
    }

    Card(
        onClick = onClick,
        enabled = !deleting,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (deleting) 0.5f else 1f
        },
        colors = CardDefaults.cardColors(
            containerColor = if (deleting) Color(0x15FF0000) else MaterialTheme.colorScheme.surface,
            disabledContainerColor = Color(0x15FF0000),
        ),
        border = BorderStroke(1.dp, if (deleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 8.dp else 2.dp),
    ) {
        Row {
            Spacer(Modifier.width(4.dp).height(180.dp).background(categoryColor(sound.category)))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(categoryEmoji(sound.category), style = MaterialTheme.typography.headlineSmall)
                    Text(sound.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        textDecoration = if (deleting) TextDecoration.LineThrough else null)
                    if (deleting) Text("En attente...", style = MaterialTheme.typography.labelSmall, color = muted)
                    else if (onDelete != null) IconButton(onClick = onDelete, interactionSource = deleteInteraction) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer ${sound.title}",
                            modifier = Modifier.size(24.dp), tint = deleteColor)
                    }
                }
                if (sound.description.isNotBlank()) Text(sound.description, style = MaterialTheme.typography.bodySmall, color = muted)
                Text("Par ${sound.nickname ?: "Anonyme"} · $date", style = MaterialTheme.typography.labelSmall, color = muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (player.isPlaying) player.pause()
                        else if (source != null) {
                            if (player.currentMediaItem?.localConfiguration?.uri != source) {
                                player.setMediaItem(MediaItem.fromUri(source))
                                player.prepare()
                            }
                            player.play()
                        }
                    }, enabled = source != null && !deleting) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Écouter", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onLike, enabled = sound.serverId != null && !deleting) {
                        Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (liked) "Retirer le like" else "Aimer", tint = MaterialTheme.colorScheme.error)
                    }
                    Text("${sound.likesCount}", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.Headphones, contentDescription = "Écoutes", modifier = Modifier.padding(start = 12.dp), tint = muted)
                    Text(" ${sound.listensCount}", style = MaterialTheme.typography.labelSmall, color = muted)
                }
                TextButton(onClick = onClick, enabled = !deleting, modifier = Modifier.align(Alignment.End)) {
                    Text("Voir le détail →")
                }
                if (deleting) Text("🗑️ Suppression en attente de synchronisation",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
