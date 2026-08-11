package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryName = :categoryName AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(categoryName: String, monthYear: String): Budget?

    @Query("DELETE FROM budgets WHERE categoryName = :categoryName AND monthYear = :monthYear")
    suspend fun deleteBudgetsByCategoryAndMonth(categoryName: String, monthYear: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
}
