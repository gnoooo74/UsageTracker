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

    val hasUsagePermission: Boolean get() = repository.hasUsagePermission()

    val appUsageList: LiveData<List<AppUsageEntity>> = _selectedDate.switchMap { date ->
        repository.getAppUsageByDate(date)
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncAppUsage()
                _syncMessage.value = "동기화 완료"
            } catch (e: Exception) {
                _syncMessage.value = "동기화 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
