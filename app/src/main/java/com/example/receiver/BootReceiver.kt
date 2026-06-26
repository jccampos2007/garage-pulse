package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.service.TelemetryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed received")
            val pendingResult = goAsync()
            val database = AppDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = database.databaseDao()
                    val userProfile = dao.getUserProfileDirect()
                    val activeVehicle = dao.getActiveVehicleDirect()

                    if (userProfile != null && activeVehicle != null) {
                        val initialDate = activeVehicle.initialDate ?: System.currentTimeMillis()
                        val daysSinceCreation = (System.currentTimeMillis() - initialDate) / (1000 * 60 * 60 * 24)
                        val isFreeTrial = daysSinceCreation <= 7
                        
                        if (userProfile.isPremium || isFreeTrial) {
                            Log.d("BootReceiver", "Starting TelemetryService on boot")
                            val serviceIntent = Intent(context, TelemetryService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error on boot receiver", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
