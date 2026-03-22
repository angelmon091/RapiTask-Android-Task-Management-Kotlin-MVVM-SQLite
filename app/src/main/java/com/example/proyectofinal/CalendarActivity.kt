package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.databinding.ActivityCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    private var allNotes: List<Note> = emptyList()
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeNotes()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            binding.appBarLayout.updatePadding(top = systemBars.top)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdf.format(calendar.time)
            filterNotesByDate()
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(this, AddNoteActivity::class.java).apply {
                    putExtra("NOTE_ID", note.id)
                }
                startActivity(intent)
            },
            onItemLongClick = { _, _ -> }
        )
        binding.rvDayNotes.layoutManager = LinearLayoutManager(this)
        binding.rvDayNotes.adapter = adapter
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { notes ->
            allNotes = notes
            filterNotesByDate()
        }
    }

    private fun filterNotesByDate() {
        val filtered = allNotes.filter { note ->
            note.endDate == selectedDate || (note.endDate == null && isSameDay(note.date, selectedDate))
        }
        adapter.submitList(filtered)
    }

    private fun isSameDay(noteDateStr: String, selectedDateStr: String): Boolean {
        // La fecha de la nota viene en formato "Lunes, 22 de Mayo a las 12:30" o similar
        // Esto es un poco frágil, lo ideal sería guardar la fecha en un formato estandarizado en la DB.
        // Por ahora, intentaremos un match parcial o simplificado.
        return false // Por ahora priorizamos endDate que es el campo nuevo exacto
    }
}
