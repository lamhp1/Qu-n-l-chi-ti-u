package com.example.data.repository

import com.example.data.dao.BudgetDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getTransactionsInRange(startTimeMillis: Long, endTimeMillis: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(startTimeMillis, endTimeMillis)
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query)
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
    }

    suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCustomCategoryByName(name: String) {
        categoryDao.deleteCustomCategoryByName(name)
    }

    suspend fun getBudgetByCategoryAndMonth(categoryName: String, monthYear: String): Budget? {
        return budgetDao.getBudgetByCategoryAndMonth(categoryName, monthYear)
    }

    suspend fun saveBudget(budget: Budget) {
        budgetDao.deleteBudgetsByCategoryAndMonth(budget.categoryName, budget.monthYear)
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun deleteBudgetByCategoryAndMonth(categoryName: String, monthYear: String) {
        budgetDao.deleteBudgetsByCategoryAndMonth(categoryName, monthYear)
    }
}
