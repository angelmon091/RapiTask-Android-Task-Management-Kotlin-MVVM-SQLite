package com.example.proyectofinal

import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.proyectofinal.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

/**
 * Pantalla principal de la aplicación TaskEzz.
 */
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
    private var currentSortOrder = "date" // "date" o "alpha"

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
                    showToast("Esta tarea está bloqueada. Quita el bloqueo para editar.")
                } else {
                    val intent = Intent(this, AddNoteActivity::class.java).apply {
                        putExtra("NOTE_ID", note.id)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { note, view ->
                showTaskOptions(note, view)
            }
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
        // Limpiar excepto el fijo "Todas"
        binding.chipGroup.removeAllViews()
        
        // Agregar "Todas" (única que no se puede eliminar)
        addChipToGroup("Todas", isDefault = true)
        
        // Agregar dinámicas cargadas (incluyendo Escuela y Trabajo si no han sido eliminadas)
        dynamicCategories.forEach { addChipToGroup(it, isDefault = false) }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                if (currentFilter != "Todas") {
                    findViewById<Chip>(R.id.chipTodas)?.isChecked = true
                    currentFilter = "Todas"
                    applyCurrentFilter()
                }
            } else {
                val checkedId = checkedIds.first()
                val checkedChip = findViewById<Chip>(checkedId)
                if (checkedChip != null) {
                    val newFilter = checkedChip.text.toString()
                    if (currentFilter != newFilter) {
                        currentFilter = newFilter
                        applyCurrentFilter()
                    }
                }
            }
        }
    }

    private fun addChipToGroup(name: String, isDefault: Boolean) {
        // Usamos ContextThemeWrapper para aplicar el estilo de Material3 Filter Chip
        val chipContext = ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
        val chip = Chip(chipContext)
        chip.text = name
        chip.isCheckable = true
        chip.isClickable = true
        
        // Estilos uniformes
        chip.setChipBackgroundColorResource(R.color.chip_background_selector)
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
        chip.isCloseIconVisible = false
        chip.isChipIconVisible = false
        chip.isCheckedIconVisible = false
        
        if (name == "Todas") {
            chip.id = R.id.chipTodas
            chip.isChecked = true
        } else if (name == "Escuela") {
            chip.id = R.id.chipEscuela
        } else if (name == "Trabajo") {
            chip.id = R.id.chipTrabajo
        } else {
            chip.id = View.generateViewId()
        }

        // Solo "Todas" es por defecto y no tiene long click
        if (!isDefault) {
            setupChipLongClick(chip)
        }

        binding.chipGroup.addView(chip)
    }

    private fun setupChipLongClick(chip: Chip) {
        chip.setOnLongClickListener {
            if (chip.text == "Todas") return@setOnLongClickListener false
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar sección")
                .setMessage("¿Estás seguro de que quieres eliminar la sección '${chip.text}'? Todas las notas dentro de esta sección también se borrarán.")
                .setPositiveButton("Eliminar") { _, _ ->
                    val categoryToDelete = chip.text.toString()
                    
                    // 1. Borrar notas de esta categoría en la base de datos
                    deleteNotesInCategory(categoryToDelete)
                    
                    // 2. Eliminar de la UI y persistencia
                    binding.chipGroup.removeView(chip)
                    dynamicCategories.remove(categoryToDelete)
                    saveCategories()
                    
                    if (currentFilter == categoryToDelete) {
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

    private fun deleteNotesInCategory(category: String) {
        // Borramos todas las notas que pertenecen a esta categoría
        lastNotesList.filter { it.category == category }.forEach { note ->
            viewModel.delete(note)
        }
    }

    private fun saveCategories() {
        val sharedPref = getSharedPreferences("TaskEzzPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putStringSet("DYNAMIC_CATEGORIES", dynamicCategories.toSet())
        editor.apply()
    }

    private fun loadCategories() {
        val sharedPref = getSharedPreferences("TaskEzzPrefs", Context.MODE_PRIVATE)
        
        // Si es la primera vez, inicializamos con Escuela y Trabajo
        if (!sharedPref.contains("CATEGORIES_INITIALIZED")) {
            val initial = setOf("Escuela", "Trabajo")
            sharedPref.edit()
                .putStringSet("DYNAMIC_CATEGORIES", initial)
                .putBoolean("CATEGORIES_INITIALIZED", true)
                .apply()
        }

        val saved = sharedPref.getStringSet("DYNAMIC_CATEGORIES", emptySet())
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
        var filteredNotes = if (currentFilter == "Todas") {
            lastNotesList.toList()
        } else {
            lastNotesList.filter { it.category == currentFilter }
        }
        
        if (currentSearchQuery.isNotEmpty()) {
            filteredNotes = filteredNotes.filter { 
                it.title.contains(currentSearchQuery, ignoreCase = true) || 
                it.content.contains(currentSearchQuery, ignoreCase = true) 
            }
        }

        filteredNotes = if (currentSortOrder == "alpha") {
            filteredNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenBy { it.title.lowercase() }
            )
        } else {
            filteredNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.date }
            )
        }
        
        adapter.submitList(filteredNotes) {
            if (filteredNotes.isNotEmpty()) {
                binding.recyclerView.post {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.apply {
            queryHint = "Buscar tareas..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    currentSearchQuery = newText ?: ""
                    applyCurrentFilter()
                    return true
                }
            })
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                showMainMenuOptions(findViewById(R.id.action_more))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showMainMenuOptions(view: View) {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, view)
        
        val menu = popup.menu
        
        val viewToggleItem = menu.add(Menu.NONE, 1, 1, if (isGridView) "Vista Listada" else "Vista Cuadrícula")
        viewToggleItem.icon = ContextCompat.getDrawable(this, if (isGridView) R.drawable.ic_view_list else R.drawable.ic_view_grid)
        
        val sortLabel = if (currentSortOrder == "date") "Ordenar A-Z" else "Ordenar por fecha"
        val sortItem = menu.add(Menu.NONE, 2, 2, sortLabel)
        sortItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_sort)
        
        val addSectionItem = menu.add(Menu.NONE, 3, 3, "Agregar Sección")
        addSectionItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_add_section)

        try {
            val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldPopup.isAccessible = true
            val menuPopupHelper = fieldPopup.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    isGridView = !isGridView
                    updateRecyclerViewLayout()
                    showToast(if (isGridView) "Vista de cuadrícula variable" else "Vista de lista")
                    true
                }
                2 -> {
                    currentSortOrder = if (currentSortOrder == "date") "alpha" else "date"
                    applyCurrentFilter()
                    showToast(if (currentSortOrder == "alpha") "Ordenado alfabéticamente" else "Ordenado por fecha")
                    true
                }
                3 -> {
                    showAddSectionDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAddSectionDialog() {
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = (24 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin, margin, margin)
        
        val input = EditText(this)
        input.layoutParams = params
        input.hint = "Nombre de la sección"
        
        input.setTextColor(ContextCompat.getColor(this, R.color.on_background))
        input.setHintTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        
        container.addView(input)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Nueva sección")
            .setView(container)
            .setPositiveButton("Agregar") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    addNewSection(name)
                } else {
                    showToast("El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val window = dialog.window
            window?.setBackgroundDrawableResource(R.drawable.bg_dialog_section)
            
            val blueColor = ContextCompat.getColor(this, R.color.primary)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(blueColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(blueColor)
        }
        
        dialog.show()
    }

    private fun addNewSection(name: String) {
        try {
            if (dynamicCategories.contains(name) || name == "Todas") {
                showToast("Esta sección ya existe")
                return
            }

            dynamicCategories.add(name)
            saveCategories()
            addChipToGroup(name, isDefault = false)
            
            showToast("Sección '$name' agregada")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error al agregar sección")
        }
    }

    private fun showTaskOptions(note: Note, view: View) {
        val wrapper = ContextThemeWrapper(this, R.style.CustomPopupMenuStyle)
        val popup = PopupMenu(wrapper, view)
        popup.menuInflater.inflate(R.menu.task_options_menu, popup.menu)

        val menu = popup.menu
        menu.findItem(R.id.action_pin).title = if (note.isPinned) "Quitar fijado" else "Fijar"
        menu.findItem(R.id.action_lock).title = if (note.isLocked) "Quitar bloqueo" else "Agregar bloqueo"

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_pin -> {
                    if (!note.isPinned) {
                        val pinnedCount = lastNotesList.count { it.isPinned }
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
                    if (!note.isLocked) {
                        val lockedCount = lastNotesList.count { it.isLocked }
                        if (lockedCount >= 5) {
                            showToast("Solo puedes bloquear un máximo de 5 tareas")
                            return@setOnMenuItemClickListener true
                        }
                    }
                    val updatedNote = note.copy(isLocked = !note.isLocked)
                    viewModel.update(updatedNote)
                    showToast(if (updatedNote.isLocked) "Bloqueo agregado" else "Bloqueo quitado")
                    true
                }
                R.id.action_duplicate -> {
                    val duplicateTask = note.copy(
                        id = 0, 
                        title = "${note.title} (Copia)",
                        isPinned = false,
                        isLocked = false
                    )
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        handleNavigationAction(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleNavigationAction(id: Int) {
        when (id) {
            R.id.nav_inicio -> {
                findViewById<Chip>(R.id.chipTodas)?.isChecked = true
                showToast(getString(R.string.toast_home_selected))
            }
            R.id.nav_escuela -> findViewById<Chip>(R.id.chipEscuela)?.isChecked = true
            R.id.nav_trabajo -> findViewById<Chip>(R.id.chipTrabajo)?.isChecked = true
            R.id.nav_todas -> findViewById<Chip>(R.id.chipTodas)?.isChecked = true
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
