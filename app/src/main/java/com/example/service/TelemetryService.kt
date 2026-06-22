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
import com.example.data.local.AppDatabase
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            .setMinUpdateDistanceMeters(50f)
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
                    
                    // Update currentKm (odometer) every 200 meters (0.2 km)
                    if (distanceKm >= 0.2) {
                        val newOdometer = activeVehicle.odometer + distanceKm
                        val updatedVehicle = activeVehicle.copy(
                            odometer = newOdometer,
                            lastKnownLocation = "${location.latitude},${location.longitude}",
                            lastUpdatedDate = System.currentTimeMillis()
                        )
                        dao.updateVehicle(updatedVehicle)
                        Log.d("TelemetryService", "Updated odometer: +$distanceKm km")
                    }
                }
            } else {
                // First time getting location
                val updatedVehicle = activeVehicle.copy(
                    lastKnownLocation = "${location.latitude},${location.longitude}"
                )
                dao.updateVehicle(updatedVehicle)
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
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
