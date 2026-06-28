package com.usagetracker

import android.app.Application
import androidx.work.*
import com.usagetracker.util.SyncWorker
import java.util.concurrent.TimeUnit

class TrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    /**
     * WorkManager로 15분마다 자동 동기화 예약
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "usage_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
