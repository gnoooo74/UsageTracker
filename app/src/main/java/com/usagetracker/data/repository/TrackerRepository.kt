package com.usagetracker.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.usagetracker.data.db.AppSummary
import com.usagetracker.data.db.DomainSummary
import com.usagetracker.data.db.TrackerDatabase
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.data.model.BrowserHistoryEntity
import com.usagetracker.util.BrowserHistoryCollector
import com.usagetracker.util.UsageStatsCollector
import java.text.SimpleDateFormat
import java.util.*

class TrackerRepository(context: Context) {

    private val db = TrackerDatabase.getInstance(context)
    private val appUsageDao = db.appUsageDao()
    private val browserHistoryDao = db.browserHistoryDao()

    private val usageCollector = UsageStatsCollector(context)
    private val browserCollector = BrowserHistoryCollector(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─── 앱 사용 기록 ────────────────────────────────────

    fun getAppUsageByDate(date: String): LiveData<List<AppUsageEntity>> =
        appUsageDao.getByDate(date)

    fun getAppSummaryByDate(date: String): LiveData<List<AppSummary>> =
        appUsageDao.getAppSummaryByDate(date)

    /**
     * 오늘 앱 사용 기록 수집 및 저장
     */
    suspend fun syncAppUsage() {
        if (!usageCollector.hasPermission()) return
        val records = usageCollector.collectToday()
        val today = dateFormat.format(Date())
        // 오늘 데이터 완전 삭제 후 재삽입 (중복 누적 방지)
        appUsageDao.deleteByDate(today)
        appUsageDao.deleteOldData(getPastDate(30))
        if (records.isNotEmpty()) {
            appUsageDao.insertAll(records)
        }
    }

    // ─── 브라우저 히스토리 ───────────────────────────────

    fun getBrowserHistoryByDate(date: String): LiveData<List<BrowserHistoryEntity>> =
        browserHistoryDao.getByDate(date)

    fun getTopDomainsByDate(date: String): LiveData<List<DomainSummary>> =
        browserHistoryDao.getTopDomainsByDate(date)

    /**
     * 오늘 브라우저 히스토리 수집 및 저장
     */
    suspend fun syncBrowserHistory() {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val records = browserCollector.collectAll(
            startTime = today.timeInMillis,
            endTime = System.currentTimeMillis()
        )
        val todayStr = dateFormat.format(Date())
        // 오늘 데이터 완전 삭제 후 재삽입 (중복 누적 방지)
        browserHistoryDao.deleteByDate(todayStr)
        browserHistoryDao.deleteOldData(getPastDate(30))
        if (records.isNotEmpty()) {
            browserHistoryDao.insertAll(records)
        }
    }

    // ─── 권한 확인 ───────────────────────────────────────

    fun hasUsagePermission(): Boolean = usageCollector.hasPermission()

    // ─── 유틸 ────────────────────────────────────────────

    private fun getPastDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo)
        return dateFormat.format(cal.time)
    }

    fun todayString(): String = dateFormat.format(Date())
}
