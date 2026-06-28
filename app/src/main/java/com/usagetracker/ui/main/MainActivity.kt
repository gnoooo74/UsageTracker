package com.usagetracker.ui.main

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import com.usagetracker.R
import com.usagetracker.databinding.ActivityMainBinding
import com.usagetracker.util.FormatUtil
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var appUsageAdapter: AppUsageAdapter
    private lateinit var browserHistoryAdapter: BrowserHistoryAdapter

    private val displayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        checkPermissions()
        setupRecyclerViews()
        setupTabs()
        setupObservers()
        setupListeners()

        // 최초 실행 시 동기화
        viewModel.sync()
    }

    private fun checkPermissions() {
        if (!viewModel.hasUsagePermission) {
            Toast.makeText(this, "앱 사용 통계 권한이 필요합니다", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun setupRecyclerViews() {
        appUsageAdapter = AppUsageAdapter()
        binding.recyclerView.apply {
            adapter = appUsageAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        browserHistoryAdapter = BrowserHistoryAdapter()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("앱 사용"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("브라우저"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> binding.recyclerView.adapter = appUsageAdapter
                    1 -> binding.recyclerView.adapter = browserHistoryAdapter
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupObservers() {
        viewModel.selectedDate.observe(this) { dateStr ->
            val date = storageDateFormat.parse(dateStr) ?: Date()
            binding.toolbar.subtitle = displayDateFormat.format(date)
        }

        viewModel.appUsageList.observe(this) { list ->
            appUsageAdapter.submitList(list)
            updateAppSummary(list)
        }

        viewModel.browserHistoryList.observe(this) { list ->
            browserHistoryAdapter.submitList(list)
            binding.tvBrowserCount.text = "브라우저 기록: ${list.size}건"
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.syncMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAppSummary(list: List<com.usagetracker.data.model.AppUsageEntity>) {
        val totalMs = list.sumOf { it.durationMs }
        val appCount = list.map { it.packageName }.distinct().size
        binding.tvSummary.text = "총 ${appCount}개 앱 · ${FormatUtil.formatDuration(totalMs)}"
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sync()
        }

        binding.fabToday.setOnClickListener {
            viewModel.setToday()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_date_picker -> {
                showDatePicker()
                true
            }
            R.id.action_permission -> {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            viewModel.setDate(storageDateFormat.format(cal.time))
            viewModel.sync()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}
