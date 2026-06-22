package com.example.data.local

import androidx.room.*
import com.example.data.model.Vehicle
import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {

    // --- VEHICLES ---
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesList(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun getActiveVehicleFlow(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVehicleDirect(): Vehicle?

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Transaction
    suspend fun selectActiveVehicle(vehicleId: Int) {
        clearActiveVehicles()
        setActiveVehicle(vehicleId)
    }

    @Query("UPDATE vehicles SET isActive = 0")
    suspend fun clearActiveVehicles()

    @Query("UPDATE vehicles SET isActive = 1 WHERE id = :vehicleId")
    suspend fun setActiveVehicle(vehicleId: Int)


    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()

    // --- SERVICE LOGS ---
    @Query("SELECT * FROM service_logs ORDER BY date DESC")
    fun getAllServiceLogs(): Flow<List<ServiceLog>>

    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getServiceLogsForVehicle(vehicleId: Int): Flow<List<ServiceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceLog(serviceLog: ServiceLog): Long

    @Delete
    suspend fun deleteServiceLog(serviceLog: ServiceLog)

    @Query("DELETE FROM service_logs")
    suspend fun deleteAllServiceLogs()


    // --- USER PROFILE ---
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}
