package corsica.universita.carnet.ui.trails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.SoundEntity
import corsica.universita.carnet.data.local.TrailEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrailStep(val position: Int, val note: String, val sound: SoundEntity)
data class TrailDetailState(val trail: TrailEntity? = null, val steps: List<TrailStep> = emptyList())

class TrailDetailViewModel(savedState: SavedStateHandle) : ViewModel() {
    val trailId: String = checkNotNull(savedState["trailId"])
    val detail: StateFlow<TrailDetailState> = combine(
        SoundscapeContainer.trailRepository.trail(trailId),
        SoundscapeContainer.trailRepository.steps(trailId),
        SoundscapeContainer.repository.sounds,
    ) { trail, steps, sounds ->
        TrailDetailState(trail, steps.mapNotNull { step ->
            sounds.firstOrNull { it.serverId == step.soundId }?.let { TrailStep(step.position, step.note, it) }
        }.sortedBy { it.position })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrailDetailState())
    var error by mutableStateOf<String?>(null)
        private set
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        try { SoundscapeContainer.trailRepository.refreshDetail(trailId); error = null }
        catch (failure: CancellationException) { throw failure }
        catch (failure: Exception) { error = failure.message }
    }
    fun recordListen(sound: SoundEntity) {
        val id = sound.serverId ?: return
        viewModelScope.launch {
            try { SoundscapeContainer.socialRepository.recordListen(id) }
            catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { /* Le lecteur conserve son état local. */ }
        }
    }
}
