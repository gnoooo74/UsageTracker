package com.usagetracker.ui.main

import android.app.Application
import androidx.lifecycle.*
import com.usagetracker.data.db.AppSummary
import com.usagetracker.data.db.DomainSummary
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.data.model.BrowserHistoryEntity
import com.usagetracker.data.repository.TrackerRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackerRepository(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _selectedDate = MutableLiveData(dateFormat.format(Date()))
    val selectedDate: LiveData<String> = _selectedDate

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _syncMessage = MutableLiveData<String>()
    val syncMessage: LiveData<String> = _syncMessage

    val hasUsagePermission: Boolean get() = repository.hasUsagePermission()
    val hasAccessibilityPermission: Boolean get() = repository.hasAccessibilityPermission()

    // 날짜 변경 시 자동으로 LiveData 전환
    val appUsageList: LiveData<List<AppUsageEntity>> = _selectedDate.switchMap { date ->
        repository.getAppUsageByDate(date)
    }

    val appSummaryList: LiveData<List<AppSummary>> = _selectedDate.switchMap { date ->
        repository.getAppSummaryByDate(date)
    }

    val browserHistoryList: LiveData<List<BrowserHistoryEntity>> = _selectedDate.switchMap { date ->
        repository.getBrowserHistoryByDate(date)
    }

    val topDomains: LiveData<List<DomainSummary>> = _selectedDate.switchMap { date ->
        repository.getTopDomainsByDate(date)
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    /**
     * 앱 사용 + 브라우저 히스토리 동기화
     */
    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncAppUsage()
                repository.cleanupOldBrowserHistory()
                _syncMessage.value = "동기화 완료"
            } catch (e: Exception) {
                _syncMessage.value = "동기화 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
