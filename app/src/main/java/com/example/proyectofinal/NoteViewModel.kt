package com.example.proyectofinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).noteDao()
    val allNotes: LiveData<List<Note>> = dao.getAllNotes().asLiveData()

    suspend fun insert(note: Note): Long {
        return dao.insert(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }

    suspend fun update(note: Note) {
        dao.update(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return dao.getNoteById(id)
    }
}
