package com.example.mediplan.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.data.MedicationComplianceStats
import com.example.mediplan.databinding.ItemMedicationStatsBinding

class MedicationStatsAdapter : ListAdapter<MedicationComplianceStats, MedicationStatsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationStatsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMedicationStatsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stats: MedicationComplianceStats) {
            binding.apply {
                medicationNameTextView.text = stats.medicationName
                val complianceRate = if (stats.total > 0) {
                    (stats.taken.toFloat() / stats.total) * 100
                } else {
                    0f
                }
                complianceRateTextView.text = "${complianceRate.toInt()}%"
                complianceProgressBar.progress = complianceRate.toInt()
                dosesTextView.text = "${stats.taken}/${stats.total} δόσεις"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MedicationComplianceStats>() {
        override fun areItemsTheSame(
            oldItem: MedicationComplianceStats,
            newItem: MedicationComplianceStats
        ): Boolean {
            return oldItem.medicationName == newItem.medicationName
        }

        override fun areContentsTheSame(
            oldItem: MedicationComplianceStats,
            newItem: MedicationComplianceStats
        ): Boolean {
            return oldItem == newItem
        }
    }
} 