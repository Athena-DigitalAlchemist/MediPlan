package com.example.mediplan.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.data.Medication
import com.example.mediplan.databinding.ItemMedicationScheduleBinding

class MedicationScheduleAdapter(
    private val onMedicationAction: (Medication, MedicationAction) -> Unit
) : ListAdapter<MedicationScheduleItem, MedicationScheduleAdapter.ViewHolder>(MedicationScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMedicationScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MedicationScheduleItem) {
            binding.apply {
                medicationName.text = item.medication.name
                medicationTime.text = item.scheduledTime
                
                actionButton.apply {
                    text = when (item.status) {
                        MedicationStatus.TAKEN -> "Taken"
                        MedicationStatus.PENDING -> "Take"
                        MedicationStatus.SNOOZED -> "Snooze"
                    }
                    
                    setOnClickListener {
                        val action = when (item.status) {
                            MedicationStatus.TAKEN -> MedicationAction.UNDO
                            MedicationStatus.PENDING -> MedicationAction.TAKE
                            MedicationStatus.SNOOZED -> MedicationAction.SNOOZE
                        }
                        onMedicationAction(item.medication, action)
                    }
                }
            }
        }
    }
}

data class MedicationScheduleItem(
    val medication: Medication,
    val scheduledTime: String,
    val status: MedicationStatus
)

enum class MedicationStatus {
    TAKEN, PENDING, SNOOZED
}

enum class MedicationAction {
    TAKE, SNOOZE, UNDO
}

private class MedicationScheduleDiffCallback : DiffUtil.ItemCallback<MedicationScheduleItem>() {
    override fun areItemsTheSame(oldItem: MedicationScheduleItem, newItem: MedicationScheduleItem): Boolean {
        return oldItem.medication.id == newItem.medication.id
    }

    override fun areContentsTheSame(oldItem: MedicationScheduleItem, newItem: MedicationScheduleItem): Boolean {
        return oldItem == newItem
    }
} 