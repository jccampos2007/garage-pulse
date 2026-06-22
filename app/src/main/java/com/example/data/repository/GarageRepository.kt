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
        // Check if database has vehicles
        val vehiclesList = allVehicles.firstOrNull() ?: emptyList()
        if (vehiclesList.isEmpty()) {
            // Populate vehicles
            val vehicle1 = Vehicle(
                id = 1,
                name = "2024 Chery Arauca",
                brand = "Chery",
                model = "Arauca",
                year = 2024,
                licensePlate = "AA2024XY",
                status = "Optimal",
                odometer = 42500.0,
                isActive = true,
                type = "Car",
                photoUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhohr5P2dCxFvCtHhd8LekMaLiFz-QggHJfhsrrcVqfFKuZuDDlSbDBlaT3H1rSTzcsCZw--vWptlNLmvJi6IVrQgEsk1tb6vB8aXeCphAqFozzE6J5S3Ez7B-PBMalJpYSdrPKfUdQX8-rmvmtsu9C1yAo3ZvoSH_EcLZBQYUp8_IH0qADqtNCdOUyIeXA9XwU-t5M_YrkMKrGbmae3u7hdQBSjhbvDWI0bgcvy8ZlXWGyM7pkkB4dZ1W5Y2MDYKh58xeg6N_fE"
            )
            val vehicle2 = Vehicle(
                id = 2,
                name = "Toyota Hilux 2.8 D-4D",
                brand = "Toyota",
                model = "Hilux",
                year = 2021,
                licensePlate = "TX-8839",
                status = "Optimal",
                odometer = 112000.0,
                isActive = false,
                type = "Car",
                photoUri = "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=600&q=80"
            )
            val vehicle3 = Vehicle(
                id = 3,
                name = "Yamaha MT-07 690cc",
                brand = "Yamaha",
                model = "MT-07",
                year = 2023,
                licensePlate = "MC-y482",
                status = "Optimal",
                odometer = 12400.0,
                isActive = false,
                type = "Motorcycle",
                photoUri = null
            )
            val vehicle4 = Vehicle(
                id = 4,
                name = "Vespa GTS 300 Super",
                brand = "Vespa",
                model = "GTS 300",
                year = 2022,
                licensePlate = "VP-7722",
                status = "Optimal",
                odometer = 3500.0,
                isActive = false,
                type = "Motorcycle",
                photoUri = "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=600&q=80"
            )

            dao.insertVehicle(vehicle1)
            dao.insertVehicle(vehicle2)
            dao.insertVehicle(vehicle3)
            dao.insertVehicle(vehicle4)

            // Populate User
            val defaultUser = UserProfile(
                id = 1,
                name = "Carlos Rodríguez",
                email = "carlos.rod@garagepulse.app",
                avatarUrl = "",
                useKm = true
            )
            dao.insertOrUpdateProfile(defaultUser)

            // Populate Service Logs matches exactly historical dashboard screen items
            // Date definitions:
            // 12 Oct 2023
            val cal = Calendar.getInstance()
            cal.set(2023, Calendar.OCTOBER, 12, 10, 0)
            val dateOct12 = cal.timeInMillis

            // 05 Oct 2023
            cal.set(2023, Calendar.OCTOBER, 5, 14, 30)
            val dateOct05 = cal.timeInMillis

            // 22 Sep 2023
            cal.set(2023, Calendar.SEPTEMBER, 22, 9, 15)
            val dateSep22 = cal.timeInMillis

            // 15 Sep 2023
            cal.set(2023, Calendar.SEPTEMBER, 15, 11, 45)
            val dateSep15 = cal.timeInMillis

            // 02 Sep 2023
            cal.set(2023, Calendar.SEPTEMBER, 2, 16, 0)
            val dateSep02 = cal.timeInMillis

            val logs = listOf(
                ServiceLog(
                    vehicleId = 1,
                    category = "Cambio de Aceite",
                    title = "Cambio de Aceite",
                    description = "Sintético 5W-30",
                    cost = 85.00,
                    mileage = 45200.0,
                    date = dateOct12,
                    type = "PREVENTIVO"
                ),
                ServiceLog(
                    vehicleId = 1,
                    category = "Neumáticos",
                    title = "Rotación de Llantas",
                    description = "Balanceo incluido",
                    cost = 40.00,
                    mileage = 44800.0,
                    date = dateOct05,
                    type = "PREVENTIVO"
                ),
                ServiceLog(
                    vehicleId = 1,
                    category = "Frenos",
                    title = "Pastillas de Freno",
                    description = "Eje delantero",
                    cost = 210.00,
                    mileage = 42150.0,
                    date = dateSep22,
                    type = "REPARACIONES"
                ),
                ServiceLog(
                    vehicleId = 1,
                    category = "General",
                    title = "Filtro de Aire",
                    description = "Motor y Cabina",
                    cost = 25.00,
                    mileage = 41900.0,
                    date = dateSep15,
                    type = "PREVENTIVO"
                ),
                ServiceLog(
                    vehicleId = 1,
                    category = "Suspensión",
                    title = "Alineación 3D",
                    description = "Preventivo",
                    cost = 65.00,
                    mileage = 41000.0,
                    date = dateSep02,
                    type = "PREVENTIVO"
                )
            )

            for (log in logs) {
                dao.insertServiceLog(log)
            }

            // Insert random/older service history items so that overall we have 28 services total as in mockup
            for (i in 6..28) {
                val randVehicleId = (1..4).random()
                val isPreventive = (1..3).random() != 1
                val logType = if (isPreventive) "PREVENTIVO" else "REPARACIONES"
                val logCategory = listOf("General", "Cambio de Aceite", "Frenos", "Neumáticos", "Motor", "Suspensión").random()
                val logCost = listOf(15.0, 30.0, 45.0, 75.0, 120.0, 350.0).random()
                cal.add(Calendar.DAY_OF_YEAR, -((3..15).random()))

                dao.insertServiceLog(
                    ServiceLog(
                        vehicleId = randVehicleId,
                        category = logCategory,
                        title = "$logCategory de Rutina",
                        description = "Inspección periódica",
                        cost = logCost,
                        mileage = if (randVehicleId == 1) 40000.0 - i * 150 else 100000.0 - i * 200,
                        date = cal.timeInMillis,
                        type = logType
                    )
                )
            }
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
