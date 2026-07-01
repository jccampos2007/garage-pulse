package com.example.data.repository

import android.util.Log
import com.example.data.api.*
import com.example.data.local.DatabaseDao
import com.example.data.model.Vehicle
import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class GarageRepository(private val dao: DatabaseDao) {

    companion object {
        private const val TAG = "GarageRepository"
    }

    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    val activeVehicleFlow: Flow<Vehicle?> = dao.getActiveVehicleFlow()
    val allServiceLogs: Flow<List<ServiceLog>> = dao.getAllServiceLogs()
    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()

    fun getServiceLogsForVehicle(vehicleId: Int): Flow<List<ServiceLog>> {
        return dao.getServiceLogsForVehicle(vehicleId)
    }

    private val api: GarageApiService = RetrofitClient.garageApiService

    // ========== AUTH OPERATIONS ==========

    /**
     * Register a new user via the REST API.
     * On success: saves JWT, creates user profile and vehicle in Room.
     * Returns the API user ID or null on failure.
     */
    suspend fun registerUserApi(
        name: String,
        email: String,
        password: String,
        vehicleBrand: String,
        vehicleModel: String,
        initialOdometer: Double,
        licensePlate: String
    ): AuthRegisterResponse? {
        return try {
            val request = AuthRegisterRequest(
                name = name,
                email = email,
                password = password,
                vehicleBrand = vehicleBrand,
                vehicleModel = vehicleModel,
                initialOdometer = initialOdometer,
                licensePlate = licensePlate
            )
            val response = api.register(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                TokenManager.saveToken(body.token)
                TokenManager.saveUserId(body.user.id)
                Log.d(TAG, "Registration successful, token saved. User ID: ${body.user.id}")
                body
            } else {
                Log.e(TAG, "Registration failed: ${response.code()} - ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error (offline?): ${e.message}")
            null
        }
    }

    /**
     * Login an existing user via the REST API.
     * On success: saves JWT and syncs profile to Room.
     */
    suspend fun loginUserApi(email: String, password: String): AuthLoginResponse? {
        return try {
            val request = AuthLoginRequest(email = email, password = password)
            val response = api.login(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                TokenManager.saveToken(body.token)
                TokenManager.saveUserId(body.user.id)
                // Sync user profile to local Room DB
                dao.insertOrUpdateProfile(body.user.toEntity())
                Log.d(TAG, "Login successful, token saved. User ID: ${body.user.id}")
                body
            } else {
                Log.e(TAG, "Login failed: ${response.code()} - ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error (offline?): ${e.message}")
            null
        }
    }

    // ========== VEHICLE OPERATIONS (Offline-First) ==========

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        val rowId = dao.insertVehicle(vehicle)
        try {
            api.createVehicle(vehicle.copy(id = rowId.toInt()).toApiDto())
            Log.d(TAG, "Vehicle synced to API: ${vehicle.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle sync failed (offline): ${e.message}")
        }
        return rowId
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        dao.updateVehicle(vehicle)
        try {
            api.updateVehicle(
                vehicle.id,
                ApiVehicleUpdate(
                    odometer = vehicle.odometer,
                    customIllustrationUrl = vehicle.customIllustrationUrl,
                    isActive = vehicle.isActive,
                    lastKnownLocation = vehicle.lastKnownLocation,
                    lastUpdatedDate = vehicle.lastUpdatedDate,
                    calculatedKpd = vehicle.calculatedKpd,
                    usageType = vehicle.usageType,
                    status = vehicle.status
                )
            )
            Log.d(TAG, "Vehicle update synced to API: ${vehicle.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle update sync failed (offline): ${e.message}")
        }
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteVehicle(vehicle)
        try {
            api.deleteVehicle(vehicle.id)
            Log.d(TAG, "Vehicle deletion synced to API: ${vehicle.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle delete sync failed (offline): ${e.message}")
        }
    }

    suspend fun selectActiveVehicle(vehicleId: Int) {
        dao.selectActiveVehicle(vehicleId)
    }

    // ========== SERVICE LOG OPERATIONS (Offline-First) ==========

    suspend fun insertServiceLog(serviceLog: ServiceLog): Long {
        val rowId = dao.insertServiceLog(serviceLog)

        // Recalculate KPD (Case A) when a new log is inserted
        recalculateVehicleKpd(serviceLog.vehicleId)

        try {
            api.createServiceLog(serviceLog.copy(id = rowId.toInt()).toApiDto())
            Log.d(TAG, "ServiceLog synced to API: ${serviceLog.title}")
        } catch (e: Exception) {
            Log.w(TAG, "ServiceLog sync failed (offline): ${e.message}")
        }
        return rowId
    }

    suspend fun recalculateVehicleKpd(vehicleId: Int) {
        val vehicle = dao.getVehicleById(vehicleId) ?: return
        val logs = dao.getServiceLogsForVehicle(vehicleId).firstOrNull() ?: emptyList()

        var minDate = vehicle.initialDate
        var minKm = vehicle.initialKm

        val sortedLogs = logs.sortedBy { it.date }
        if (sortedLogs.isNotEmpty()) {
            if (minDate == null || sortedLogs.first().date < minDate) {
                minDate = sortedLogs.first().date
                minKm = sortedLogs.first().mileage
            }

            val maxDate = sortedLogs.last().date
            val maxKm = sortedLogs.last().mileage

            if (minDate != null && minKm != null && maxDate > minDate) {
                val daysDiff = (maxDate - minDate) / (1000 * 60 * 60 * 24).toDouble()
                if (daysDiff >= 1.0) {
                    val kpd = (maxKm - minKm) / daysDiff
                    val updatedVehicle = vehicle.copy(calculatedKpd = maxOf(0.0, kpd))
                    updateVehicle(updatedVehicle)
                }
            }
        }
    }

    suspend fun deleteServiceLog(serviceLog: ServiceLog) {
        dao.deleteServiceLog(serviceLog)
        try {
            api.deleteServiceLog(serviceLog.id)
            Log.d(TAG, "ServiceLog deletion synced to API: ${serviceLog.title}")
        } catch (e: Exception) {
            Log.w(TAG, "ServiceLog delete sync failed (offline): ${e.message}")
        }
    }

    // ========== USER PROFILE OPERATIONS ==========

    suspend fun saveUserProfile(profile: UserProfile) {
        dao.insertOrUpdateProfile(profile)
        try {
            api.updateUserProfile(
                ApiUserProfileUpdate(
                    name = profile.name,
                    useKm = profile.useKm
                )
            )
            Log.d(TAG, "Profile synced to API: ${profile.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Profile sync failed (offline): ${e.message}")
        }
    }

    suspend fun clearAllVehicles() {
        dao.deleteAllVehicles()
    }

    suspend fun clearAllServiceLogs() {
        dao.deleteAllServiceLogs()
    }

    suspend fun prepopulateIfEmpty() {
        // Check if user profile exists
        val currentProfile = userProfile.firstOrNull()
        if (currentProfile == null) {
            // Populate Default User
            val defaultUser = UserProfile(
                id = 1,
                name = "Carlos Rodríguez",
                email = "carlos.rod@garagepulse.app",
                avatarUrl = "",
                useKm = true
            )
            dao.insertOrUpdateProfile(defaultUser)
        }
    }

    /**
     * Synchronize local Room DB with the remote REST API.
     * Downloads remote data and merges into local storage.
     * Requires a valid JWT token in TokenManager.
     */
    suspend fun syncLocalWithRemote(): Boolean {
        if (!TokenManager.isAuthenticated()) {
            Log.w(TAG, "Sync skipped: No auth token available")
            return false
        }

        return try {
            // 1. Fetch and sync user profile
            val profileResponse = api.getUserProfile()
            if (profileResponse.isSuccessful) {
                profileResponse.body()?.let { remoteProfile ->
                    dao.insertOrUpdateProfile(remoteProfile.toEntity())
                    Log.d(TAG, "Profile synced from API")
                }
            }

            // 2. Fetch and merge remote vehicles
            val vehiclesResponse = api.getVehicles()
            if (vehiclesResponse.isSuccessful) {
                val remoteVehicles = vehiclesResponse.body() ?: emptyList()
                val localVehicles = dao.getAllVehicles().firstOrNull() ?: emptyList()
                for (remoteV in remoteVehicles) {
                    val entity = remoteV.toEntity()
                    if (localVehicles.none { it.licensePlate == entity.licensePlate }) {
                        dao.insertVehicle(entity)
                        Log.d(TAG, "New vehicle synced from API: ${entity.name}")
                    }
                }
            }

            // 3. Fetch and merge remote service logs
            val servicesResponse = api.getAllServices()
            if (servicesResponse.isSuccessful) {
                val remoteLogs = servicesResponse.body() ?: emptyList()
                val localLogs = dao.getAllServiceLogs().firstOrNull() ?: emptyList()
                for (remoteL in remoteLogs) {
                    val entity = remoteL.toEntity()
                    if (localLogs.none { it.title == entity.title && it.date == entity.date }) {
                        dao.insertServiceLog(entity)
                        Log.d(TAG, "New service log synced from API: ${entity.title}")
                    }
                }
            }

            Log.d(TAG, "Full sync completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            false
        }
    }
}
