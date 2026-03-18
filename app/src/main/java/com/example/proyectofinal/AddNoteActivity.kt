package com.example.proyectofinal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.proyectofinal.databinding.ActivityAddNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla para añadir o editar una tarea.
 */
class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setCurrentDate()
    }

    private fun setupUI() {
        // Manejo de Insets para respetar las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            // La Toolbar superior maneja el padding del status bar
            binding.toolbar.updatePadding(top = systemBars.top)
            insets
        }

        // Botón para regresar a la pantalla principal
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Botón para guardar la tarea
        binding.btnSave.setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val date = binding.tvDate.text.toString()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val note = Note(
            title = if (title.isEmpty()) "Sin título" else title,
            content = content,
            date = date
        )

        viewModel.insert(note)
        Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * Establece la fecha actual automáticamente en el formato solicitado.
     */
    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM 'a las' HH:mm", Locale("es", "ES"))
        val currentDate = sdf.format(Date())
        // Capitalizar la primera letra
        binding.tvDate.text = currentDate.replaceFirstChar { it.uppercase() }
    }
}
