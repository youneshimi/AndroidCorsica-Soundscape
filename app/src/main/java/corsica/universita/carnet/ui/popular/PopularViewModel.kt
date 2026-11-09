package corsica.universita.carnet.ui.popular

import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.SoundEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PopularViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val sort = savedState.getStateFlow("sort", "likes")
    private val rankedIds = MutableStateFlow<List<String>?>(null)
    val likes = SoundscapeContainer.socialRepository.likes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sounds: StateFlow<List<SoundEntity>> = combine(SoundscapeContainer.repository.sounds, sort, rankedIds) { all, order, ids ->
        if (ids != null) ids.mapNotNull { id -> all.firstOrNull { it.serverId == id } }
        else all.filter { it.serverId != null }.sortedByDescending { if (order == "likes") it.likesCount else it.listensCount }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var error by mutableStateOf<String?>(null)
        private set

    init { refresh() }
    fun setSort(value: String) {
        if (value !in setOf("likes", "listens")) return
        savedState["sort"] = value
        rankedIds.value = null
        refresh()
    }
    fun refresh() = viewModelScope.launch {
        try { rankedIds.value = SoundscapeContainer.popularRepository.refresh(sort.value); error = null }
        catch (failure: CancellationException) { throw failure }
        catch (failure: Exception) { error = failure.message }
    }
    fun toggleLike(sound: SoundEntity) = viewModelScope.launch {
        val id = sound.serverId ?: return@launch
        try { SoundscapeContainer.socialRepository.toggleLike(id); error = null }
        catch (failure: CancellationException) { throw failure }
        catch (failure: Exception) { error = failure.message }
    }
}
