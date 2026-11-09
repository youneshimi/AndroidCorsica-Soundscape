package corsica.universita.carnet.ui.sounds

import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.CommentEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommentViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val soundId: String = checkNotNull(savedState["serverSoundId"])
    val comments: StateFlow<List<CommentEntity>> = SoundscapeContainer.commentRepository.comments(soundId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val draft = savedState.getStateFlow("commentDraft", "")
    var error by mutableStateOf<String?>(null)
        private set
    var posting by mutableStateOf(false)
        private set

    init { refresh() }
    fun setDraft(value: String) { savedState["commentDraft"] = value.take(500) }
    fun refresh() = viewModelScope.launch {
        try { SoundscapeContainer.commentRepository.refresh(soundId); error = null }
        catch (failure: CancellationException) { throw failure }
        catch (failure: Exception) { error = failure.message }
    }
    fun post() {
        if (posting) return
        posting = true
        viewModelScope.launch {
            try {
                SoundscapeContainer.commentRepository.add(soundId, draft.value)
                savedState["commentDraft"] = ""
                error = null
            } catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) { error = failure.message }
            finally { posting = false }
        }
    }
}
