package com.example.data.api

import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import kotlinx.coroutines.delay

/**
 * Mock implementation for local testing without a backend server.
 * This class is NOT connected to the GarageApiService Retrofit interface.
 * It is preserved for unit testing and offline-only development scenarios.
 * 
 * To use: instantiate directly as MockGarageApiService() in tests.
 */
class MockGarageApiService {

    private var userProfileState = UserProfile(
        id = 1,
        name = "Carlos Rodríguez",
        email = "carlos.rod@garagepulse.app",
        avatarUrl = "",
        useKm = true
    )

    private val vehiclesListState = mutableListOf(
        Vehicle(
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
        ),
        Vehicle(
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
        ),
        Vehicle(
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
        ),
        Vehicle(
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
    )

    private val servicesListState = mutableListOf<ServiceLog>()

    init {
        // Initialize with default API service logs
        servicesListState.add(
            ServiceLog(
                id = 1,
                vehicleId = 1,
                category = "Cambio de Aceite",
                title = "Cambio de Aceite",
                description = "Sintético 5W-30 (Remote Cached Service)",
                cost = 85.00,
                mileage = 45200.0,
                date = System.currentTimeMillis() - 86400000 * 5,
                type = "PREVENTIVO"
            )
        )
        servicesListState.add(
            ServiceLog(
                id = 2,
                vehicleId = 1,
                category = "Frenos",
                title = "Pastillas de freno traseras",
                description = "Frenos de cerámica (Remote Cached Service)",
                cost = 145.00,
                mileage = 43200.0,
                date = System.currentTimeMillis() - 86400000 * 12,
                type = "REPARACIONES"
            )
        )
    }

    suspend fun getUserProfile(): UserProfile {
        delay(800) // simulate API latency
        return userProfileState
    }

    suspend fun updateUserProfile(profile: UserProfile): UserProfile {
        delay(1000)
        userProfileState = profile
        return userProfileState
    }

    suspend fun getVehicles(): List<Vehicle> {
        delay(900)
        return vehiclesListState.toList()
    }

    suspend fun createVehicle(vehicle: Vehicle): Vehicle {
        delay(1200)
        val newVehicle = if (vehicle.id == 0) {
            vehicle.copy(id = (vehiclesListState.maxOfOrNull { it.id } ?: 0) + 1)
        } else {
            vehicle
        }
        vehiclesListState.add(newVehicle)
        return newVehicle
    }

    suspend fun updateVehicle(id: Int, vehicle: Vehicle): Vehicle {
        delay(1000)
        val index = vehiclesListState.indexOfFirst { it.id == id }
        if (index != -1) {
            vehiclesListState[index] = vehicle
        } else {
            vehiclesListState.add(vehicle)
        }
        return vehicle
    }

    suspend fun deleteVehicle(id: Int) {
        delay(800)
        vehiclesListState.removeAll { it.id == id }
    }

    suspend fun getAllServices(): List<ServiceLog> {
        delay(950)
        return servicesListState.toList()
    }

    suspend fun getServicesForVehicle(vehicleId: Int): List<ServiceLog> {
        delay(700)
        return servicesListState.filter { it.vehicleId == vehicleId }
    }

    suspend fun createServiceLog(serviceLog: ServiceLog): ServiceLog {
        delay(1100)
        val newLog = if (serviceLog.id == 0) {
            serviceLog.copy(id = (servicesListState.maxOfOrNull { it.id } ?: 0) + 1)
        } else {
            serviceLog
        }
        servicesListState.add(newLog)
        return newLog
    }

    suspend fun deleteServiceLog(id: Int) {
        delay(800)
        servicesListState.removeAll { it.id == id }
    }
}
