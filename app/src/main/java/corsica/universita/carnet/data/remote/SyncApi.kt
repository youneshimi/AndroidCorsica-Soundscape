package corsica.universita.carnet.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.Response

interface SyncApi {
    @DELETE("api/sounds/{id}")
    suspend fun deleteSound(@Path("id") id: String): Response<Unit>

    @POST("api/profiles")
    suspend fun createProfile(@Body profile: ProfileRequest): ProfileResponse

    @POST("api/sounds/{id}/like")
    suspend fun likeSound(@Path("id") id: String, @Body action: ProfileAction)

    @HTTP(method = "DELETE", path = "api/sounds/{id}/like", hasBody = true)
    suspend fun unlikeSound(@Path("id") id: String, @Body action: ProfileAction)

    @POST("api/sounds/{id}/listen")
    suspend fun listenSound(@Path("id") id: String, @Body action: ProfileAction)

    @GET("api/sounds/{id}/comments")
    suspend fun getComments(@Path("id") id: String): List<CommentDto>

    @POST("api/sounds/{id}/comments")
    suspend fun postComment(@Path("id") id: String, @Body comment: CommentRequest): CommentDto

    @GET("api/sounds/popular")
    suspend fun getPopular(@Query("sort") sort: String, @Query("limit") limit: Int = 10): List<SoundDto>

    @GET("api/trails")
    suspend fun getTrails(): List<TrailDto>

    @GET("api/trails/{id}")
    suspend fun getTrail(@Path("id") id: String): TrailDetailDto

    @POST("api/trails")
    suspend fun createTrail(@Body trail: CreateTrailRequest): CreatedTrailDto

    @GET("api/sounds")
    suspend fun getSounds(): List<SoundDto>

    @Multipart
    @POST("api/sounds")
    suspend fun pushSound(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("category") category: RequestBody,
        @Part("profileId") profileId: RequestBody?,
        @Part audio: MultipartBody.Part,
    ): SoundDto
}
