package corsica.universita.carnet.ui.trails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.TrailEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrailListViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val query = savedState.getStateFlow("trailQuery", "")
    val trails: StateFlow<List<TrailEntity>> = combine(SoundscapeContainer.trailRepository.trails, query) { list, text ->
        list.filter { it.title.contains(text, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var error by mutableStateOf<String?>(null)
        private set
    init { refresh() }
    fun setQuery(value: String) { savedState["trailQuery"] = value }
    fun refresh() = viewModelScope.launch {
        try { SoundscapeContainer.trailRepository.refreshList(); error = null }
        catch (failure: CancellationException) { throw failure }
        catch (failure: Exception) { error = failure.message }
    }
}
