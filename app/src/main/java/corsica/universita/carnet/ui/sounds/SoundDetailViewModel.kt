package corsica.universita.carnet.ui.sounds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.SoundEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import corsica.universita.carnet.ui.UiFeedback

/**
 * Cycle de vie : l'ID du son est dans SavedStateHandle ; la navigation le restaure aussi.
 * Le contenu vient de Room et est relu après une recréation du processus.
 */
class SoundDetailViewModel(savedState: SavedStateHandle) : ViewModel() {
    var isDeleting by mutableStateOf(false)
        private set
    val soundId: Long = checkNotNull(savedState["soundId"])
    val sound: StateFlow<SoundEntity?> = SoundscapeContainer.repository.sounds
        .map { items -> items.firstOrNull { it.id == soundId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun recordListen() {
        val id = sound.value?.serverId ?: return
        viewModelScope.launch {
            try { SoundscapeContainer.socialRepository.recordListen(id) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { /* Lecture locale toujours possible. */ }
        }
    }

    fun deleteSound(sound: SoundEntity, onDeleted: () -> Unit) {
        if (isDeleting) return
        isDeleting = true
        viewModelScope.launch {
            try {
                SoundscapeContainer.repository.deleteSound(sound)
                UiFeedback.show("Suppression en attente de synchronisation")
                onDeleted()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                UiFeedback.show("Suppression impossible", error = true)
            } finally {
                isDeleting = false
            }
        }
    }
}
