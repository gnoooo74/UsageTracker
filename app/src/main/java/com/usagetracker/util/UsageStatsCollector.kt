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

    // 제외할 시스템/런처 앱 패키지 목록
    val excludePackages = setOf(
        "com.sec.android.app.launcher",
        "com.sec.android.app.launcher3",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.microsoft.launcher",
        "com.action.launcher",
        "com.teslacoilsw.launcher",
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.samsung.android.honeyboard",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
    )

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

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

    fun collectToday(): List<AppUsageEntity> = collectForDate(Calendar.getInstance())

    /**
     * filterSystem: true = 시스템/런처 앱 제외, false = 전체
     * minDurationMs: 0 = 1ms 이상 전부, 1000 = 1초 이상만
     */
    private fun collectEvents(startTime: Long, endTime: Long): List<AppUsageEntity> {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val resumeMap = mutableMapOf<String, Long>()
        val result = mutableListOf<AppUsageEntity>()
        val dateStr = dateFormat.format(Date(startTime))

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue  // 자기 자신만 제외

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumeMap[packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumeTime = resumeMap.remove(packageName) ?: continue
                    val duration = event.timeStamp - resumeTime
                    if (duration <= 0) continue  // 음수만 제외, 1ms 이상 전부 기록

                    val appName = getAppName(packageName)
                    val isSystem = excludePackages.contains(packageName)
                    result.add(
                        AppUsageEntity(
                            packageName = packageName,
                            appName = appName,
                            foregroundTime = resumeTime,
                            backgroundTime = event.timeStamp,
                            durationMs = duration,
                            date = dateStr,
                            isSystemApp = isSystem
                        )
                    )
                }
            }
        }

        // 현재 사용중인 앱
        val now = System.currentTimeMillis()
        resumeMap.forEach { (packageName, resumeTime) ->
            if (resumeTime < now) {
                val appName = getAppName(packageName)
                val isSystem = excludePackages.contains(packageName)
                result.add(
                    AppUsageEntity(
                        packageName = packageName,
                        appName = appName,
                        foregroundTime = resumeTime,
                        backgroundTime = null,
                        durationMs = now - resumeTime,
                        date = dateStr,
                        isSystemApp = isSystem
                    )
                )
            }
        }

        return result.sortedByDescending { it.foregroundTime }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.split(".").lastOrNull()
                ?.replaceFirstChar { it.uppercase() } ?: packageName
        }
    }
}
