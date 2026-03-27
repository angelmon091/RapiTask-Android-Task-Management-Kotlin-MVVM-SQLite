package com.example.proyectofinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateText: String,
    val category: String = "Todas",
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val endDate: String? = null,
    val endTime: String? = null,
    val isCompleted: Boolean = false,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val filePath: String? = null,
    val fontSize: Float = 16f,
    val fontType: String? = null
)
