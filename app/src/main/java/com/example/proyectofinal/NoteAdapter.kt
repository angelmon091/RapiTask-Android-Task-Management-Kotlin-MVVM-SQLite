package com.example.proyectofinal

import android.content.res.ColorStateList
import android.graphics.Paint
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

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
            
            // Render HTML content for the description
            val contentHtml = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(note.content, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(note.content)
            }
            binding.textViewDescription.text = contentHtml

            binding.textViewDate.text = note.date
            
            if (note.isCompleted) {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.viewStatusIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.status_completed)
                )
            } else {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                val statusColor = getStatusColor(note.endDate)
                binding.viewStatusIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, statusColor)
                )
            }
            
            binding.ivPinned.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            binding.ivLocked.visibility = if (note.isLocked) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener { onItemClick(note) }
            binding.root.setOnLongClickListener {
                onItemLongClick(note, it)
                true
            }
        }

        private fun getStatusColor(endDate: String?): Int {
            if (endDate == null) return R.color.status_on_time
            
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val end = sdf.parse(endDate) ?: return R.color.status_on_time
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val diff = end.time - today.time
                val daysDiff = diff / (1000 * 60 * 60 * 24)

                when {
                    end.before(today) -> R.color.status_overdue
                    daysDiff <= 2 -> R.color.status_soon
                    else -> R.color.status_on_time
                }
            } catch (e: Exception) {
                R.color.status_on_time
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}
