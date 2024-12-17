package com.example.mediplan.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.data.Medication
import com.example.mediplan.databinding.ItemMedicationBinding
import com.google.android.material.chip.Chip
import java.time.format.DateTimeFormatter

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

    inner class MedicationViewHolder(
        private val binding: ItemMedicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        
        init {
            binding.root.setOnClickListener {
                val medication = getItem(adapterPosition)
                val context = binding.root.context
                val intent = Intent(context, EditMedicationActivity::class.java).apply {
                    putExtra(EditMedicationActivity.EXTRA_MEDICATION_ID, medication.id)
                }
                context.startActivity(intent)
            }
        }
        
        fun bind(medication: Medication) {
            binding.apply {
                medicationNameTextView.text = medication.name
                dosageTextView.text = "${medication.dosage} ${medication.unit.displayName}"
                
                // Εμφάνιση των ωρών λήψης
                daysChipGroup.removeAllViews()
                medication.times.sorted().forEach { time ->
                    val chip = Chip(root.context).apply {
                        text = time.format(timeFormatter)
                        isCheckable = false
                    }
                    daysChipGroup.addView(chip)
                }
            }
        }
    }

    private class MedicationDiffCallback : DiffUtil.ItemCallback<Medication>() {
        override fun areItemsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem == newItem
        }
    }
} 