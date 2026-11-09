package corsica.universita.carnet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun observeProfile(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): ProfileEntity?

    @Upsert suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM likes")
    fun likes(): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes WHERE soundId = :soundId LIMIT 1")
    suspend fun getLike(soundId: String): LikeEntity?

    @Upsert suspend fun upsertLike(like: LikeEntity)

    @Query("SELECT * FROM comments WHERE soundId = :soundId ORDER BY createdAt ASC")
    fun comments(soundId: String): Flow<List<CommentEntity>>

    @Query("DELETE FROM comments WHERE soundId = :soundId")
    suspend fun clearComments(soundId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM trails ORDER BY createdAt DESC")
    fun trails(): Flow<List<TrailEntity>>

    @Query("SELECT * FROM trails WHERE id = :id LIMIT 1")
    fun trail(id: String): Flow<TrailEntity?>

    @Query("SELECT * FROM trail_sounds WHERE trailId = :id ORDER BY position ASC")
    fun trailSounds(id: String): Flow<List<TrailSoundEntity>>

    @Query("DELETE FROM trail_sounds WHERE trailId = :id")
    suspend fun clearTrailSounds(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailSounds(steps: List<TrailSoundEntity>)

    @Upsert suspend fun upsertTrail(trail: TrailEntity)

    @Upsert suspend fun upsertTrails(trails: List<TrailEntity>)
}
