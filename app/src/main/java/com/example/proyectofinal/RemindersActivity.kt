package com.example.proyectofinal

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.databinding.ActivityRemindersBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding
    private val reminderViewModel: ReminderViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter
    
    private var selectedDate = Calendar.getInstance()
    private var selectedTime = Calendar.getInstance()
    private var currentReminders: List<Reminder> = emptyList()
    private var allNotes: List<Note> = emptyList()
    private var showCompleted = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones permitidas", Toast.LENGTH_SHORT).show()
            checkExactAlarmPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupTabs()
        observeData()
        
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkExactAlarmPermission()
            }
        } else {
            checkExactAlarmPermission()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Permiso de Alarma Exacta")
                    .setMessage("Android requiere un permiso especial para que los recordatorios suenen al segundo exacto.")
                    .setPositiveButton("Permitir") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .show()
            }
        }
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fabAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReminderAdapter(
            onToggleComplete = { reminder ->
                reminderViewModel.updateCompletionStatus(reminder.id, !reminder.isCompleted)
            },
            onDelete = { reminder ->
                showDeleteConfirmation(reminder)
            },
            onClick = { reminder -> }
        )
        binding.rvReminders.layoutManager = LinearLayoutManager(this)
        binding.rvReminders.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showCompleted = tab?.position == 1
                filterReminders()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeData() {
        reminderViewModel.allReminders.observe(this) { reminders ->
            currentReminders = reminders
            filterReminders()
        }
        noteViewModel.allNotes.observe(this) { notes ->
            allNotes = notes
        }
    }

    private fun filterReminders() {
        val filteredList = currentReminders.filter { it.isCompleted == showCompleted }
        adapter.submitList(filteredList)
        
        if (filteredList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvReminders.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvReminders.visibility = View.VISIBLE
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val actvTaskSelector = dialogView.findViewById<AutoCompleteTextView>(R.id.actvTaskSelector)
        val actvRepeat = dialogView.findViewById<AutoCompleteTextView>(R.id.actvRepeat)
        val btnDate = dialogView.findViewById<Button>(R.id.btnPickDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnPickTime)
        val cbHighPriority = dialogView.findViewById<MaterialCheckBox>(R.id.cbHighPriority)
        
        val taskTitles = allNotes.map { it.title }
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, taskTitles)
        actvTaskSelector.setAdapter(arrayAdapter)
        
        val repeatOptions = arrayOf("No repetir", "Diariamente", "Semanalmente", "Mensualmente")
        val repeatAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, repeatOptions)
        actvRepeat.setAdapter(repeatAdapter)
        
        selectedDate = Calendar.getInstance()
        selectedTime = Calendar.getInstance()
        
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        btnDate.text = getString(R.string.today)
        btnTime.text = sdfTime.format(selectedTime.time)

        btnDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                btnDate.text = sdfDate.format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                btnTime.text = sdfTime.format(selectedTime.time)
            }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), true).show()
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_new_reminder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_add)) { _, _ ->
                val selectedTaskTitle = actvTaskSelector.text.toString().trim()
                val selectedRepeat = actvRepeat.text.toString()
                
                if (selectedTaskTitle.isNotEmpty()) {
                    val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dbTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    val repeatType = when (selectedRepeat) {
                        "Diariamente" -> "Daily"
                        "Semanalmente" -> "Weekly"
                        "Mensualmente" -> "Monthly"
                        else -> "None"
                    }

                    val reminder = Reminder(
                        title = selectedTaskTitle,
                        dueDate = dbDateFormat.format(selectedDate.time),
                        dueTime = dbTimeFormat.format(selectedTime.time),
                        priority = if (cbHighPriority.isChecked) 1 else 0,
                        category = allNotes.find { it.title == selectedTaskTitle }?.category ?: "General",
                        repeatType = repeatType
                    )
                    reminderViewModel.insert(reminder)
                } else {
                    Toast.makeText(this, "Por favor selecciona una tarea", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showDeleteConfirmation(reminder: Reminder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_reminder))
            .setMessage(getString(R.string.msg_delete_reminder_confirm))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                reminderViewModel.delete(reminder)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
