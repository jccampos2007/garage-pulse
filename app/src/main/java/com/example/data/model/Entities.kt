package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val status: String = "Optimal",
    val odometer: Double = 42500.0,
    val isActive: Boolean = false,
    val type: String = "Car", // "Car" or "Motorcycle"
    val photoUri: String? = null,
    
    // Predictive Maintenance
    val initialKm: Double? = null,
    val initialDate: Long? = null,
    val lastUpdatedDate: Long = System.currentTimeMillis(),
    val calculatedKpd: Double = 0.0,
    
    // Premium Telemetry & Customization
    val lastKnownLocation: String? = null,
    val customIllustrationUrl: String? = null,
    
    // Usage profiling
    val usageType: String = "PARTICULAR"
) : Serializable

@Entity(tableName = "service_logs")
data class ServiceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val category: String, // e.g. "Cambio de Aceite", "Frenos", "Neumáticos", "Motor", "Suspensión", "General"
    val title: String,
    val description: String,
    val cost: Double,
    val mileage: Double,
    val date: Long, // timestamp
    val type: String, // "PREVENTIVO" or "REPARACIONES"
    val details: String = "" // CSV or JSON for specific actions
) : Serializable

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only one user profile for simplicity
    val name: String = "Carlos Rodríguez",
    val email: String = "carlos.rod@garagepulse.app",
    val avatarUrl: String = "",
    val useKm: Boolean = true,
    val isPremium: Boolean = false
) : Serializable
