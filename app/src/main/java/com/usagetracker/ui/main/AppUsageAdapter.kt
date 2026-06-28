package com.usagetracker.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.databinding.ItemAppUsageBinding
import com.usagetracker.util.FormatUtil

class AppUsageAdapter : ListAdapter<AppUsageEntity, AppUsageAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppUsageEntity) {
            binding.tvAppName.text = item.appName
            binding.tvPackageName.text = item.packageName
            binding.tvForegroundTime.text = "시작: ${FormatUtil.formatTime(item.foregroundTime)}"
            binding.tvBackgroundTime.text = if (item.backgroundTime != null) {
                "종료: ${FormatUtil.formatTime(item.backgroundTime)}"
            } else {
                "종료: 사용중"
            }
            binding.tvDuration.text = FormatUtil.formatDuration(item.durationMs)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppUsageEntity>() {
            override fun areItemsTheSame(old: AppUsageEntity, new: AppUsageEntity) =
                old.id == new.id
            override fun areContentsTheSame(old: AppUsageEntity, new: AppUsageEntity) =
                old == new
        }
    }
}
