package com.example.proyectofinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository
    val allReminders: LiveData<List<Reminder>>

    init {
        val reminderDao = AppDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(reminderDao)
        allReminders = repository.allReminders.asLiveData()
    }

    fun insert(reminder: Reminder) = viewModelScope.launch {
        val id = repository.insert(reminder)
        val newReminder = reminder.copy(id = id.toInt())
        // Usamos el Scheduler profesional
        ReminderScheduler.schedule(getApplication(), newReminder)
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        ReminderScheduler.schedule(getApplication(), reminder)
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        repository.delete(reminder)
        ReminderScheduler.cancel(getApplication(), reminder.id)
    }

    fun updateCompletionStatus(id: Int, completed: Boolean) = viewModelScope.launch {
        repository.updateCompletionStatus(id, completed)
        if (completed) {
            ReminderScheduler.cancel(getApplication(), id)
        } else {
            // Si se desmarca como completado, se vuelve a programar
            allReminders.value?.find { it.id == id }?.let {
                ReminderScheduler.schedule(getApplication(), it)
            }
        }
    }
}

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders = reminderDao.getAllReminders()
    suspend fun insert(reminder: Reminder): Long = reminderDao.insert(reminder)
    suspend fun update(reminder: Reminder) = reminderDao.update(reminder)
    suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
    suspend fun updateCompletionStatus(id: Int, completed: Boolean) = reminderDao.updateCompletionStatus(id, completed)
}
