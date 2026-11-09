package corsica.universita.carnet.ui.trails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.SoundEntity
import corsica.universita.carnet.data.remote.CreateTrailSound
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrailCreateViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val title = savedState.getStateFlow("trailTitle", "")
    val description = savedState.getStateFlow("trailDescription", "")
    val selectedIds = savedState.getStateFlow("trailSelected", arrayListOf<String>())
    val sounds: StateFlow<List<SoundEntity>> = SoundscapeContainer.repository.sounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = savedState.getStateFlow("trailNotes", hashMapOf<String, String>())
    var error by mutableStateOf<String?>(null)
        private set
    var creating by mutableStateOf(false)
        private set
    var createdId by mutableStateOf<String?>(null)
        private set
    fun setTitle(value: String) { savedState["trailTitle"] = value.take(80) }
    fun setDescription(value: String) { savedState["trailDescription"] = value.take(500) }
    fun toggle(id: String) {
        val next = ArrayList(selectedIds.value)
        if (id in next) next.remove(id) else if (next.size < 30) next.add(id)
        savedState["trailSelected"] = next
    }
    fun setNote(id: String, value: String) {
        savedState["trailNotes"] = HashMap(notes.value).apply { put(id, value.take(300)) }
    }
    fun create() {
        if (creating) return
        creating = true
        viewModelScope.launch {
            try {
                createdId = SoundscapeContainer.trailRepository.create(
                    title.value, description.value,
                    selectedIds.value.map { CreateTrailSound(it, notes.value[it].orEmpty()) },
                )
                error = null
            } catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) { error = failure.message }
            finally { creating = false }
        }
    }
}
