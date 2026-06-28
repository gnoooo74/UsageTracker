package com.usagetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 브라우저 방문기록 엔티티
 * ContentProvider로 크롬/삼성인터넷 히스토리 수집
 */
@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 방문한 URL */
    val url: String,

    /** 페이지 제목 */
    val title: String,

    /** 방문 시각 (Unix timestamp ms) */
    val visitTime: Long,

    /** 방문 횟수 */
    val visitCount: Int = 1,

    /** 브라우저 출처 (chrome / samsung / unknown) */
    val browserSource: String,

    /** 수집 날짜 (yyyy-MM-dd) */
    val date: String
)
