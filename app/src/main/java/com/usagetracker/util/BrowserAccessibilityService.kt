package com.usagetracker.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.usagetracker.data.model.BrowserHistoryEntity
import com.usagetracker.data.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AccessibilityService로 브라우저 URL 실시간 수집
 * - 크롬, 삼성인터넷, 네이버 앱 등 URL바가 있는 모든 브라우저 대응
 * - 권한: 접근성 서비스 1회 허용
 */
class BrowserAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 마지막으로 저장한 URL (연속 중복 방지)
    private var lastUrl: String = ""
    private var lastSavedTime: Long = 0L

    // 지원 브라우저 패키지명
    companion object {
        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.sec.android.app.sbrowser",       // 삼성 인터넷
            "com.sec.android.app.sbrowser.beta",
            "org.mozilla.firefox",
            "com.microsoft.emmx",                  // Edge
            "com.opera.browser",
            "com.brave.browser",
            "com.naver.whale",                     // 네이버 웨일
        )

        // URL바 리소스 ID (브라우저별)
        val URL_BAR_IDS = setOf(
            "com.android.chrome:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/sb_url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.microsoft.emmx:id/url_bar",
            "com.naver.whale:id/url_bar",
        )

        // 브라우저 이름 매핑
        fun getBrowserName(packageName: String): String = when (packageName) {
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev" -> "chrome"
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta" -> "samsung"
            "org.mozilla.firefox" -> "firefox"
            "com.microsoft.emmx" -> "edge"
            "com.naver.whale" -> "whale"
            else -> "browser"
        }
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = BROWSER_PACKAGES.toTypedArray()
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 500
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        if (!BROWSER_PACKAGES.contains(packageName)) return

        val url = extractUrl(event, packageName) ?: return

        // http/https URL만 수집
        if (!url.startsWith("http://") && !url.startsWith("https://")) return

        val now = System.currentTimeMillis()

        // 동일 URL 3초 이내 중복 저장 방지
        if (url == lastUrl && now - lastSavedTime < 3000) return

        lastUrl = url
        lastSavedTime = now

        saveUrl(url, packageName, now)
    }

    /**
     * 이벤트에서 URL 추출
     * 1순위: URL바 리소스 ID로 직접 찾기
     * 2순위: 전체 노드 트리 탐색
     */
    private fun extractUrl(event: AccessibilityEvent, packageName: String): String? {
        val rootNode = rootInActiveWindow ?: return null

        // 1순위: 알려진 URL바 ID로 찾기
        for (id in URL_BAR_IDS) {
            if (!id.startsWith(packageName)) continue
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNullOrEmpty()) continue
            val text = nodes[0].text?.toString()
            if (!text.isNullOrBlank()) return text
        }

        // 2순위: 텍스트 내용이 URL처럼 생긴 노드 탐색
        return findUrlInNode(rootNode)
    }

    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        val text = node.text?.toString()
        if (!text.isNullOrBlank() && isLikelyUrl(text)) {
            return text
        }

        for (i in 0 until node.childCount) {
            val result = findUrlInNode(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun isLikelyUrl(text: String): Boolean {
        return (text.startsWith("http://") || text.startsWith("https://")) &&
                text.contains(".") &&
                !text.contains("\n")
    }

    private fun saveUrl(url: String, packageName: String, timestamp: Long) {
        scope.launch {
            try {
                val repository = TrackerRepository(applicationContext)
                val entity = BrowserHistoryEntity(
                    url = url,
                    title = extractDomain(url),  // 제목은 도메인으로 임시 저장
                    visitTime = timestamp,
                    visitCount = 1,
                    browserSource = getBrowserName(packageName),
                    date = dateFormat.format(Date(timestamp))
                )
                repository.insertBrowserHistory(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val withoutProtocol = url.removePrefix("https://").removePrefix("http://")
            withoutProtocol.substringBefore("/").substringBefore("?")
        } catch (e: Exception) {
            url
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[SupervisorJob()]?.cancel()
    }
}
