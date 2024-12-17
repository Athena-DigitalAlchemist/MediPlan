package com.example.mediplan.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mediplan.databinding.ItemMedicationTimeBinding

class MedicationTimesAdapter(
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<String, MedicationTimesAdapter.ViewHolder>(TimesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationTimeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMedicationTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(time: String) {
            binding.timeText.text = time
            binding.deleteButton.setOnClickListener {
                onDeleteClick(time)
            }
        }
    }
}

private class TimesDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
} 