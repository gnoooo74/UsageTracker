package com.usagetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.usagetracker.data.model.AppUsageEntity

@Dao
interface AppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AppUsageEntity>)

    /** 특정 날짜의 앱 사용 기록 (최근 사용순) */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY foregroundTime DESC")
    fun getByDate(date: String): LiveData<List<AppUsageEntity>>

    /** 특정 날짜 기록 (동기) */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY foregroundTime ASC")
    suspend fun getByDateSync(date: String): List<AppUsageEntity>

    /** 특정 앱의 전체 기록 */
    @Query("SELECT * FROM app_usage WHERE packageName = :packageName ORDER BY foregroundTime DESC")
    fun getByPackage(packageName: String): LiveData<List<AppUsageEntity>>

    /** 날짜별 총 사용시간 집계 */
    @Query("SELECT date, SUM(durationMs) as total FROM app_usage GROUP BY date ORDER BY date DESC")
    fun getDailySummary(): LiveData<List<DailySummary>>

    /** 앱별 총 사용시간 (특정 날짜) */
    @Query("""
        SELECT packageName, appName, SUM(durationMs) as totalDuration 
        FROM app_usage 
        WHERE date = :date 
        GROUP BY packageName 
        ORDER BY totalDuration DESC
    """)
    fun getAppSummaryByDate(date: String): LiveData<List<AppSummary>>

    @Delete
    suspend fun delete(entity: AppUsageEntity)

    /** 특정 날짜 데이터 전체 삭제 (새로고침 시 오늘 데이터 교체용) */
    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)

    /** 30일 이전 데이터 자동 삭제 */
    @Query("DELETE FROM app_usage WHERE date < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)
}

data class DailySummary(val date: String, val total: Long)
data class AppSummary(val packageName: String, val appName: String, val totalDuration: Long)
