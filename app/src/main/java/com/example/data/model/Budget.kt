package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String, // e.g., "Ăn uống" or "ALL" for total budget
    val monthlyLimit: Double,
    val monthYear: String // Format: "MM/yyyy" e.g., "08/2026"
)
