package com.example.data.api

import com.example.data.model.ServiceLog
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ========== AUTH DTOs ==========

@JsonClass(generateAdapter = true)
data class AuthRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val vehicleBrand: String,
    val vehicleModel: String,
    val initialOdometer: Double,
    val licensePlate: String
)

@JsonClass(generateAdapter = true)
data class AuthLoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthRegisterResponse(
    val token: String,
    val user: ApiUserProfile,
    val vehicle: ApiVehicle
)

@JsonClass(generateAdapter = true)
data class AuthLoginResponse(
    val token: String,
    val user: ApiUserProfile
)

// ========== USER PROFILE DTO ==========

@JsonClass(generateAdapter = true)
data class ApiUserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val useKm: Boolean = true,
    val isPremium: Boolean = false
) {
    fun toEntity(): UserProfile = UserProfile(
        id = id,
        name = name,
        email = email,
        avatarUrl = avatarUrl ?: "",
        useKm = useKm,
        isPremium = isPremium
    )
}

@JsonClass(generateAdapter = true)
data class ApiUserProfileUpdate(
    val name: String,
    val useKm: Boolean
)

// ========== VEHICLE DTO ==========

@JsonClass(generateAdapter = true)
data class ApiVehicle(
    val id: Int = 0,
    val name: String,
    val brand: String? = null,
    val model: String? = null,
    val year: Int = 0,
    @Json(name = "license_plate") val licensePlate: String,
    val status: String = "Optimal",
    val odometer: Double = 0.0,
    @Json(name = "is_active") val isActive: Boolean = false,
    val type: String = "Car",
    @Json(name = "photo_uri") val photoUri: String? = null,
    @Json(name = "initial_km") val initialKm: Double? = null,
    @Json(name = "initial_date") val initialDate: Long? = null,
    @Json(name = "last_updated_date") val lastUpdatedDate: Long? = null,
    @Json(name = "calculated_kpd") val calculatedKpd: Double = 0.0,
    @Json(name = "last_known_location") val lastKnownLocation: String? = null,
    @Json(name = "custom_illustration_url") val customIllustrationUrl: String? = null,
    @Json(name = "usage_type") val usageType: String = "PARTICULAR",
    @Json(name = "user_id") val userId: Int? = null
) {
    fun toEntity(): Vehicle = Vehicle(
        id = id,
        name = name,
        brand = brand ?: "",
        model = model ?: "",
        year = year,
        licensePlate = licensePlate,
        status = status,
        odometer = odometer,
        isActive = isActive,
        type = type,
        photoUri = photoUri,
        initialKm = initialKm,
        initialDate = initialDate,
        lastUpdatedDate = lastUpdatedDate ?: System.currentTimeMillis(),
        calculatedKpd = calculatedKpd,
        lastKnownLocation = lastKnownLocation,
        customIllustrationUrl = customIllustrationUrl,
        usageType = usageType
    )
}

fun Vehicle.toApiDto(): ApiVehicle = ApiVehicle(
    id = id,
    name = name,
    brand = brand,
    model = model,
    year = year,
    licensePlate = licensePlate,
    status = status,
    odometer = odometer,
    isActive = isActive,
    type = type,
    photoUri = photoUri,
    initialKm = initialKm,
    initialDate = initialDate,
    lastUpdatedDate = lastUpdatedDate,
    calculatedKpd = calculatedKpd,
    lastKnownLocation = lastKnownLocation,
    customIllustrationUrl = customIllustrationUrl,
    usageType = usageType
)

// ========== SERVICE LOG DTO ==========

@JsonClass(generateAdapter = true)
data class ApiServiceLog(
    val id: Int = 0,
    @Json(name = "vehicle_id") val vehicleId: Int,
    val category: String,
    val title: String,
    val description: String? = null,
    val cost: Double,
    val mileage: Double,
    val date: Long,
    val type: String,
    val details: String? = null
) {
    fun toEntity(): ServiceLog = ServiceLog(
        id = id,
        vehicleId = vehicleId,
        category = category,
        title = title,
        description = description ?: "",
        cost = cost,
        mileage = mileage,
        date = date,
        type = type,
        details = details ?: ""
    )
}

fun ServiceLog.toApiDto(): ApiServiceLog = ApiServiceLog(
    id = id,
    vehicleId = vehicleId,
    category = category,
    title = title,
    description = description,
    cost = cost,
    mileage = mileage,
    date = date,
    type = type,
    details = details
)

// ========== VEHICLE UPDATE DTO ==========
// Partial update DTO matching what the backend PUT /api/vehicles/:id expects

@JsonClass(generateAdapter = true)
data class ApiVehicleUpdate(
    val odometer: Double? = null,
    val customIllustrationUrl: String? = null,
    val isActive: Boolean? = null
)
