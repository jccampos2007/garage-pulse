package com.example.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for the GaragePulse REST API.
 * Base URL: https://garage-pulse-api.gscloud.us/api/
 * 
 * All authenticated endpoints require a Bearer token injected
 * automatically via AuthInterceptor in RetrofitClient.
 */
interface GarageApiService {

    // ========== AUTH ==========

    @POST("auth/register")
    suspend fun register(@Body request: AuthRegisterRequest): Response<AuthRegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthLoginRequest): Response<AuthLoginResponse>

    // ========== USER PROFILE ==========

    @GET("user/profile")
    suspend fun getUserProfile(): Response<ApiUserProfile>

    @PUT("user/profile")
    suspend fun updateUserProfile(@Body profile: ApiUserProfileUpdate): Response<ApiUserProfile>

    // ========== VEHICLES ==========

    @GET("vehicles")
    suspend fun getVehicles(): Response<List<ApiVehicle>>

    @POST("vehicles")
    suspend fun createVehicle(@Body vehicle: ApiVehicle): Response<ApiVehicle>

    @PUT("vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: Int, @Body update: ApiVehicleUpdate): Response<ApiVehicle>

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int): Response<Unit>

    // ========== SERVICES ==========

    @GET("services")
    suspend fun getAllServices(
        @Query("category") category: String? = null,
        @Query("type") type: String? = null
    ): Response<List<ApiServiceLog>>

    @GET("vehicles/{vehicleId}/services")
    suspend fun getServicesForVehicle(@Path("vehicleId") vehicleId: Int): Response<List<ApiServiceLog>>

    @POST("services")
    suspend fun createServiceLog(@Body serviceLog: ApiServiceLog): Response<ApiServiceLog>

    @DELETE("services/{id}")
    suspend fun deleteServiceLog(@Path("id") id: Int): Response<Unit>
}
