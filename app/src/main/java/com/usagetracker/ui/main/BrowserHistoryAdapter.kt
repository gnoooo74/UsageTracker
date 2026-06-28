package com.usagetracker.ui.main

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.usagetracker.data.model.BrowserHistoryEntity
import com.usagetracker.databinding.ItemBrowserHistoryBinding
import com.usagetracker.util.FormatUtil

class BrowserHistoryAdapter : ListAdapter<BrowserHistoryEntity, BrowserHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemBrowserHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BrowserHistoryEntity) {
            binding.tvTitle.text = item.title.ifBlank { item.url }
            binding.tvUrl.text = item.url
            binding.tvVisitTime.text = FormatUtil.formatTime(item.visitTime)
            binding.tvSource.text = when (item.browserSource) {
                "chrome" -> "🔵 크롬"
                "samsung" -> "🟠 삼성인터넷"
                else -> "🌐 기타"
            }
            binding.tvVisitCount.text = "${item.visitCount}회"

            // URL 클릭 시 브라우저로 열기
            binding.root.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                    binding.root.context.startActivity(intent)
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBrowserHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BrowserHistoryEntity>() {
            override fun areItemsTheSame(old: BrowserHistoryEntity, new: BrowserHistoryEntity) =
                old.id == new.id
            override fun areContentsTheSame(old: BrowserHistoryEntity, new: BrowserHistoryEntity) =
                old == new
        }
    }
}
