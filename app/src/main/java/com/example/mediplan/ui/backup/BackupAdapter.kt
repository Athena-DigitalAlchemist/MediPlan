package com.example.mediplan.ui.backup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.R
import com.example.mediplan.databinding.ItemBackupBinding
import com.example.mediplan.utils.BackupFile
import java.time.format.DateTimeFormatter

class BackupAdapter(
    private val onRestoreClick: (BackupFile) -> Unit,
    private val onDeleteClick: (BackupFile) -> Unit,
    private val onShareClick: (BackupFile) -> Unit
) : ListAdapter<BackupFile, BackupAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBackupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemBackupBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        init {
            binding.menuButton.setOnClickListener {
                showPopupMenu(it, getItem(adapterPosition))
            }
        }

        fun bind(backup: BackupFile) {
            binding.apply {
                dateTextView.text = backup.date.format(dateFormatter)
                versionTextView.text = "v${backup.version}"
                sizeTextView.text = formatFileSize(backup.size)
            }
        }

        private fun showPopupMenu(view: View, backup: BackupFile) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.backup_item_menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_restore -> {
                            onRestoreClick(backup)
                            true
                        }
                        R.id.action_share -> {
                            onShareClick(backup)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(backup)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BackupFile>() {
        override fun areItemsTheSame(oldItem: BackupFile, newItem: BackupFile): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: BackupFile, newItem: BackupFile): Boolean {
            return oldItem == newItem
        }
    }
} 