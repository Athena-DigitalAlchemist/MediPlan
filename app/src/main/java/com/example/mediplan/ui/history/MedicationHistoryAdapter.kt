package com.example.mediplan.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.R
import com.example.mediplan.data.MedicationStatus
import com.example.mediplan.databinding.ItemMedicationHistoryBinding
import java.time.format.DateTimeFormatter

class MedicationHistoryAdapter : ListAdapter<MedicationHistoryItem, MedicationHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationHistoryBinding.inflate(
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
        private val binding: ItemMedicationHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(item: MedicationHistoryItem) {
            binding.apply {
                medicationName.text = item.medicationName
                medicationDetails.text = item.amount

                val scheduledTimeText = item.scheduledTime.format(timeFormatter)
                timestamp.text = scheduledTimeText

                when (item.status) {
                    MedicationStatus.TAKEN -> {
                        actionIcon.setImageResource(R.drawable.ic_check)
                        actionIcon.setColorFilter(root.context.getColor(R.color.compliance_good))
                    }
                    MedicationStatus.MISSED -> {
                        actionIcon.setImageResource(R.drawable.ic_missed)
                        actionIcon.setColorFilter(root.context.getColor(R.color.compliance_poor))
                    }
                    MedicationStatus.SKIPPED -> {
                        actionIcon.setImageResource(R.drawable.ic_skip)
                        actionIcon.setColorFilter(root.context.getColor(R.color.compliance_medium))
                    }
                    MedicationStatus.SCHEDULED -> {
                        actionIcon.setImageResource(R.drawable.ic_time)
                        actionIcon.setColorFilter(root.context.getColor(R.color.primary))
                    }
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MedicationHistoryItem>() {
        override fun areItemsTheSame(oldItem: MedicationHistoryItem, newItem: MedicationHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MedicationHistoryItem, newItem: MedicationHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

data class MedicationHistoryItem(
    val id: Long,
    val medicationName: String,
    val amount: Double,
    val scheduledTime: LocalDateTime,
    val status: MedicationStatus
) 