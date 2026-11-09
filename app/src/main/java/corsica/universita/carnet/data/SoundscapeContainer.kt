package corsica.universita.carnet.data

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import corsica.universita.carnet.data.local.SoundscapeDatabase
import corsica.universita.carnet.data.local.MIGRATION_1_2
import corsica.universita.carnet.data.remote.ApiClient
import retrofit2.Retrofit

object SoundscapeContainer {
    lateinit var database: SoundscapeDatabase
        private set
    lateinit var retrofit: Retrofit
        private set
    lateinit var workManager: WorkManager
        private set
    lateinit var repository: SoundRepository
        private set
    lateinit var profileRepository: ProfileRepository
        private set
    lateinit var socialRepository: SocialRepository
        private set
    lateinit var commentRepository: CommentRepository
        private set
    lateinit var popularRepository: PopularRepository
        private set
    lateinit var trailRepository: TrailRepository
        private set

    @Synchronized
    fun init(context: Context) {
        if (::repository.isInitialized) return
        val appContext = context.applicationContext
        database = Room.databaseBuilder(appContext, SoundscapeDatabase::class.java, "soundscape.db")
            .addMigrations(MIGRATION_1_2).build()
        retrofit = ApiClient.retrofit
        workManager = WorkManager.getInstance(appContext)
        profileRepository = ProfileRepository(appContext, database.socialDao(), ApiClient.syncApi)
        repository = SoundRepository(database, database.soundDao(), ApiClient.syncApi, workManager, profileRepository)
        socialRepository = SocialRepository(database.socialDao(), database.soundDao(), ApiClient.syncApi, profileRepository)
        commentRepository = CommentRepository(database, database.socialDao(), ApiClient.syncApi, profileRepository)
        popularRepository = PopularRepository(repository, ApiClient.syncApi)
        trailRepository = TrailRepository(database, database.socialDao(), repository, ApiClient.syncApi, profileRepository)
    }
}
