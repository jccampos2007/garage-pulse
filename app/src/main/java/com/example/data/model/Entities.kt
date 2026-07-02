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

fun Vehicle.getStatisticalControlKpd(): Double {
    val initDate = this.initialDate ?: this.lastUpdatedDate
    val initKm = this.initialKm ?: this.odometer
    val calibKm = maxOf(0.0, this.odometer - initKm)
    val elapsedDays = maxOf(0.01, (System.currentTimeMillis() - initDate).toDouble() / (1000.0 * 60 * 60 * 24))
    return if (calibKm > 0.0) calibKm / elapsedDays else 0.0
}

fun Vehicle.getEffectiveKpd(): Double {
    if (this.calculatedKpd > 0.0) {
        return this.calculatedKpd
    }
    val controlKpd = this.getStatisticalControlKpd()
    if (controlKpd > 0.0) {
        return controlKpd
    }
    return when {
        this.usageType.equals("PUBLICO", ignoreCase = true) || 
        this.usageType.equals("PÚBLICO", ignoreCase = true) || 
        this.usageType.equals("TAXI", ignoreCase = true) || 
        this.usageType.equals("CARGA", ignoreCase = true) -> 120.0
        this.type.equals("Motorcycle", ignoreCase = true) || 
        this.type.equals("Moto", ignoreCase = true) -> 25.0
        else -> 42.5
    }
}
