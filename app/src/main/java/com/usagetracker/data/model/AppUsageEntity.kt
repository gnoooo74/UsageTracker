package com.usagetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 앱 사용 기록 엔티티
 * UsageStatsManager로 수집된 데이터 저장
 */
@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 패키지명 (com.samsung.android.browser 등) */
    val packageName: String,

    /** 앱 표시 이름 */
    val appName: String,

    /** 포그라운드 진입 시각 (Unix timestamp ms) */
    val foregroundTime: Long,

    /** 백그라운드/종료 시각 (Unix timestamp ms), null = 아직 사용중 */
    val backgroundTime: Long? = null,

    /** 총 사용 시간 (ms), backgroundTime - foregroundTime */
    val durationMs: Long = 0,

    /** 수집 날짜 (yyyy-MM-dd) */
    val date: String
)
