package com.usagetracker.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.usagetracker.data.model.BrowserHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

class BrowserHistoryCollector(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 크롬 + 삼성인터넷 히스토리 모두 수집
     */
    fun collectAll(startTime: Long, endTime: Long): List<BrowserHistoryEntity> {
        val result = mutableListOf<BrowserHistoryEntity>()
        result.addAll(collectChrome(startTime, endTime))
        result.addAll(collectSamsungInternet(startTime, endTime))
        return result.sortedByDescending { it.visitTime }
    }

    /**
     * 오늘 하루 수집
     */
    fun collectToday(): List<BrowserHistoryEntity> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return collectAll(cal.timeInMillis, System.currentTimeMillis())
    }

    /**
     * 크롬 히스토리 수집
     * URI: content://com.android.chrome.browser/bookmarks
     *
     * ※ Android 6+ 에서는 크롬이 ContentProvider를 비공개로 변경함
     *    → 이 방법은 일부 기기/버전에서만 동작, 실패 시 빈 리스트 반환
     */
    private fun collectChrome(startTime: Long, endTime: Long): List<BrowserHistoryEntity> {
        val chromeUris = listOf(
            "content://com.android.chrome.browser/bookmarks",
            "content://com.chrome.browser/bookmarks",
            "content://com.android.browser/bookmarks"
        )

        for (uriStr in chromeUris) {
            try {
                val result = queryBrowserHistory(
                    uri = Uri.parse(uriStr),
                    startTime = startTime,
                    endTime = endTime,
                    source = "chrome"
                )
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                // 접근 불가 → 다음 URI 시도
            }
        }
        return emptyList()
    }

    /**
     * 삼성 인터넷 히스토리 수집
     * URI: content://com.sec.android.app.sbrowser/bookmarks
     */
    private fun collectSamsungInternet(startTime: Long, endTime: Long): List<BrowserHistoryEntity> {
        val samsungUris = listOf(
            "content://com.sec.android.app.sbrowser/bookmarks",
            "content://com.sec.android.app.sbrowser.browser/bookmarks"
        )

        for (uriStr in samsungUris) {
            try {
                val result = queryBrowserHistory(
                    uri = Uri.parse(uriStr),
                    startTime = startTime,
                    endTime = endTime,
                    source = "samsung"
                )
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                // 접근 불가 → 다음 URI 시도
            }
        }
        return emptyList()
    }

    /**
     * ContentProvider 공통 쿼리
     */
    private fun queryBrowserHistory(
        uri: Uri,
        startTime: Long,
        endTime: Long,
        source: String
    ): List<BrowserHistoryEntity> {
        val result = mutableListOf<BrowserHistoryEntity>()
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                uri,
                arrayOf("url", "title", "date", "visits"),
                "date >= ? AND date <= ? AND bookmark = 0",
                arrayOf(startTime.toString(), endTime.toString()),
                "date DESC"
            )

            cursor?.use { c ->
                val urlIdx = c.getColumnIndex("url")
                val titleIdx = c.getColumnIndex("title")
                val dateIdx = c.getColumnIndex("date")
                val visitsIdx = c.getColumnIndex("visits")

                while (c.moveToNext()) {
                    val url = if (urlIdx >= 0) c.getString(urlIdx) ?: continue else continue
                    val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "" else ""
                    val visitTime = if (dateIdx >= 0) c.getLong(dateIdx) else continue
                    val visitCount = if (visitsIdx >= 0) c.getInt(visitsIdx) else 1

                    // javascript: 및 빈 URL 제외
                    if (url.startsWith("javascript:") || url.isBlank()) continue

                    result.add(
                        BrowserHistoryEntity(
                            url = url,
                            title = title,
                            visitTime = visitTime,
                            visitCount = visitCount,
                            browserSource = source,
                            date = dateFormat.format(Date(visitTime))
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // 권한 없음 - 정상적으로 무시
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return result
    }
}
