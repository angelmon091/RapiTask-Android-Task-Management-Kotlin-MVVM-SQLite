package com.example.proyectofinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            showToast(getString(R.string.thanks_for_interest))
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
        }

        binding.btnPrivacy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tu-usuario/rapitask-privacy-policy")) // URL de ejemplo funcional
            startActivity(browserIntent)
        }

        binding.btnClearData.setOnClickListener {
            showDeleteDataDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Español", "English")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_language)
            .setItems(languages) { _, which ->
                val selected = languages[which]
                binding.btnLanguage.text = getString(R.string.language_label, selected)
                showToast(getString(R.string.language_changed, selected))
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_theme)
            .setItems(themes) { _, which ->
                when (which) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                binding.btnTheme.text = getString(R.string.theme_label, themes[which])
            }
            .show()
    }

    private fun showDateFormatDialog() {
        val formats = arrayOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.date_format_title)
            .setItems(formats) { _, which ->
                binding.btnDateFormat.text = getString(R.string.date_format_label, formats[which])
                showToast(getString(R.string.format_updated))
            }
            .show()
    }

    private fun showDeleteDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_data_title)
            .setMessage(R.string.delete_all_data_confirm)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                clearAppData()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun clearAppData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(this@SettingsActivity)
                db.clearAllTables()
            }
            showToast(getString(R.string.data_cleared_success))
            // Opcional: Reiniciar la app o volver a inicio
        }
    }

    private fun updateUI() {
        // Cargar estados actuales (opcional)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
