package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

/**
 * Pantalla principal de la aplicación TaskEzz.
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var keepSplashScreen = true
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        if (savedInstanceState != null) {
            keepSplashScreen = false
        } else {
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
            Handler(Looper.getMainLooper()).postDelayed({
                keepSplashScreen = false
            }, 2000)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupNavigation()
        setupBackNavigation()
        observeNotes()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mainContent.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.navView.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note ->
                if (note.isLocked) {
                    showToast("Esta tarea está bloqueada. Quita el bloqueo para editar.")
                } else {
                    showToast("Abriendo tarea: ${note.title}")
                }
            },
            onItemLongClick = { note, view ->
                showTaskOptions(note, view)
            }
        )
        binding.recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = this@MainActivity.adapter
        }
    }

    private fun showTaskOptions(note: Note, view: View) {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, view)
        popup.menuInflater.inflate(R.menu.task_options_menu, popup.menu)

        val menu = popup.menu
        menu.findItem(R.id.action_pin).title = if (note.isPinned) "Quitar fijado" else "Fijar"
        menu.findItem(R.id.action_lock).title = if (note.isLocked) "Quitar bloqueo" else "Agregar bloqueo"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> {
                    if (!note.isPinned) {
                        // Intentando fijar: Verificar límite de 3
                        val pinnedCount = adapter.currentList.count { it.isPinned }
                        if (pinnedCount >= 3) {
                            showToast("Solo puedes fijar un máximo de 3 tareas")
                            return@setOnMenuItemClickListener true
                        }
                    }

                    val updatedNote = note.copy(isPinned = !note.isPinned)
                    viewModel.update(updatedNote)
                    showToast(if (updatedNote.isPinned) "Tarea fijada" else "Tarea desfijada")
                    true
                }
                R.id.action_lock -> {
                    val updatedNote = note.copy(isLocked = !note.isLocked)
                    viewModel.update(updatedNote)
                    showToast(if (updatedNote.isLocked) "Bloqueo agregado" else "Bloqueo quitado")
                    true
                }
                R.id.action_duplicate -> {
                    val duplicateTask = note.copy(id = 0, title = "${note.title} (Copia)")
                    viewModel.insert(duplicateTask)
                    showToast("Tarea duplicada")
                    true
                }
                R.id.action_delete -> {
                    viewModel.delete(note)
                    showToast("Tarea eliminada")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        handleNavigationAction(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleNavigationAction(id: Int) {
        when (id) {
            R.id.nav_inicio -> showToast(getString(R.string.toast_home_selected))
            else -> showToast("Opción seleccionada: ${getString(getMenuTitleRes(id))}")
        }
    }

    private fun getMenuTitleRes(id: Int): Int {
        return when (id) {
            R.id.nav_inicio -> R.string.menu_home
            R.id.nav_recordatorio -> R.string.menu_reminders
            R.id.nav_favoritos -> R.string.menu_favorites
            R.id.nav_todas -> R.string.menu_all
            R.id.nav_escuela -> R.string.menu_school
            R.id.nav_proyecto -> R.string.menu_project
            R.id.nav_trabajo -> R.string.menu_work
            R.id.nav_ajustes -> R.string.menu_settings
            else -> R.string.app_name
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
