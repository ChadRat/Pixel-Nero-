package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // Format: "YYYY-MM-DD" for quick daily aggregates
    val beverageType: String = "Water",
    val waterEquivalency: Float = 1.0f,
    val waterEquivalentMl: Int = (amountMl * waterEquivalency).toInt()
)
