package com.example.proyectofinal

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemSubtaskBinding

class SubtaskAdapter(
    private val onSubtaskChanged: (Subtask) -> Unit,
    private val onDeleteSubtask: (Subtask) -> Unit
) : ListAdapter<Subtask, SubtaskAdapter.SubtaskViewHolder>(SubtaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val binding = ItemSubtaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubtaskViewHolder(private val binding: ItemSubtaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subtask: Subtask) {
            binding.etSubtaskTitle.setText(subtask.title)
            binding.cbSubtask.isChecked = subtask.isCompleted
            
            updateTextStrikeThrough(subtask.isCompleted)

            binding.cbSubtask.setOnCheckedChangeListener { _, isChecked ->
                val updated = subtask.copy(isCompleted = isChecked)
                updateTextStrikeThrough(isChecked)
                onSubtaskChanged(updated)
            }

            binding.etSubtaskTitle.addTextChangedListener {
                val updated = subtask.copy(title = it.toString())
                onSubtaskChanged(updated)
            }

            binding.btnDeleteSubtask.setOnClickListener {
                onDeleteSubtask(subtask)
            }
        }

        private fun updateTextStrikeThrough(isCompleted: Boolean) {
            if (isCompleted) {
                binding.etSubtaskTitle.paintFlags = binding.etSubtaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.etSubtaskTitle.paintFlags = binding.etSubtaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    class SubtaskDiffCallback : DiffUtil.ItemCallback<Subtask>() {
        override fun areItemsTheSame(oldItem: Subtask, newItem: Subtask) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Subtask, newItem: Subtask) = oldItem == newItem
    }
}
