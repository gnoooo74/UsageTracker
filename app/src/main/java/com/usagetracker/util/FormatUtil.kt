package com.usagetracker.util

import java.text.SimpleDateFormat
import java.util.*

object FormatUtil {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /**
     * Unix timestamp → HH:mm:ss
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * ms → "X시간 Y분 Z초" 형식
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0초"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}시간 ")
            if (minutes > 0) append("${minutes}분 ")
            if (seconds > 0 || (hours == 0L && minutes == 0L)) append("${seconds}초")
        }.trim()
    }

    /**
     * ms → "HH:mm:ss" 형식
     */
    fun formatDurationShort(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
