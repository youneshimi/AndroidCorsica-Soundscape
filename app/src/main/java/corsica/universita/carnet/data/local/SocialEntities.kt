package corsica.universita.carnet.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "likes")
data class LikeEntity(@PrimaryKey val soundId: String, val liked: Boolean)

@Entity(tableName = "comments", indices = [Index(value = ["soundId"])])
data class CommentEntity(
    @PrimaryKey val id: String,
    val soundId: String,
    val profileId: String,
    val nickname: String,
    val text: String,
    val createdAt: String,
)

@Entity(tableName = "trails")
data class TrailEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val profileId: String,
    val nickname: String,
    val createdAt: String,
    val soundsCount: Int = 0,
)

@Entity(tableName = "trail_sounds", primaryKeys = ["trailId", "soundId"], indices = [Index(value = ["trailId"])])
data class TrailSoundEntity(
    val trailId: String,
    val soundId: String,
    val position: Int,
    val note: String,
)
