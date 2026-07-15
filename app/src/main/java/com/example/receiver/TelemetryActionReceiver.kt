package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONFIRM_VEHICLE = "com.example.action.CONFIRM_VEHICLE"
        const val ACTION_STOP_ALIEN_TRIP = "com.example.action.STOP_ALIEN_TRIP"
        const val ACTION_RESUME_TRIP = "com.example.action.RESUME_TRIP"
        const val DAILY_TRIP_CHECK_ID = 1001
        const val FOREGROUND_SERVICE_ID = 1
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("TelemetryActionReceiver", "Action received: $action")

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val tripPrefs = context.getSharedPreferences("TelemetryTripCheckPrefs", Context.MODE_PRIVATE)
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        when (action) {
            ACTION_CONFIRM_VEHICLE -> {
                Log.d("TelemetryActionReceiver", "User confirmed current active vehicle for today's trip.")
                notificationManager?.cancel(DAILY_TRIP_CHECK_ID)
            }
            ACTION_STOP_ALIEN_TRIP -> {
                Log.d("TelemetryActionReceiver", "User marked ALIEN VEHICLE. Pausing odometry tracking for today: $todayStr")
                tripPrefs.edit()
                    .putBoolean("paused_for_alien_vehicle_$todayStr", true)
                    .apply()

                notificationManager?.cancel(DAILY_TRIP_CHECK_ID)

                // Update TelemetryService foreground notification to reflect paused state
                val pausedNotification = NotificationCompat.Builder(context, "telemetry_channel")
                    .setContentTitle("GaragePulse Premium")
                    .setContentText("⏸️ Rastreo Pausado (Viajando en Vehículo Ajeno)")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                notificationManager?.notify(FOREGROUND_SERVICE_ID, pausedNotification)
            }
            ACTION_RESUME_TRIP -> {
                Log.d("TelemetryActionReceiver", "User resumed normal odometry tracking for today: $todayStr")
                tripPrefs.edit()
                    .putBoolean("paused_for_alien_vehicle_$todayStr", false)
                    .apply()

                // Restore active notification
                val activeNotification = NotificationCompat.Builder(context, "telemetry_channel")
                    .setContentTitle("GaragePulse Premium")
                    .setContentText("Rastreo Perpetuo Activo (Modo Vehículo)")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                notificationManager?.notify(FOREGROUND_SERVICE_ID, activeNotification)
            }
        }
    }
}
