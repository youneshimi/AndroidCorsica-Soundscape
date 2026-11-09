package corsica.universita.carnet.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corsica.universita.carnet.data.SoundscapeContainer
import corsica.universita.carnet.data.local.ProfileEntity
import corsica.universita.carnet.ui.UiFeedback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    var profileReady by mutableStateOf(false)
        private set
    var hasStoredProfile by mutableStateOf(false)
        private set
    init {
        viewModelScope.launch {
            SoundscapeContainer.profileRepository.ensureId()
            hasStoredProfile = SoundscapeContainer.profileRepository.current() != null
            profileReady = true
        }
    }
    val profile: StateFlow<ProfileEntity?> = SoundscapeContainer.profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val nickname = savedState.getStateFlow("nickname", "")
    var error by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    fun setNickname(value: String) { savedState["nickname"] = value.take(30) }

    fun save() {
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                SoundscapeContainer.profileRepository.chooseNickname(nickname.value)
                hasStoredProfile = true
                error = null
                UiFeedback.show("Bienvenue en Corse !")
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                error = failure.message ?: "Profil indisponible"
                UiFeedback.show(error ?: "Profil indisponible", error = true)
            } finally {
                saving = false
            }
        }
    }
}
