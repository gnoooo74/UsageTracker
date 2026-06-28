package com.usagetracker.ui.main

import android.app.Application
import androidx.lifecycle.*
import com.usagetracker.data.model.AppUsageEntity
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

    // 필터 상태: true = 시스템 앱 제외, false = 전체 보기
    private val _filterSystem = MutableLiveData(true)
    val filterSystem: LiveData<Boolean> = _filterSystem

    val hasUsagePermission: Boolean get() = repository.hasUsagePermission()

    // 날짜 + 필터 상태 조합으로 LiveData 전환
    val appUsageList: LiveData<List<AppUsageEntity>> =
        MediatorLiveData<List<AppUsageEntity>>().also { mediator ->
            var currentDate = _selectedDate.value ?: dateFormat.format(Date())
            var currentFilter = _filterSystem.value ?: true
            var currentSource: LiveData<List<AppUsageEntity>>? = null

            fun resubscribe() {
                currentSource?.let { mediator.removeSource(it) }
                val newSource = repository.getAppUsageByDate(currentDate, currentFilter)
                mediator.addSource(newSource) { mediator.value = it }
                currentSource = newSource
            }

            mediator.addSource(_selectedDate) { date ->
                currentDate = date
                resubscribe()
            }
            mediator.addSource(_filterSystem) { filter ->
                currentFilter = filter
                resubscribe()
            }

            resubscribe()
        }

    fun setDate(date: String) {
        _selectedDate.value = date
        sync()
    }

    fun setToday() {
        _selectedDate.value = dateFormat.format(Date())
        sync()
    }

    fun toggleFilter() {
        _filterSystem.value = !(_filterSystem.value ?: true)
    }

    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncAppUsage(_selectedDate.value)
                _syncMessage.value = "동기화 완료"
            } catch (e: Exception) {
                _syncMessage.value = "동기화 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
