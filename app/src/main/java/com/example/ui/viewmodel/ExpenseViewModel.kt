package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeFilter(val displayName: String) {
    TODAY("Hôm nay"),
    THIS_WEEK("Tuần này"),
    THIS_MONTH("Tháng này"),
    SPECIFIC_DAY("Theo Ngày"),
    SPECIFIC_MONTH("Theo Tháng"),
    SPECIFIC_YEAR("Theo Năm"),
    ALL("Tất cả")
}

data class CategoryExpenseSummary(
    val categoryName: String,
    val iconKey: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float
)

data class BudgetStatus(
    val categoryName: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val percentage: Float
)

data class PeriodBreakdownItem(
    val title: String,
    val subTitle: String,
    val dateMillis: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val transactions: List<Transaction>
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    val selectedTimeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
    val selectedDayMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1..12
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val searchQuery = MutableStateFlow("")

    val expenseCategories: StateFlow<List<Category>>
    val incomeCategories: StateFlow<List<Category>>

    val filteredTransactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>
    val categoryBreakdown: StateFlow<List<CategoryExpenseSummary>>
    val periodBreakdown: StateFlow<List<PeriodBreakdownItem>>
    val monthlyBudgets: StateFlow<List<BudgetStatus>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(
            transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(),
            budgetDao = db.budgetDao()
        )

        expenseCategories = repository.getCategoriesByType(TransactionType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Category.DEFAULT_EXPENSE_CATEGORIES)

        incomeCategories = repository.getCategoriesByType(TransactionType.INCOME)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Category.DEFAULT_INCOME_CATEGORIES)

        // Combine all transactions with time filter, date/month/year & search query
        filteredTransactions = combine(
            repository.allTransactions,
            selectedTimeFilter,
            selectedDayMillis,
            selectedMonth,
            selectedYear
        ) { allList, timeFilter, dayMillis, month, year ->
            val range = when (timeFilter) {
                TimeFilter.TODAY -> DateUtils.getStartAndEndOfToday()
                TimeFilter.THIS_WEEK -> DateUtils.getStartAndEndOfWeek()
                TimeFilter.THIS_MONTH -> DateUtils.getStartAndEndOfMonth(0)
                TimeFilter.SPECIFIC_DAY -> DateUtils.getStartAndEndOfDay(dayMillis)
                TimeFilter.SPECIFIC_MONTH -> DateUtils.getStartAndEndOfSpecificMonth(month, year)
                TimeFilter.SPECIFIC_YEAR -> DateUtils.getStartAndEndOfYear(year)
                TimeFilter.ALL -> Pair(0L, Long.MAX_VALUE)
            }
            allList.filter { it.dateMillis in range.first..range.second }
        }.combine(searchQuery) { list, query ->
            if (query.isNotBlank()) {
                val q = query.lowercase().trim()
                list.filter {
                    it.title.lowercase().contains(q) ||
                    it.note.lowercase().contains(q) ||
                    it.categoryName.lowercase().contains(q)
                }
            } else {
                list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Calculate Totals
        totalIncome = filteredTransactions.combine(selectedTimeFilter) { list, _ ->
            list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalExpense = filteredTransactions.combine(selectedTimeFilter) { list, _ ->
            list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Calculate Category Breakdown for Expenses
        categoryBreakdown = combine(filteredTransactions, totalExpense) { list, totalExp ->
            if (totalExp <= 0.0) {
                emptyList()
            } else {
                val expenses = list.filter { it.type == TransactionType.EXPENSE }
                val grouped = expenses.groupBy { it.categoryName }

                grouped.map { (catName, items) ->
                    val sum = items.sumOf { it.amount }
                    val icon = items.firstOrNull()?.categoryIcon ?: "category"
                    val color = items.firstOrNull()?.categoryColorHex ?: "#00897B"
                    CategoryExpenseSummary(
                        categoryName = catName,
                        iconKey = icon,
                        colorHex = color,
                        totalAmount = sum,
                        percentage = (sum / totalExp * 100).toFloat()
                    )
                }.sortedByDescending { it.totalAmount }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Calculate Period Breakdown (by Day or by Month)
        periodBreakdown = combine(
            filteredTransactions,
            selectedTimeFilter
        ) { list, timeFilter ->
            if (list.isEmpty()) {
                emptyList()
            } else {
                when (timeFilter) {
                    TimeFilter.SPECIFIC_YEAR, TimeFilter.ALL -> {
                        // Group by Month (MM/yyyy)
                        val grouped = list.groupBy { DateUtils.formatMonthYear(it.dateMillis) }
                        grouped.map { (monthYearStr, items) ->
                            val inc = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                            val exp = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                            val maxMillis = items.maxOf { it.dateMillis }
                            PeriodBreakdownItem(
                                title = "Tháng $monthYearStr",
                                subTitle = "${items.size} giao dịch",
                                dateMillis = maxMillis,
                                totalIncome = inc,
                                totalExpense = exp,
                                transactions = items.sortedByDescending { it.dateMillis }
                            )
                        }.sortedByDescending { it.dateMillis }
                    }
                    else -> {
                        // Group by Day (dd/MM/yyyy)
                        val grouped = list.groupBy { DateUtils.formatDate(it.dateMillis) }
                        grouped.map { (dateStr, items) ->
                            val inc = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                            val exp = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                            val firstMillis = items.first().dateMillis
                            PeriodBreakdownItem(
                                title = DateUtils.getRelativeDateString(firstMillis),
                                subTitle = "${items.size} giao dịch",
                                dateMillis = firstMillis,
                                totalIncome = inc,
                                totalExpense = exp,
                                transactions = items.sortedByDescending { it.dateMillis }
                            )
                        }.sortedByDescending { it.dateMillis }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Calculate Monthly Budget Statuses
        val currentMonthYear = DateUtils.formatMonthYear(System.currentTimeMillis())
        monthlyBudgets = combine(
            repository.getBudgetsForMonth(currentMonthYear),
            repository.allTransactions
        ) { budgetList, allTrans ->
            val monthRange = DateUtils.getStartAndEndOfMonth()
            val startOfMonth = monthRange.first
            val endOfMonth = monthRange.second
            val monthExpenses = allTrans.filter {
                it.type == TransactionType.EXPENSE && it.dateMillis in startOfMonth..endOfMonth
            }

            budgetList
                .groupBy { it.categoryName }
                .map { (_, items) -> items.maxByOrNull { it.id } ?: items.first() }
                .map { budget ->
                    val spent = if (budget.categoryName == "ALL") {
                        monthExpenses.sumOf { it.amount }
                    } else {
                        monthExpenses.filter { it.categoryName == budget.categoryName }.sumOf { it.amount }
                    }

                    val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
                    BudgetStatus(
                        categoryName = budget.categoryName,
                        limitAmount = budget.monthlyLimit,
                        spentAmount = spent,
                        percentage = pct.coerceAtMost(2.0f)
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryName: String,
        categoryIcon: String,
        categoryColorHex: String,
        note: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                type = type,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColorHex = categoryColorHex,
                note = note,
                dateMillis = dateMillis
            )
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setBudget(
        categoryName: String,
        limitAmount: Double,
        iconKey: String = "account_balance_wallet",
        colorHex: String = "#00897B"
    ) {
        viewModelScope.launch {
            val monthYear = DateUtils.formatMonthYear(System.currentTimeMillis())
            val existing = repository.getBudgetByCategoryAndMonth(categoryName, monthYear)
            val budget = Budget(
                id = existing?.id ?: 0,
                categoryName = categoryName,
                monthlyLimit = limitAmount,
                monthYear = monthYear
            )
            repository.saveBudget(budget)

            // Ensure category exists or update icon/color in categories table if it's not "ALL"
            if (categoryName != "ALL") {
                val currentExpenseCategories = expenseCategories.value
                val existingCategory = currentExpenseCategories.find { it.name.equals(categoryName, ignoreCase = true) }
                if (existingCategory == null) {
                    val newCat = Category(
                        name = categoryName,
                        type = TransactionType.EXPENSE,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isDefault = false
                    )
                    repository.addCategory(newCat)
                } else {
                    val updatedCat = existingCategory.copy(iconKey = iconKey, colorHex = colorHex)
                    repository.addCategory(updatedCat)
                }
            }
        }
    }

    fun deleteBudget(categoryName: String, deleteCustomCategoryAlso: Boolean = true) {
        viewModelScope.launch {
            val monthYear = DateUtils.formatMonthYear(System.currentTimeMillis())
            repository.deleteBudgetByCategoryAndMonth(categoryName, monthYear)
            if (deleteCustomCategoryAlso && categoryName != "ALL") {
                repository.deleteCustomCategoryByName(categoryName)
            }
        }
    }

    fun addCustomCategory(name: String, type: TransactionType, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                type = type,
                iconKey = iconKey,
                colorHex = colorHex,
                isDefault = false
            )
            repository.addCategory(category)
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        selectedTimeFilter.value = filter
    }

    fun setSelectedDay(millis: Long) {
        selectedDayMillis.value = millis
        selectedTimeFilter.value = TimeFilter.SPECIFIC_DAY
    }

    fun navigateDay(deltaDays: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDayMillis.value
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        selectedDayMillis.value = cal.timeInMillis
        selectedTimeFilter.value = TimeFilter.SPECIFIC_DAY
    }

    fun setSelectedMonthYear(month: Int, year: Int) {
        selectedMonth.value = month
        selectedYear.value = year
        selectedTimeFilter.value = TimeFilter.SPECIFIC_MONTH
    }

    fun navigateMonth(deltaMonths: Int) {
        var m = selectedMonth.value + deltaMonths
        var y = selectedYear.value
        while (m > 12) {
            m -= 12
            y += 1
        }
        while (m < 1) {
            m += 12
            y -= 1
        }
        selectedMonth.value = m
        selectedYear.value = y
        selectedTimeFilter.value = TimeFilter.SPECIFIC_MONTH
    }

    fun setSelectedYear(year: Int) {
        selectedYear.value = year
        selectedTimeFilter.value = TimeFilter.SPECIFIC_YEAR
    }

    fun navigateYear(deltaYears: Int) {
        selectedYear.value += deltaYears
        selectedTimeFilter.value = TimeFilter.SPECIFIC_YEAR
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
}
