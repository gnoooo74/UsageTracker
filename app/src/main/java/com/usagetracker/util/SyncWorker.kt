package com.usagetracker.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usagetracker.data.repository.TrackerRepository

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val repository = TrackerRepository(applicationContext)
            repository.syncAppUsage(null) // null = 오늘
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
