package corsica.universita.carnet.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import corsica.universita.carnet.data.local.ProfileEntity
import corsica.universita.carnet.data.local.SocialDao
import corsica.universita.carnet.data.preferences.SettingsKeys
import corsica.universita.carnet.data.preferences.settingsDataStore
import corsica.universita.carnet.data.remote.ProfileRequest
import corsica.universita.carnet.data.remote.SyncApi
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ProfileRepository(private val context: Context, private val dao: SocialDao, private val api: SyncApi) {
    val profile: Flow<ProfileEntity?> = context.settingsDataStore.data
        .map { it[SettingsKeys.PROFILE_ID] }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else dao.observeProfile(id) }

    suspend fun ensureId(): String {
        val existing = context.settingsDataStore.data.first()[SettingsKeys.PROFILE_ID]
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        context.settingsDataStore.edit { preferences ->
            if (preferences[SettingsKeys.PROFILE_ID] == null) preferences[SettingsKeys.PROFILE_ID] = generated
        }
        return context.settingsDataStore.data.first()[SettingsKeys.PROFILE_ID] ?: generated
    }

    suspend fun current(): ProfileEntity? {
        val id = context.settingsDataStore.data.first()[SettingsKeys.PROFILE_ID] ?: return null
        return dao.getProfile(id)
    }

    suspend fun chooseNickname(value: String): ProfileEntity {
        val nickname = value.trim()
        require(nickname.isNotEmpty() && nickname.length <= 30) { "Pseudo de 1 à 30 caractères requis" }
        val entity = ProfileEntity(id = ensureId(), nickname = nickname)
        dao.upsertProfile(entity)
        context.settingsDataStore.edit { it[SettingsKeys.PROFILE_NICKNAME] = nickname }
        try { sync(entity) } catch (error: CancellationException) { throw error } catch (_: Exception) { /* Réessai avec la prochaine synchronisation. */ }
        return entity
    }

    suspend fun sync(profile: ProfileEntity? = null) {
        val currentProfile = profile ?: current()
        if (currentProfile != null) api.createProfile(ProfileRequest(currentProfile.id, currentProfile.nickname))
    }
}
