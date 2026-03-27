package com.example.proyectofinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: LiveData<List<Note>>

    init {
        val dao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(dao)
        allNotes = repository.allNotes.asLiveData()
    }

    suspend fun insert(note: Note): Long {
        return repository.insert(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }
}
