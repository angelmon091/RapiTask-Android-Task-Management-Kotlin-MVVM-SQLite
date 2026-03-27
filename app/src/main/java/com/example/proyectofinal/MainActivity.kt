package com.example.proyectofinal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var keepSplashScreen = true
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    private var currentFilter = "Todas"
    private var lastNotesList: List<Note> = emptyList()
    private var currentSearchQuery = ""
    private var isGridView = false
    private val dynamicCategories = mutableListOf<String>()
    private var currentSortOrder = "date"
    private var showFavoritesOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        if (savedInstanceState != null) {
            keepSplashScreen = false
        } else {
            splashScreen.setKeepOnScreenCondition { keepSplashScreen }
            Handler(Looper.getMainLooper()).postDelayed({ keepSplashScreen = false }, 2000)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCategories()
        setupUI()
        setupRecyclerView()
        setupNavigation()
        setupBackNavigation()
        setupFilters()

        observeNotesOnce()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mainContent.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.navView.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java).apply {
                putExtra("CATEGORY_EXTRA", currentFilter)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note ->
                if (note.isLocked) {
                    showToast("Esta tarea está bloqueada.")
                } else {
                    val intent = Intent(this, AddNoteActivity::class.java).apply {
                        putExtra("NOTE_ID", note.id)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { note, view -> showTaskOptions(note, view) }
        )
        updateRecyclerViewLayout()
        binding.recyclerView.adapter = adapter
    }

    private fun updateRecyclerViewLayout() {
        binding.recyclerView.layoutManager = if (isGridView) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(this)
        }
    }

    private fun setupFilters() {
        binding.chipGroup.removeAllViews()
        addChipToGroup("Todas", isDefault = true)
        dynamicCategories.forEach { addChipToGroup(it, isDefault = false) }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                if (currentFilter != "Todas") {
                    findViewById<Chip>(R.id.chipTodas)?.isChecked = true
                    currentFilter = "Todas"
                    showFavoritesOnly = false
                    applyCurrentFilter()
                }
            } else {
                val checkedId = checkedIds.first()
                val checkedChip = findViewById<Chip>(checkedId)
                if (checkedChip != null) {
                    val newFilter = checkedChip.text.toString()
                    if (currentFilter != newFilter) {
                        currentFilter = newFilter
                        showFavoritesOnly = false
                        applyCurrentFilter()
                    }
                }
            }
        }
    }

    private fun addChipToGroup(name: String, isDefault: Boolean) {
        val chipContext = ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
        val chip = Chip(chipContext)
        chip.text = name
        chip.isCheckable = true
        chip.isClickable = true
        chip.setChipBackgroundColorResource(R.color.chip_background_selector)
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
        chip.isCheckedIconVisible = false
        
        when (name) {
            "Todas" -> { chip.id = R.id.chipTodas; chip.isChecked = (currentFilter == "Todas") }
            "Escuela" -> { chip.id = R.id.chipEscuela; chip.isChecked = (currentFilter == "Escuela") }
            "Trabajo" -> { chip.id = R.id.chipTrabajo; chip.isChecked = (currentFilter == "Trabajo") }
            else -> {
                chip.id = View.generateViewId()
                chip.isChecked = (currentFilter == name)
            }
        }

        if (!isDefault) {
            chip.setOnLongClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Eliminar sección")
                    .setMessage("¿Eliminar '${chip.text}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        dynamicCategories.remove(chip.text.toString())
                        saveCategories()
                        binding.chipGroup.removeView(chip)
                        updateDrawerMenu()
                        if (currentFilter == chip.text.toString()) {
                            currentFilter = "Todas"
                            findViewById<Chip>(R.id.chipTodas)?.isChecked = true
                            applyCurrentFilter()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }
        }
        binding.chipGroup.addView(chip)
    }

    private fun saveCategories() {
        getSharedPreferences("TaskEzzPrefs", MODE_PRIVATE).edit {
            putStringSet("DYNAMIC_CATEGORIES", dynamicCategories.toSet())
        }
    }

    private fun loadCategories() {
        val sharedPref = getSharedPreferences("TaskEzzPrefs", MODE_PRIVATE)
        val saved = sharedPref.getStringSet("DYNAMIC_CATEGORIES", setOf("Escuela", "Trabajo"))
        dynamicCategories.clear()
        dynamicCategories.addAll(saved ?: emptySet())
    }

    private fun observeNotesOnce() {
        viewModel.allNotes.observe(this) { notes ->
            lastNotesList = notes
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        var filteredNotes = when {
            showFavoritesOnly -> lastNotesList.filter { it.isFavorite }
            currentFilter == "Todas" -> lastNotesList.toList()
            else -> lastNotesList.filter { it.category == currentFilter }
        }
        
        if (currentSearchQuery.isNotEmpty()) {
            filteredNotes = filteredNotes.filter { 
                it.title.contains(currentSearchQuery, ignoreCase = true) || 
                it.content.contains(currentSearchQuery, ignoreCase = true) 
            }
        }

        filteredNotes = if (currentSortOrder == "alpha") {
            filteredNotes.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.title.lowercase() })
        } else {
            filteredNotes.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.date })
        }
        adapter.submitList(filteredNotes)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyCurrentFilter()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_more) {
            showMainMenuOptions(findViewById(R.id.action_more))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showMainMenuOptions(view: View) {
        val popup = PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenuStyle), view)
        val menu = popup.menu
        menu.add(Menu.NONE, 1, 1, if (isGridView) "Vista Listada" else "Vista Cuadrícula")
        menu.add(Menu.NONE, 2, 2, if (currentSortOrder == "date") "Ordenar A-Z" else "Ordenar por fecha")
        menu.add(Menu.NONE, 3, 3, "Agregar Sección")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> { isGridView = !isGridView; updateRecyclerViewLayout(); true }
                2 -> { currentSortOrder = if (currentSortOrder == "date") "alpha" else "date"; applyCurrentFilter(); true }
                3 -> { showAddSectionDialog(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAddSectionDialog() {
        val input = EditText(this)
        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva sección")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && !dynamicCategories.contains(name)) {
                    dynamicCategories.add(name)
                    saveCategories()
                    addChipToGroup(name, false)
                    updateDrawerMenu()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTaskOptions(note: Note, view: View) {
        val popup = PopupMenu(ContextThemeWrapper(this, R.style.CustomPopupMenuStyle), view)
        popup.menuInflater.inflate(R.menu.task_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_pin -> { 
                    lifecycleScope.launch { viewModel.update(note.copy(isPinned = !note.isPinned)) }
                    true 
                }
                R.id.action_lock -> { 
                    lifecycleScope.launch { viewModel.update(note.copy(isLocked = !note.isLocked)) }
                    true 
                }
                R.id.action_duplicate -> { duplicateTask(note); true }
                R.id.action_delete -> { viewModel.delete(note); true }
                R.id.action_favorite -> { 
                    lifecycleScope.launch { viewModel.update(note.copy(isFavorite = !note.isFavorite)) }
                    true 
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun duplicateTask(note: Note) {
        lifecycleScope.launch {
            val duplicatedNote = note.copy(id = 0, title = "${note.title} (copia)")
            val db = AppDatabase.getDatabase(this@MainActivity)
            val newNoteId = db.noteDao().insert(duplicatedNote).toInt()
            
            // Duplicar subtareas si existen
            val subtasks = db.subtaskDao().getSubtasksByNoteId(note.id).first()
            subtasks.forEach { subtask ->
                db.subtaskDao().insert(subtask.copy(id = 0, noteId = newNoteId))
            }
            showToast("Tarea duplicada")
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this)
        updateDrawerMenu()
    }

    private fun updateDrawerMenu() {
        val menu = binding.navView.menu
        val categoriesItem = menu.findItem(R.id.group_categories) ?: return
        val subMenu = categoriesItem.subMenu ?: return
        
        subMenu.clear()
        
        // Add "Todas" with assignment icon
        subMenu.add(R.id.group_categories, R.id.nav_todas, 0, "Todas")
            .setIcon(R.drawable.ic_assignment)
            
        // Add dynamic categories with the list icon (ic_list) for all
        dynamicCategories.forEach { category ->
            subMenu.add(R.id.group_categories, Menu.NONE, Menu.NONE, category)
                .setIcon(R.drawable.ic_list)
        }
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var categoryToSelect: String? = null

        when (item.itemId) {
            R.id.nav_inicio, R.id.nav_todas -> categoryToSelect = "Todas"
            R.id.nav_calendario -> startActivity(Intent(this, CalendarActivity::class.java))
            R.id.nav_recordatorio -> startActivity(Intent(this, RemindersActivity::class.java))
            R.id.nav_favoritos -> {
                showFavoritesOnly = true
                currentFilter = "Todas"
                updateChipSelection("Todas")
                applyCurrentFilter()
            }
            R.id.nav_ajustes -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> {
                // Check if it's a dynamic category by title
                val title = item.title.toString()
                if (dynamicCategories.contains(title) || title == "Todas") {
                    categoryToSelect = title
                }
            }
        }

        categoryToSelect?.let {
            showFavoritesOnly = false
            currentFilter = it
            updateChipSelection(it)
            applyCurrentFilter()
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun updateChipSelection(categoryName: String) {
        for (i in 0 until binding.chipGroup.childCount) {
            val chip = binding.chipGroup.getChildAt(i) as? Chip
            if (chip?.text == categoryName) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
