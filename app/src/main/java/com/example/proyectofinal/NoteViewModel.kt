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

    fun insert(note: Note) = viewModelScope.launch {
        dao.insert(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        dao.update(note)
    }
}
