package com.usagetracker.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.usagetracker.data.db.AppSummary
import com.usagetracker.data.db.TrackerDatabase
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.util.UsageStatsCollector
import java.text.SimpleDateFormat
import java.util.*

class TrackerRepository(private val context: Context) {

    private val db = TrackerDatabase.getInstance(context)
    private val appUsageDao = db.appUsageDao()
    private val usageCollector = UsageStatsCollector(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getAppUsageByDate(date: String): LiveData<List<AppUsageEntity>> =
        appUsageDao.getByDate(date)

    fun getAppSummaryByDate(date: String): LiveData<List<AppSummary>> =
        appUsageDao.getAppSummaryByDate(date)

    suspend fun syncAppUsage() {
        if (!usageCollector.hasPermission()) return
        val records = usageCollector.collectToday()
        val today = dateFormat.format(Date())
        appUsageDao.deleteByDate(today)
        appUsageDao.deleteOldData(getPastDate(30))
        if (records.isNotEmpty()) {
            appUsageDao.insertAll(records)
        }
    }

    fun hasUsagePermission(): Boolean = usageCollector.hasPermission()

    private fun getPastDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo)
        return dateFormat.format(cal.time)
    }

    fun todayString(): String = dateFormat.format(Date())
}
