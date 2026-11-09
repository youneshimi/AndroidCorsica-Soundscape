package corsica.universita.carnet.data

import androidx.room.withTransaction
import corsica.universita.carnet.data.local.SocialDao
import corsica.universita.carnet.data.local.SoundscapeDatabase
import corsica.universita.carnet.data.local.TrailSoundEntity
import corsica.universita.carnet.data.remote.CreateTrailRequest
import corsica.universita.carnet.data.remote.CreateTrailSound
import corsica.universita.carnet.data.remote.SyncApi

class TrailRepository(
    private val database: SoundscapeDatabase,
    private val dao: SocialDao,
    private val sounds: SoundRepository,
    private val api: SyncApi,
    private val profileRepository: ProfileRepository,
) {
    val trails = dao.trails()
    fun trail(id: String) = dao.trail(id)
    fun steps(id: String) = dao.trailSounds(id)

    suspend fun refreshList() {
        dao.upsertTrails(api.getTrails().map { it.toEntity() })
    }

    suspend fun refreshDetail(id: String) {
        val remote = api.getTrail(id)
        sounds.cacheRemote(remote.steps.map { it.sound })
        database.withTransaction {
            dao.upsertTrail(remote.toEntity())
            dao.clearTrailSounds(id)
            dao.insertTrailSounds(remote.steps.map { TrailSoundEntity(id, it.sound.id, it.position, it.note) })
        }
    }

    suspend fun create(title: String, description: String, steps: List<CreateTrailSound>): String {
        val profile = requireNotNull(profileRepository.current()) { "Choisissez un profil" }
        require(title.trim().isNotEmpty() && title.trim().length <= 80)
        require(description.length <= 500)
        require(steps.size in 1..30 && steps.distinctBy { it.soundId }.size == steps.size)
        require(steps.all { it.note.length <= 300 })
        profileRepository.sync(profile)
        val id = api.createTrail(CreateTrailRequest(profile.id, title.trim(), description, steps)).id
        refreshDetail(id)
        refreshList()
        return id
    }
}
