package corsica.universita.carnet.data

import corsica.universita.carnet.data.local.LikeEntity
import corsica.universita.carnet.data.local.SocialDao
import corsica.universita.carnet.data.local.SoundDao
import corsica.universita.carnet.data.remote.ProfileAction
import corsica.universita.carnet.data.remote.SyncApi
import kotlinx.coroutines.flow.Flow

class SocialRepository(
    private val dao: SocialDao,
    private val soundDao: SoundDao,
    private val api: SyncApi,
    private val profileRepository: ProfileRepository,
) {
    val likes: Flow<List<LikeEntity>> = dao.likes()

    suspend fun toggleLike(serverId: String) {
        val profile = profileRepository.current() ?: return
        val liked = dao.getLike(serverId)?.liked == true
        if (liked) api.unlikeSound(serverId, ProfileAction(profile.id)) else api.likeSound(serverId, ProfileAction(profile.id))
        dao.upsertLike(LikeEntity(serverId, !liked))
        soundDao.getByServerId(serverId)?.let { sound ->
            soundDao.setLikesCount(serverId, (sound.likesCount + if (liked) -1 else 1).coerceAtLeast(0))
        }
    }

    suspend fun recordListen(serverId: String) {
        val profile = profileRepository.current() ?: return
        api.listenSound(serverId, ProfileAction(profile.id))
        soundDao.getByServerId(serverId)?.let { sound -> soundDao.setListensCount(serverId, sound.listensCount + 1) }
    }
}
