package com.example.proyectofinal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteId: Int,
    val title: String,
    val isCompleted: Boolean = false
)
