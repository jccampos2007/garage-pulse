package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase

class OdometerUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.databaseDao()

        val userProfile = dao.getUserProfileDirect() ?: return Result.success()
        val vehicles = dao.getAllVehiclesList()

        val currentTime = System.currentTimeMillis()

        vehicles.forEach { vehicle ->
            // Skip vehicles without calculated KPD (e.g. Case B calibration phase)
            if (vehicle.calculatedKpd <= 0) return@forEach

            val daysDiff = (currentTime - vehicle.lastUpdatedDate) / (1000 * 60 * 60 * 24).toDouble()
            
            // Only update if at least a significant portion of a day has passed (e.g. 1 day)
            // Or if Premium, we update it mathematically even for smaller intervals if needed
            if (daysDiff >= 1.0 || (userProfile.isPremium && daysDiff > 0.1)) {
                val addedKm = vehicle.calculatedKpd * daysDiff
                val newOdometer = vehicle.odometer + addedKm

                val updatedVehicle = vehicle.copy(
                    odometer = newOdometer,
                    lastUpdatedDate = currentTime
                )
                dao.updateVehicle(updatedVehicle)
            }
        }

        return Result.success()
    }
}
