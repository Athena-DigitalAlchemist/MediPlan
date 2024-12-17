package com.example.mediplan.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.R
import com.example.mediplan.data.MedicationHistory
import com.example.mediplan.data.MedicationStatus
import com.example.mediplan.databinding.ItemMedicationHistoryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MedicationHistoryAdapter(
    private val onTakenClick: (Long) -> Unit,
    private val onSkippedClick: (Long) -> Unit
) : ListAdapter<MedicationHistory, MedicationHistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMedicationHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMedicationHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: MedicationHistory) {
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            val scheduledTime = LocalDateTime.parse(history.scheduledTime)
            
            binding.scheduledTime.text = "Scheduled: ${scheduledTime.format(timeFormatter)}"
            
            history.takenTime?.let { takenTime ->
                val taken = LocalDateTime.parse(takenTime)
                binding.takenTime.text = "Taken: ${taken.format(timeFormatter)}"
            }

            binding.statusChip.apply {
                when (history.status) {
                    MedicationStatus.TAKEN -> {
                        text = "Taken"
                        setChipBackgroundColorResource(R.color.chip_taken)
                    }
                    MedicationStatus.MISSED -> {
                        text = "Missed"
                        setChipBackgroundColorResource(R.color.chip_missed)
                    }
                    MedicationStatus.SKIPPED -> {
                        text = "Skipped"
                        setChipBackgroundColorResource(R.color.chip_skipped)
                    }
                    MedicationStatus.SCHEDULED -> {
                        text = "Take Now"
                        setChipBackgroundColorResource(R.color.chip_scheduled)
                        setOnClickListener {
                            showActionDialog(history.id)
                        }
                    }
                }
            }
        }

        private fun showActionDialog(historyId: Long) {
            // Εμφάνιση dialog με επιλογές "Taken" και "Skipped"
            val context = binding.root.context
            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle("Mark medication as")
                .setItems(arrayOf("Taken", "Skipped")) { _, which ->
                    when (which) {
                        0 -> onTakenClick(historyId)
                        1 -> onSkippedClick(historyId)
                    }
                }
                .create()
            dialog.show()
        }
    }
}

private class HistoryDiffCallback : DiffUtil.ItemCallback<MedicationHistory>() {
    override fun areItemsTheSame(oldItem: MedicationHistory, newItem: MedicationHistory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MedicationHistory, newItem: MedicationHistory): Boolean {
        return oldItem == newItem
    }
} 