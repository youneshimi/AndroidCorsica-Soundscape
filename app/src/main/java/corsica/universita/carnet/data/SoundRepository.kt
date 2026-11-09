package corsica.universita.carnet.data

import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import corsica.universita.carnet.data.local.OutboxEntity
import corsica.universita.carnet.data.local.SoundDao
import corsica.universita.carnet.data.local.SoundEntity
import corsica.universita.carnet.data.local.SoundscapeDatabase
import corsica.universita.carnet.data.remote.SyncApi
import corsica.universita.carnet.work.SyncWorker
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class SoundRepository(
    private val database: SoundscapeDatabase,
    private val dao: SoundDao,
    private val api: SyncApi,
    private val workManager: WorkManager,
    private val profileRepository: ProfileRepository,
) {
    private val syncMutex = Mutex()
    // Empêche une réponse de liste déjà en vol de réinsérer un son supprimé.
    private val deletedServerIds = ConcurrentHashMap.newKeySet<String>()
    val sounds: Flow<List<SoundEntity>> = dao.all()

    suspend fun getSound(id: Long): SoundEntity? = dao.get(id)

    fun soundsByCategory(cat: String): Flow<List<SoundEntity>> = dao.byCategory(cat)

    /** Returns false when the network is unavailable; Room remains the source of truth. */
    suspend fun refreshFromServer(): Boolean {
        val remote = try {
            api.getSounds()
        } catch (error: IOException) {
            Log.d(TAG, "Network unavailable; retaining Room cache", error)
            return false
        }

        cacheRemote(remote)
        return true
    }

    suspend fun cacheRemote(remote: List<corsica.universita.carnet.data.remote.SoundDto>) {
        database.withTransaction {
            for (dto in remote) {
                if (dto.id in deletedServerIds) continue
                val incoming = dto.toEntity()
                val existing = dao.getByServerId(dto.id) ?: dao.getByClientId(incoming.clientId)
                if (existing == null) {
                    dao.upsert(incoming)
                } else if (existing.syncState != "DELETING") {
                    // Preserve a locally captured file while replacing server metadata.
                    dao.upsert(incoming.copy(id = existing.id, audioPath = existing.audioPath))
                    dao.deleteOutbox(existing.clientId)
                }
            }
        }
    }

    suspend fun addSound(
        title: String,
        description: String,
        lat: Double,
        lng: Double,
        category: String,
        audioPath: String,
    ): Long {
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 80) { "Invalid title" }
        require(description.length <= 300) { "Description too long" }
        require(lat.isFinite() && lat in -90.0..90.0 && lng.isFinite() && lng in -180.0..180.0) {
            "Invalid coordinates"
        }
        require(category in CATEGORIES) { "Invalid category" }
        val file = File(audioPath)
        require(file.isFile && file.length() in 1..MAX_AUDIO_BYTES) { "Audio file missing or too large" }
        require(mimeType(file) != null) { "Unsupported audio format" }

        val clientId = "local-${UUID.randomUUID()}"
        val profile = profileRepository.current()
        val sound = SoundEntity(
            clientId = clientId,
            title = trimmedTitle,
            description = description,
            latitude = lat,
            longitude = lng,
            category = category,
            audioPath = file.absolutePath,
            syncState = "PENDING",
            profileId = profile?.id,
            nickname = profile?.nickname,
        )
        val id = dao.insertSoundAndOutbox(sound, OutboxEntity(clientId))
        enqueueSync()
        return id
    }

    suspend fun syncPending() = withContext(Dispatchers.IO) { syncMutex.withLock {
        for (entry in dao.getOutbox()) {
            val sound = dao.getByClientId(entry.clientId)
            if (sound == null) {
                dao.deleteOutbox(entry.clientId)
                continue
            }
            if (sound.syncState == "DELETING") continue
            val file = sound.audioPath?.let(::File)
            val mime = file?.let(::mimeType)
            if (file == null || !file.isFile || file.length() !in 1..MAX_AUDIO_BYTES || mime == null) {
                dao.failPermanently(sound.id, entry.clientId, "Audio file missing or unsupported")
                continue
            }

            val text = "text/plain; charset=utf-8".toMediaType()
            val audio = MultipartBody.Part.createFormData(
                "audio",
                "recording.${file.extension.lowercase()}",
                file.asRequestBody(mime.toMediaType()),
            )
            val result = try {
                if (sound.profileId != null) profileRepository.sync()
                api.pushSound(
                    idempotencyKey = entry.clientId,
                    title = sound.title.toRequestBody(text),
                    description = sound.description.toRequestBody(text),
                    latitude = sound.latitude.toString().toRequestBody(text),
                    longitude = sound.longitude.toString().toRequestBody(text),
                    category = sound.category.toRequestBody(text),
                    profileId = sound.profileId?.toRequestBody(text),
                    audio = audio,
                )
            } catch (error: HttpException) {
                if (error.code() in PERMANENT_HTTP_ERRORS) {
                    dao.failPermanently(sound.id, entry.clientId, "HTTP ${error.code()}")
                    continue
                }
                throw error
            }
            // Retrofit returns the DTO for both HTTP 201 and idempotent HTTP 200.
            dao.finishSync(sound.id, entry.clientId, result.id, result.audioUrl)
        }
    } }

    /** La carte reste visible en attente ; WorkManager effectue la suppression. */
    suspend fun deleteSound(sound: SoundEntity) {
        dao.markForDeletion(sound.id)
        enqueueSync()
    }

    suspend fun getPendingDeletes(): List<SoundEntity> = dao.pendingDeletes()

    suspend fun executeServerDelete(sound: SoundEntity) = withContext(Dispatchers.IO) { syncMutex.withLock {
        val current = dao.get(sound.id) ?: return@withLock
        if (current.syncState != "DELETING") return@withLock
        if (!deleteMarked(current)) throw IOException("Sound deletion will retry")
    } }

    /** Le verrou empêche de supprimer un fichier pendant son upload. */
    private suspend fun deleteMarked(sound: SoundEntity): Boolean {
        val serverId = sound.serverId
        if (serverId != null) {
            val response = try { api.deleteSound(serverId) }
            catch (error: IOException) {
                Log.d(TAG, "Deletion queued until network returns", error)
                return false
            }
            // Le serveur a déjà supprimé le son si la réponse est 404.
            val deleted = response.isSuccessful || response.code() == 404
            if (!deleted) return false
            deletedServerIds.add(serverId)
        }
        database.withTransaction {
            dao.deleteOutboxByClientId(sound.clientId)
            dao.deleteById(sound.id)
        }
        sound.audioPath?.let { File(it).delete() }
        return true
    }

    fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun mimeType(file: File): String? = when (file.extension.lowercase()) {
        "m4a", "mp4" -> "audio/mp4"
        "aac" -> "audio/aac"
        "webm" -> "audio/webm"
        "ogg" -> "audio/ogg"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        else -> null
    }

    private companion object {
        const val TAG = "SoundRepository"
        const val MAX_AUDIO_BYTES = 10L * 1024 * 1024
        val CATEGORIES = setOf("mer", "nature", "village", "animal", "musique", "marche", "autre")
        val PERMANENT_HTTP_ERRORS = setOf(400, 413, 415)
    }
}
