package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.SummaryCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun OverviewScreen(
    viewModel: ExpenseViewModel,
    onOpenAddTransaction: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val filter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val categorySummaries by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val budgets by viewModel.monthlyBudgets.collectAsStateWithLifecycle()

    val overallBudget = budgets.firstOrNull { it.categoryName == "ALL" }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddTransaction,
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch") },
                text = { Text("Thêm Chi Thu", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_transaction_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary Card (Wallet balance, Income, Expense, Filter tabs)
            item {
                SummaryCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    selectedFilter = filter,
                    onFilterSelected = { viewModel.setTimeFilter(it) }
                )
            }

            // Bank Notification Capture & Simulator Card
            item {
                com.example.ui.components.BankNotificationCard()
            }

            // Monthly Budget Banner (if configured)
            if (overallBudget != null) {
                item {
                    BudgetProgressCard(
                        budgetStatus = overallBudget,
                        onEditBudget = { onNavigateToBudget() }
                    )
                }
            }

            // Category Expense Breakdown Chart Preview
            item {
                ExpenseDonutChart(
                    categorySummaries = categorySummaries,
                    totalExpense = totalExpense
                )
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giao Dịch Gần Đây",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text("Xem tất cả", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Recent Transactions List
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.height(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có giao dịch nào trong khoảng thời gian này",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = transactions.take(6),
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
