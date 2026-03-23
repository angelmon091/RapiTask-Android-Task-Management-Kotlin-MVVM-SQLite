package com.example.proyectofinal

import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemSubtaskBinding

class SubtaskAdapter(
    private val onTitleChanged: (Int, String) -> Unit,
    private val onCheckedStateChanged: (Int, Boolean) -> Unit,
    private val onDeleteClicked: (Int) -> Unit
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

        private var textWatcher: TextWatcher? = null

        fun bind(subtask: Subtask) {
            // Remove previous listeners to avoid loops/leaks
            binding.etSubtaskTitle.removeTextChangedListener(textWatcher)
            binding.cbSubtask.setOnCheckedChangeListener(null)
            
            binding.etSubtaskTitle.setText(subtask.title)
            binding.cbSubtask.isChecked = subtask.isCompleted
            
            updateTextStrikeThrough(subtask.isCompleted)

            binding.cbSubtask.setOnCheckedChangeListener { _, isChecked ->
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onCheckedStateChanged(pos, isChecked)
                    updateTextStrikeThrough(isChecked)
                }
            }

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onTitleChanged(pos, s.toString())
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            binding.etSubtaskTitle.addTextChangedListener(textWatcher)

            binding.btnDeleteSubtask.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClicked(pos)
                }
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
        override fun areItemsTheSame(oldItem: Subtask, newItem: Subtask) = 
            if (oldItem.id != 0 && newItem.id != 0) oldItem.id == newItem.id 
            else oldItem === newItem

        override fun areContentsTheSame(oldItem: Subtask, newItem: Subtask) = oldItem == newItem
    }
}
