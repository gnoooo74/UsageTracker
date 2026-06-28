package com.usagetracker.data.repository

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.LiveData
import com.usagetracker.data.db.AppSummary
import com.usagetracker.data.db.DomainSummary
import com.usagetracker.data.db.TrackerDatabase
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.data.model.BrowserHistoryEntity
import com.usagetracker.util.BrowserAccessibilityService
import com.usagetracker.util.UsageStatsCollector
import java.text.SimpleDateFormat
import java.util.*

class TrackerRepository(private val context: Context) {

    private val db = TrackerDatabase.getInstance(context)
    private val appUsageDao = db.appUsageDao()
    private val browserHistoryDao = db.browserHistoryDao()

    private val usageCollector = UsageStatsCollector(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─── 앱 사용 기록 ────────────────────────────────────

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

    // ─── 브라우저 히스토리 (AccessibilityService 실시간 수집) ───

    fun getBrowserHistoryByDate(date: String): LiveData<List<BrowserHistoryEntity>> =
        browserHistoryDao.getByDate(date)

    fun getTopDomainsByDate(date: String): LiveData<List<DomainSummary>> =
        browserHistoryDao.getTopDomainsByDate(date)

    /** AccessibilityService에서 URL 감지 시 단건 저장 */
    suspend fun insertBrowserHistory(entity: BrowserHistoryEntity) {
        browserHistoryDao.insertAll(listOf(entity))
    }

    /** 수동 새로고침 시 30일 이전 데이터만 정리 (실시간 수집이므로 오늘 데이터 삭제 안 함) */
    suspend fun cleanupOldBrowserHistory() {
        browserHistoryDao.deleteOldData(getPastDate(30))
    }

    // ─── 권한 확인 ───────────────────────────────────────

    fun hasUsagePermission(): Boolean = usageCollector.hasPermission()

    /** 접근성 서비스 활성화 여부 확인 */
    fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        val serviceName = "${context.packageName}/${BrowserAccessibilityService::class.java.canonicalName}"
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) return true
        }
        return false
    }

    // ─── 유틸 ────────────────────────────────────────────

    private fun getPastDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo)
        return dateFormat.format(cal.time)
    }

    fun todayString(): String = dateFormat.format(Date())
}
