package com.usagetracker.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.usagetracker.data.model.AppUsageEntity
import java.text.SimpleDateFormat
import java.util.*

class UsageStatsCollector(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 사용 통계 권한 확인
     */
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 특정 날짜의 앱 사용 이벤트 수집
     * ACTIVITY_RESUMED → ACTIVITY_PAUSED/STOPPED 쌍으로 매칭
     */
    fun collectForDate(date: Calendar): List<AppUsageEntity> {
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = startOfDay.clone() as Calendar
        endOfDay.add(Calendar.DAY_OF_MONTH, 1)

        return collectEvents(startOfDay.timeInMillis, endOfDay.timeInMillis)
    }

    /**
     * 오늘 하루 수집
     */
    fun collectToday(): List<AppUsageEntity> {
        return collectForDate(Calendar.getInstance())
    }

    /**
     * 지정 시간 범위 이벤트 수집 및 파싱
     */
    private fun collectEvents(startTime: Long, endTime: Long): List<AppUsageEntity> {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        // 앱별 마지막 RESUME 시각 저장
        val resumeMap = mutableMapOf<String, Long>()
        val result = mutableListOf<AppUsageEntity>()
        val dateStr = dateFormat.format(Date(startTime))

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            val packageName = event.packageName ?: continue

            // 자기 자신 제외
            if (packageName == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumeMap[packageName] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumeTime = resumeMap.remove(packageName) ?: continue
                    val duration = event.timeStamp - resumeTime

                    // 1초 미만 무시 (광고/팝업 등)
                    if (duration < 1000) continue

                    val appName = getAppName(packageName)
                    result.add(
                        AppUsageEntity(
                            packageName = packageName,
                            appName = appName,
                            foregroundTime = resumeTime,
                            backgroundTime = event.timeStamp,
                            durationMs = duration,
                            date = dateStr
                        )
                    )
                }
            }
        }

        // 아직 종료 안 된 앱 (현재 사용중)
        val now = System.currentTimeMillis()
        resumeMap.forEach { (packageName, resumeTime) ->
            if (resumeTime < now) {
                val appName = getAppName(packageName)
                result.add(
                    AppUsageEntity(
                        packageName = packageName,
                        appName = appName,
                        foregroundTime = resumeTime,
                        backgroundTime = null,
                        durationMs = now - resumeTime,
                        date = dateStr
                    )
                )
            }
        }

        return result.sortedBy { it.foregroundTime }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
