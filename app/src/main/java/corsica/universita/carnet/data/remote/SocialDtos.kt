package corsica.universita.carnet.data.remote

import corsica.universita.carnet.data.local.CommentEntity
import corsica.universita.carnet.data.local.TrailEntity

data class ProfileRequest(val id: String, val nickname: String)
data class ProfileResponse(val id: String, val nickname: String)
data class ProfileAction(val profileId: String)
data class CommentRequest(val profileId: String, val text: String)
data class CommentDto(val id: String, val profileId: String, val nickname: String, val text: String, val createdAt: String) {
    fun toEntity(soundId: String) = CommentEntity(id, soundId, profileId, nickname, text, createdAt)
}

data class TrailDto(
    val id: String,
    val title: String,
    val description: String,
    val profileId: String,
    val nickname: String,
    val createdAt: String,
    val soundsCount: Int = 0,
) {
    fun toEntity() = TrailEntity(id, title, description, profileId, nickname, createdAt, soundsCount)
}

data class TrailStepDto(val position: Int, val note: String, val sound: SoundDto)
data class TrailDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val profileId: String,
    val nickname: String,
    val createdAt: String,
    val steps: List<TrailStepDto>,
) {
    fun toEntity() = TrailEntity(id, title, description, profileId, nickname, createdAt, steps.size)
}

data class CreateTrailSound(val soundId: String, val note: String = "")
data class CreateTrailRequest(val profileId: String, val title: String, val description: String, val sounds: List<CreateTrailSound>)
data class CreatedTrailDto(val id: String)
