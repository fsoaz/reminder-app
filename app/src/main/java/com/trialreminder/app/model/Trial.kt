package com.trialreminder.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trials")
data class Trial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val endDate: Long, // Timestamp in milliseconds
    val reminderTime: Long // Timestamp in milliseconds for when to show notification
)
