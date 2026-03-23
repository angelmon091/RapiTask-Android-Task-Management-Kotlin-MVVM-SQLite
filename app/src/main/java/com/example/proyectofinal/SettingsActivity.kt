package com.example.proyectofinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupListeners()
        updateUI()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, RemindersActivity::class.java))
        }

        binding.btnTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.btnDateFormat.setOnClickListener {
            showDateFormatDialog()
        }

        binding.btnRateApp.setOnClickListener {
            showToast("Gracias por tu interés en calificar la app")
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "¡Mira esta app de notas que estoy usando!")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir vía"))
        }

        binding.btnPrivacy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            startActivity(browserIntent)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Español", "English")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Idioma")
            .setItems(languages) { _, which ->
                val selected = languages[which]
                binding.btnLanguage.text = "Idioma: $selected"
                showToast("Idioma cambiado a $selected")
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Sistema", "Claro", "Oscuro")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Tema")
            .setItems(themes) { _, which ->
                when (which) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                binding.btnTheme.text = "Tema de la Aplicación: ${themes[which]}"
            }
            .show()
    }

    private fun showDateFormatDialog() {
        val formats = arrayOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")
        AlertDialog.Builder(this)
            .setTitle("Formato de Fecha")
            .setItems(formats) { _, which ->
                binding.btnDateFormat.text = "Formato de Fecha: ${formats[which]}"
                showToast("Formato actualizado")
            }
            .show()
    }

    private fun updateUI() {
        // Podrías cargar preferencias guardadas aquí
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
