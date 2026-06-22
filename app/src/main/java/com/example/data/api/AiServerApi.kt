package com.example.data.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body
import retrofit2.Response

data class AudioProcessResponse(
    val km: Int?,
    val service: String?
)

data class OdometerResponse(
    val odometerValue: Int?
)

data class IllustrationRequest(
    val brand: String,
    val model: String
)

data class IllustrationResponse(
    val url: String?
)

interface AiServerApi {
    @Multipart
    @POST("/process-audio")
    suspend fun processAudio(
        @Part audioFile: MultipartBody.Part
    ): Response<AudioProcessResponse>

    @Multipart
    @POST("/scan-odometer")
    suspend fun scanOdometer(
        @Part image: MultipartBody.Part
    ): Response<OdometerResponse>

    @POST("/generate-illustration")
    suspend fun generateIllustration(
        @Body request: IllustrationRequest
    ): Response<IllustrationResponse>
}
