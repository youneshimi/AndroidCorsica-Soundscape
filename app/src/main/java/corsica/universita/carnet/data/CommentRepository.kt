package corsica.universita.carnet.data

import androidx.room.withTransaction
import corsica.universita.carnet.data.local.CommentEntity
import corsica.universita.carnet.data.local.SocialDao
import corsica.universita.carnet.data.local.SoundscapeDatabase
import corsica.universita.carnet.data.remote.CommentRequest
import corsica.universita.carnet.data.remote.SyncApi
import kotlinx.coroutines.flow.Flow

class CommentRepository(
    private val database: SoundscapeDatabase,
    private val dao: SocialDao,
    private val api: SyncApi,
    private val profileRepository: ProfileRepository,
) {
    fun comments(soundId: String): Flow<List<CommentEntity>> = dao.comments(soundId)

    suspend fun refresh(soundId: String) {
        val remote = api.getComments(soundId).map { it.toEntity(soundId) }
        database.withTransaction {
            dao.clearComments(soundId)
            dao.insertComments(remote)
        }
    }

    suspend fun add(soundId: String, value: String) {
        val profile = requireNotNull(profileRepository.current()) { "Choisissez un profil" }
        val text = value.trim()
        require(text.isNotEmpty() && text.length <= 500) { "Commentaire de 1 à 500 caractères requis" }
        val remote = api.postComment(soundId, CommentRequest(profile.id, text))
        dao.insertComment(remote.toEntity(soundId))
    }
}
