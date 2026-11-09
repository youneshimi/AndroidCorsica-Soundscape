package corsica.universita.carnet.data

import corsica.universita.carnet.data.remote.SyncApi

class PopularRepository(private val sounds: SoundRepository, private val api: SyncApi) {
    suspend fun refresh(sort: String): List<String> {
        require(sort == "likes" || sort == "listens")
        val remote = api.getPopular(sort, 10)
        sounds.cacheRemote(remote)
        return remote.map { it.id }
    }
}
