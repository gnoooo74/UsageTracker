package com.usagetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.usagetracker.data.model.BrowserHistoryEntity

@Dao
interface BrowserHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<BrowserHistoryEntity>)

    /** 특정 날짜의 브라우저 기록 (최신순) */
    @Query("SELECT * FROM browser_history WHERE date = :date ORDER BY visitTime DESC")
    fun getByDate(date: String): LiveData<List<BrowserHistoryEntity>>

    /** URL 중복 체크 */
    @Query("SELECT COUNT(*) FROM browser_history WHERE url = :url AND visitTime = :visitTime")
    suspend fun exists(url: String, visitTime: Long): Int

    /** 특정 날짜 + 브라우저별 기록 */
    @Query("""
        SELECT * FROM browser_history 
        WHERE date = :date AND browserSource = :source 
        ORDER BY visitTime DESC
    """)
    fun getByDateAndSource(date: String, source: String): LiveData<List<BrowserHistoryEntity>>

    /** 도메인별 방문 횟수 집계 */
    @Query("""
        SELECT 
            SUBSTR(url, INSTR(url, '://') + 3, 
                CASE WHEN INSTR(SUBSTR(url, INSTR(url, '://') + 3), '/') = 0 
                THEN LENGTH(url) 
                ELSE INSTR(SUBSTR(url, INSTR(url, '://') + 3), '/') - 1 END
            ) as domain,
            COUNT(*) as visitCount
        FROM browser_history 
        WHERE date = :date
        GROUP BY domain 
        ORDER BY visitCount DESC
        LIMIT 20
    """)
    fun getTopDomainsByDate(date: String): LiveData<List<DomainSummary>>

    /** 특정 날짜 데이터 전체 삭제 (새로고침 시 오늘 데이터 교체용) */
    @Query("DELETE FROM browser_history WHERE date = :date")
    suspend fun deleteByDate(date: String)

    /** 30일 이전 데이터 삭제 */
    @Query("DELETE FROM browser_history WHERE date < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)
}

data class DomainSummary(val domain: String, val visitCount: Int)
