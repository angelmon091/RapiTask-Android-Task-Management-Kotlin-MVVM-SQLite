package com.example.proyectofinal

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.databinding.ActivityAddNoteBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private val viewModel: NoteViewModel by viewModels()
    private var existingNote: Note? = null
    private var category: String = "Todas"
    private var endDate: String? = null
    
    private lateinit var subtaskAdapter: SubtaskAdapter
    private val subtasks = mutableListOf<Subtask>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSubtasks()
        
        category = intent.getStringExtra("CATEGORY_EXTRA") ?: "Todas"
        
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        if (noteId != -1) {
            loadExistingNote(noteId)
        } else {
            setCurrentDate()
            setupCategoryDropdown()
        }
    }

    private fun setupSubtasks() {
        subtaskAdapter = SubtaskAdapter(
            onSubtaskChanged = { updatedSubtask ->
                val index = subtasks.indexOfFirst { it === updatedSubtask || (it.id != 0 && it.id == updatedSubtask.id) }
                if (index != -1) {
                    subtasks[index] = updatedSubtask
                }
            },
            onDeleteSubtask = { subtask ->
                subtasks.remove(subtask)
                subtaskAdapter.submitList(subtasks.toList())
            }
        )
        binding.rvSubtasks.layoutManager = LinearLayoutManager(this)
        binding.rvSubtasks.adapter = subtaskAdapter
    }

    private fun loadExistingNote(id: Int) {
        lifecycleScope.launch {
            existingNote = viewModel.getNoteById(id)
            existingNote?.let { note ->
                binding.etTitle.setText(note.title)
                binding.etContent.setText(note.content)
                binding.tvDate.text = "Creada el ${note.date}"
                binding.swCompleted.isChecked = note.isCompleted
                category = note.category
                endDate = note.endDate
                
                if (endDate != null) {
                    binding.chipEndDate.text = "Vence: $endDate"
                }

                setupCategoryDropdown()
                
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                val loadedSubtasks = db.subtaskDao().getSubtasksByNoteId(note.id).first()
                subtasks.clear()
                subtasks.addAll(loadedSubtasks)
                subtaskAdapter.submitList(subtasks.toList())
            }
        }
    }

    private fun setupCategoryDropdown() {
        val sharedPref = getSharedPreferences("TaskEzzPrefs", MODE_PRIVATE)
        val saved = sharedPref.getStringSet("DYNAMIC_CATEGORIES", setOf("Escuela", "Trabajo"))
        val categories = mutableListOf("Todas")
        categories.addAll(saved ?: emptySet())
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setText(category, false)
        
        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            category = adapter.getItem(position) ?: "Todas"
        }
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = 0)
            binding.toolbar.updatePadding(top = systemBars.top)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveNote() }

        binding.chipEndDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnAddSubtask.setOnClickListener {
            val newSubtask = Subtask(noteId = existingNote?.id ?: 0, title = "")
            subtasks.add(newSubtask)
            subtaskAdapter.submitList(subtasks.toList())
        }
        
        binding.btnTextFormat.setOnClickListener { showToast("Formato de texto próximamente") }
        binding.btnAudio.setOnClickListener { showToast("Grabación de audio próximamente") }
        binding.btnImage.setOnClickListener { showToast("Agregar imagen próximamente") }
        binding.btnFile.setOnClickListener { showToast("Adjuntar archivos próximamente") }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            endDate = sdf.format(selectedDate.time)
            binding.chipEndDate.text = "Vence: $endDate"
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveNote() {
        val titleInput = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val isCompleted = binding.swCompleted.isChecked

        if (titleInput.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val finalTitle = if (titleInput.isEmpty()) "Sin título" else titleInput

        lifecycleScope.launch {
            val noteToSave = if (existingNote != null) {
                existingNote!!.copy(
                    title = finalTitle,
                    content = content,
                    category = category,
                    endDate = endDate,
                    isCompleted = isCompleted
                )
            } else {
                val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                Note(
                    title = finalTitle,
                    content = content,
                    date = sdf.format(Date()),
                    category = category,
                    endDate = endDate,
                    isCompleted = isCompleted
                )
            }

            val noteId = if (existingNote != null) {
                viewModel.update(noteToSave)
                existingNote!!.id
            } else {
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                db.noteDao().insert(noteToSave).toInt()
            }
            
            if (noteId != 0) {
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                db.subtaskDao().deleteSubtasksByNoteId(noteId)
                subtasks.forEach { sub ->
                    if (sub.title.isNotEmpty()) {
                        db.subtaskDao().insert(sub.copy(noteId = noteId, id = 0))
                    }
                }
            }

            Toast.makeText(this@AddNoteActivity, "Tarea guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        binding.tvDate.text = "Creada el ${sdf.format(Date())}"
    }
    
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
