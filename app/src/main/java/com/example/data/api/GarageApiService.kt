package com.example.data.api

import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import retrofit2.http.*

interface GarageApiService {

    @GET("user/profile")
    suspend fun getUserProfile(): UserProfile

    @PUT("user/profile")
    suspend fun updateUserProfile(@Body profile: UserProfile): UserProfile

    @GET("vehicles")
    suspend fun getVehicles(): List<Vehicle>

    @POST("vehicles")
    suspend fun createVehicle(@Body vehicle: Vehicle): Vehicle

    @PUT("vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: Int, @Body vehicle: Vehicle): Vehicle

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int)

    @GET("services")
    suspend fun getAllServices(): List<ServiceLog>

    @GET("vehicles/{vehicleId}/services")
    suspend fun getServicesForVehicle(@Path("vehicleId") vehicleId: Int): List<ServiceLog>

    @POST("services")
    suspend fun createServiceLog(@Body serviceLog: ServiceLog): ServiceLog

    @DELETE("services/{id}")
    suspend fun deleteServiceLog(@Path("id") id: Int)
}
