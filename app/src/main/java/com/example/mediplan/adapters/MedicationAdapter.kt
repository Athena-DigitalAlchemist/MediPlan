package com.example.mediplan.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.data.Medication
import com.example.mediplan.data.FrequencyType
import com.example.mediplan.databinding.ItemMedicationBinding

class MedicationAdapter : ListAdapter<Medication, MedicationAdapter.MedicationViewHolder>(MedicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicationViewHolder(
        private val binding: ItemMedicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medication: Medication) {
            binding.apply {
                // Set medication name
                textMedicationName.text = medication.name

                // Set medication details (dosage)
                textMedicationDetails.text = buildString {
                    append(medication.amount)
                    append(" ")
                    append(medication.unit.getDisplayName(medication.amount))
                }

                // Set schedule information
                textMedicationSchedule.text = when (medication.frequency) {
                    FrequencyType.EVERY_X_HOURS -> "Every ${medication.intervalHours} hours"
                    FrequencyType.SPECIFIC_TIMES -> "At ${medication.startTime}"
                    FrequencyType.DAILY -> "Daily at ${medication.startTime}"
                    FrequencyType.WEEKLY -> {
                        val days = medication.selectedDays.joinToString(", ") { it.name }
                        "Weekly on $days at ${medication.startTime}"
                    }
                    FrequencyType.MONTHLY -> "Monthly on day ${medication.startDate.split("-")[2]} at ${medication.startTime}"
                }

                // Set additional info
                val additionalInfo = mutableListOf<String>()
                if (medication.withFood) {
                    additionalInfo.add("Take with food")
                }
                if (medication.notes.isNotBlank()) {
                    additionalInfo.add(medication.notes)
                }

                if (additionalInfo.isNotEmpty()) {
                    textMedicationNotes.text = additionalInfo.joinToString("\n")
                    notesContainer.visibility = View.VISIBLE
                } else {
                    notesContainer.visibility = View.GONE
                }

                // Handle switch state
                switchTaken.isChecked = false // You might want to add this state to your Medication class
                switchTaken.setOnCheckedChangeListener { _, isChecked ->
                    // Handle medication taken status
                    // You might want to add a callback here to update the database
                }
            }
        }
    }
}

class MedicationDiffCallback : DiffUtil.ItemCallback<Medication>() {
    override fun areItemsTheSame(oldItem: Medication, newItem: Medication): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Medication, newItem: Medication): Boolean {
        return oldItem == newItem
    }
}
