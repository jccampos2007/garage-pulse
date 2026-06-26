package com.example.data.repository

import com.example.data.local.DatabaseDao
import com.example.data.model.Vehicle
import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class GarageRepository(private val dao: DatabaseDao) {

    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    val activeVehicleFlow: Flow<Vehicle?> = dao.getActiveVehicleFlow()
    val allServiceLogs: Flow<List<ServiceLog>> = dao.getAllServiceLogs()
    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()

    fun getServiceLogsForVehicle(vehicleId: Int): Flow<List<ServiceLog>> {
        return dao.getServiceLogsForVehicle(vehicleId)
    }

    private val mockApiService: com.example.data.api.GarageApiService = com.example.data.api.MockGarageApiService()

    val api: com.example.data.api.GarageApiService get() = mockApiService

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        val rowId = dao.insertVehicle(vehicle)
        try {
            mockApiService.createVehicle(vehicle.copy(id = rowId.toInt()))
        } catch (e: Exception) {
            // Handled offline gracefully
        }
        return rowId
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        dao.updateVehicle(vehicle)
        try {
            mockApiService.updateVehicle(vehicle.id, vehicle)
        } catch (e: Exception) {
            // Handled offline gracefully
        }
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteVehicle(vehicle)
        try {
            mockApiService.deleteVehicle(vehicle.id)
        } catch (e: Exception) {
            // Handled offline gracefully
        }
    }

    suspend fun selectActiveVehicle(vehicleId: Int) {
        dao.selectActiveVehicle(vehicleId)
    }

    suspend fun insertServiceLog(serviceLog: ServiceLog): Long {
        val rowId = dao.insertServiceLog(serviceLog)
        
        // Recalculate KPD (Case A) when a new log is inserted
        recalculateVehicleKpd(serviceLog.vehicleId)
        
        try {
            mockApiService.createServiceLog(serviceLog.copy(id = rowId.toInt()))
        } catch (e: Exception) {
            // Handled offline gracefully
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
                    dao.updateVehicle(updatedVehicle)
                }
            }
        }
    }

    suspend fun deleteServiceLog(serviceLog: ServiceLog) {
        dao.deleteServiceLog(serviceLog)
        try {
            mockApiService.deleteServiceLog(serviceLog.id)
        } catch (e: Exception) {
            // Handled offline gracefully
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dao.insertOrUpdateProfile(profile)
        try {
            mockApiService.updateUserProfile(profile)
        } catch (e: Exception) {
            // Handled offline gracefully
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

    suspend fun syncLocalWithRemote(): Boolean {
        return try {
            // Simulate synchronizing local Room DB records with a remote REST container
            // 1. Fetch remote user profile and merge
            val remoteProfile = mockApiService.getUserProfile()
            dao.insertOrUpdateProfile(remoteProfile)

            // 2. Fetch remote vehicles and insert any that are missing
            val remoteVehicles = mockApiService.getVehicles()
            val localVehicles = dao.getAllVehicles().firstOrNull() ?: emptyList()
            for (remoteV in remoteVehicles) {
                if (localVehicles.none { it.licensePlate == remoteV.licensePlate }) {
                    dao.insertVehicle(remoteV)
                }
            }

            // 3. Fetch remote service logs and merge
            val remoteLogs = mockApiService.getAllServices()
            val localLogs = dao.getAllServiceLogs().firstOrNull() ?: emptyList()
            for (remoteL in remoteLogs) {
                if (localLogs.none { it.title == remoteL.title && it.date == remoteL.date }) {
                    dao.insertServiceLog(remoteL)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
