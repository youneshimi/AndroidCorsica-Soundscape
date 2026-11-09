package corsica.universita.carnet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SoundEntity::class, OutboxEntity::class, ProfileEntity::class, LikeEntity::class, CommentEntity::class, TrailEntity::class, TrailSoundEntity::class], version = 2, exportSchema = true)
abstract class SoundscapeDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
    abstract fun socialDao(): SocialDao
}
