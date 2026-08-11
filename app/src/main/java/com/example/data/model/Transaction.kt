package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE, // Chi tiêu
    INCOME   // Thu nhập
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double, // Giá trị tiền (VND)
    val type: TransactionType,
    val categoryName: String,
    val categoryIcon: String, // Key string for icon mapping
    val categoryColorHex: String, // Color hex string, e.g. "#FB8C00"
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)
