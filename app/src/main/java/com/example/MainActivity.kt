package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.example.service.BankNotificationManager
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.BankTransactionSyncDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.QuanLyChiTieuTheme
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

enum class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW("Tổng quan", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("Lịch sử", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    ANALYTICS("Thống kê", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    BUDGET("Ngân sách", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuanLyChiTieuTheme {
                var currentTab by remember { mutableStateOf(NavTab.OVERVIEW) }
                var isAddSheetOpen by remember { mutableStateOf(false) }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
                val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()

                val pendingBankTx by BankNotificationManager.pendingTransaction.collectAsStateWithLifecycle()

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldPrimary,
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                NavTab.OVERVIEW -> {
                                    OverviewScreen(
                                        viewModel = viewModel,
                                        onOpenAddTransaction = { isAddSheetOpen = true },
                                        onNavigateToHistory = { currentTab = NavTab.HISTORY },
                                        onNavigateToBudget = { currentTab = NavTab.BUDGET }
                                    )
                                }
                                NavTab.HISTORY -> {
                                    HistoryScreen(viewModel = viewModel)
                                }
                                NavTab.ANALYTICS -> {
                                    AnalyticsScreen(viewModel = viewModel)
                                }
                                NavTab.BUDGET -> {
                                    BudgetScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }

                    // Bottom Sheet for Adding Transaction
                    if (isAddSheetOpen) {
                        AddTransactionBottomSheet(
                            sheetState = sheetState,
                            expenseCategories = expenseCategories,
                            incomeCategories = incomeCategories,
                            onDismiss = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    isAddSheetOpen = false
                                }
                            },
                            onSave = { title, amount, type, categoryName, categoryIcon, categoryColorHex, note, dateMillis ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    categoryName = categoryName,
                                    categoryIcon = categoryIcon,
                                    categoryColorHex = categoryColorHex,
                                    note = note,
                                    dateMillis = dateMillis
                                )
                            }
                        )
                    }

                    // Bank Notification Capture Dialog
                    pendingBankTx?.let { bankTx ->
                        val context = LocalContext.current
                        BankTransactionSyncDialog(
                            transaction = bankTx,
                            expenseCategories = expenseCategories,
                            incomeCategories = incomeCategories,
                            onDismiss = {
                                BankNotificationManager.clearPendingTransaction(context)
                            },
                            onConfirmSync = { title, amount, type, categoryName, categoryIcon, categoryColorHex, note ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    categoryName = categoryName,
                                    categoryIcon = categoryIcon,
                                    categoryColorHex = categoryColorHex,
                                    note = note,
                                    dateMillis = bankTx.timestampMillis
                                )
                                BankNotificationManager.clearPendingTransaction(context)
                            }
                        )
                    }
                }
            }
        }
    }
}
