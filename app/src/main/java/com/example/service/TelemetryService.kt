package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.data.api.ApiVehicleUpdate
import com.example.data.api.RetrofitClient
import com.example.data.api.TokenManager
import com.example.data.local.AppDatabase
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.*

class TelemetryService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    // Simplification for the mock context: we'll simulate IN_VEHICLE optimization by reducing update intervals
    // For full ActivityRecognition we would need PendingIntents and BroadcastReceivers.
    
    private var lastLocation: Location? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    processNewLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "telemetry_channel")
            .setContentTitle("GaragePulse Premium")
            .setContentText("Rastreo Perpetuo Activo (Modo Vehículo)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun processNewLocation(location: Location) {
        serviceScope.launch {
            val dao = database.databaseDao()
            val userProfile = dao.getUserProfileDirect() ?: return@launch
            val activeVehicle = dao.getActiveVehicleDirect() ?: return@launch

            val initialDate = activeVehicle.initialDate ?: System.currentTimeMillis()
            val daysSinceCreation = (System.currentTimeMillis() - initialDate) / (1000 * 60 * 60 * 24)
            val isFreeTrial = daysSinceCreation <= 7

            if (!userProfile.isPremium && !isFreeTrial) {
                stopSelf()
                return@launch
            }

            val lastLocStr = activeVehicle.lastKnownLocation
            if (lastLocStr != null) {
                val parts = lastLocStr.split(",")
                if (parts.size == 2) {
                    val prevLat = parts[0].toDoubleOrNull() ?: 0.0
                    val prevLon = parts[1].toDoubleOrNull() ?: 0.0
                    
                    val distanceKm = calculateHaversineDistance(prevLat, prevLon, location.latitude, location.longitude)
                    
                    val updateThresholdKm = 0.01
                    
                    // Update currentKm (odometer) every threshold (10m for everyone)
                    if (distanceKm >= updateThresholdKm) {
                        val newOdometer = activeVehicle.odometer + distanceKm
                        val updatedVehicle = activeVehicle.copy(
                            odometer = newOdometer,
                            lastKnownLocation = "${location.latitude},${location.longitude}",
                            lastUpdatedDate = System.currentTimeMillis()
                        )
                        dao.updateVehicle(updatedVehicle)

                        try {
                            if (TokenManager.isAuthenticated()) {
                                RetrofitClient.garageApiService.updateVehicle(
                                    updatedVehicle.id,
                                    ApiVehicleUpdate(
                                        odometer = updatedVehicle.odometer,
                                        lastKnownLocation = updatedVehicle.lastKnownLocation,
                                        lastUpdatedDate = updatedVehicle.lastUpdatedDate,
                                        calculatedKpd = updatedVehicle.calculatedKpd
                                    )
                                )
                                Log.d("TelemetryService", "Synced updated odometer to API: ${updatedVehicle.odometer} km")
                            }
                        } catch (e: Exception) {
                            Log.w("TelemetryService", "Failed to sync telemetry to API: ${e.message}")
                        }

                        // Accumulate GPS tracked distance in preferences (total and today)
                        val gpsPrefs = getSharedPreferences("GaragePulsePrefs", Context.MODE_PRIVATE)
                        val currentGpsDist = gpsPrefs.getFloat("gps_distance_vehicle_${activeVehicle.id}", 0f)
                        val newGpsDist = currentGpsDist + distanceKm.toFloat()
                        
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                        val todayKey = "gps_distance_today_${activeVehicle.id}_$todayStr"
                        val currentTodayDist = gpsPrefs.getFloat(todayKey, 0f)
                        val newTodayDist = currentTodayDist + distanceKm.toFloat()

                        gpsPrefs.edit()
                            .putFloat("gps_distance_vehicle_${activeVehicle.id}", newGpsDist)
                            .putFloat(todayKey, newTodayDist)
                            .apply()

                        Log.d("TelemetryService", "Updated odometer: +$distanceKm km")
                        Log.d("TelemetryService", "Updated GPS tracked distance (total): $newGpsDist km, (today): $newTodayDist km")
                        
                        checkPredictiveAlerts(dao, activeVehicle, newOdometer)
                    }
                }
            } else {
                val updatedVehicle = activeVehicle.copy(
                    lastKnownLocation = "${location.latitude},${location.longitude}"
                )
                dao.updateVehicle(updatedVehicle)
                try {
                    if (TokenManager.isAuthenticated()) {
                        RetrofitClient.garageApiService.updateVehicle(
                            updatedVehicle.id,
                            ApiVehicleUpdate(
                                lastKnownLocation = updatedVehicle.lastKnownLocation
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("TelemetryService", "Failed to sync initial location to API: ${e.message}")
                }
            }
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return R * c
    }

    private suspend fun checkPredictiveAlerts(dao: com.example.data.local.DatabaseDao, activeVehicle: com.example.data.model.Vehicle, newOdometer: Double) {
        val logs = dao.getServiceLogsForVehicle(activeVehicle.id).firstOrNull() ?: return
        val garagePrefs = getSharedPreferences("garage_pulse_prefs", Context.MODE_PRIVATE)
        val defaultCategories = listOf(
            Pair("Neumáticos", listOf(Pair("Rotación y Alineación", Pair(10000.0, 180)), Pair("Alineación", Pair(10000.0, 180)), Pair("Cambio", Pair(40000.0, 730)))),
            Pair("Filtros", listOf(Pair("Gasolina", Pair(20000.0, 365)), Pair("Aceite", Pair(10000.0, 180)), Pair("Aire AC", Pair(15000.0, 365)))),
            Pair("Cambio de Aceite", listOf(Pair("Sintético 5W-30", Pair(10000.0, 180)), Pair("Mineral", Pair(5000.0, 90)), Pair("Semi-Sintético", Pair(7500.0, 135)))),
            Pair("Frenos", listOf(Pair("Pastillas y Discos", Pair(25000.0, 730)), Pair("Cambio de Liga", Pair(40000.0, 730)), Pair("Ajuste/Revisión", Pair(15000.0, 365)))),
            Pair("Batería", listOf(Pair("Test de Corriente", Pair(15000.0, 365)), Pair("Cambio de Batería", Pair(60000.0, 1095)), Pair("Limpieza de Bornes", Pair(10000.0, 180))))
        )
        val currentTime = System.currentTimeMillis()
        val prefs = getSharedPreferences("GaragePulsePrefs", Context.MODE_PRIVATE)

        for ((category, subServices) in defaultCategories) {
            val categoryLogs = logs.filter { log ->
                log.category.split(", ").any { c -> c.trim().equals(category, ignoreCase = true) } ||
                (category == "Neumáticos" && log.category.split(", ").any { c -> c.trim().equals("Llantas", ignoreCase = true) })
            }
            if (categoryLogs.isEmpty()) continue

            for ((subName, defaultPair) in subServices) {
                val intervalKm = garagePrefs.getFloat("config_sub_km_${category}_${subName}", defaultPair.first.toFloat()).toDouble()
                val intervalDays = garagePrefs.getInt("config_sub_days_${category}_${subName}", defaultPair.second)

                val validLogs = categoryLogs.filter { log ->
                    log.title.equals(subName, ignoreCase = true) ||
                    log.description.contains(subName, ignoreCase = true) ||
                    log.details.split(",").map { d -> d.trim() }.any { d -> d.equals(subName, ignoreCase = true) } ||
                    (categoryLogs.size == 1)
                }
                val latestLog = validLogs.maxByOrNull { it.date } ?: continue
                
                val baseMileage = latestLog.mileage
                val baseDate = latestLog.date
                
                val wearKm = ((newOdometer - baseMileage) / intervalKm).toFloat().coerceIn(0f, 1f)
                val wearDays = ((currentTime - baseDate).toDouble() / (24.0*60*60*1000) / intervalDays).toFloat().coerceIn(0f, 1f)
                val wearIndex = Math.max(wearKm, wearDays)
                
                if (wearIndex >= 0.90f) {
                    val alertLevel = if (wearIndex >= 1.0f) 100 else 90
                    val lastAlertKey = "alert_${activeVehicle.id}_${category}_${subName}_${latestLog.id}"
                    val lastAlertLevelSent = prefs.getInt(lastAlertKey, 0)
                    
                    if (alertLevel > lastAlertLevelSent) {
                        sendPushNotification("$subName ($category)", alertLevel)
                        prefs.edit().putInt(lastAlertKey, alertLevel).apply()
                    }
                }
            }
        }
    }

    private fun sendPushNotification(category: String, alertLevel: Int) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val title = if (alertLevel == 100) "🚨 Acción requerida" else "⚠️ Precaución"
        val text = if (alertLevel == 100) "Tu $category alcanzó el límite del 100%." else "Tu $category está al 90% de desgaste estimado."
        
        val notificationId = category.hashCode()
        val notification = NotificationCompat.Builder(this, "alerts_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "telemetry_channel",
                "Telemetry Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val alertsChannel = NotificationChannel(
                "alerts_channel",
                "Alertas Predictivas",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertsChannel)
        }
    }
}
