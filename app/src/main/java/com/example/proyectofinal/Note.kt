package com.example.proyectofinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val date: String,
    val category: String = "Todas",
    val isFavorite: Boolean = false
)
