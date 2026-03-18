package com.example.proyectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemNoteBinding

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note, android.view.View) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(note: Note) {
            binding.textViewTitle.text = note.title
            binding.textViewDescription.text = note.content
            binding.textViewDate.text = note.date
            
            // Mostrar u ocultar iconos de estado
            binding.ivPinned.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.ivLocked.visibility = if (note.isLocked) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener { onItemClick(note) }
            
            binding.root.setOnLongClickListener {
                onItemLongClick(note, it)
                true
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
