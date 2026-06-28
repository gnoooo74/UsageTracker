package com.usagetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val foregroundTime: Long,
    val backgroundTime: Long? = null,
    val durationMs: Long = 0,
    val date: String,
    /** 시스템/런처 앱 여부 (필터링 기준) */
    val isSystemApp: Boolean = false
)
