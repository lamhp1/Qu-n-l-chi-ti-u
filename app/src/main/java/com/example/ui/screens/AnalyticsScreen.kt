package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.CustomDatePickerDialog
import com.example.ui.components.DateNavigatorCard
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategoryExpenseSummary
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.PeriodBreakdownItem
import com.example.ui.viewmodel.TimeFilter
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.IconUtils
import java.util.Calendar

enum class AnalyticsViewMode(val title: String) {
    CATEGORY("Theo Danh Mục"),
    TIMELINE("Chi Tiết Thời Gian")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val filter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val selectedDayMillis by viewModel.selectedDayMillis.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val categorySummaries by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val periodBreakdowns by viewModel.periodBreakdown.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(AnalyticsViewMode.CATEGORY) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showYearPickerDialog by remember { mutableStateOf(false) }

    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((savings / totalIncome) * 100).coerceIn(0.0, 100.0) else 0.0

    Scaffold(
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

            // Screen Header & Filter Options
            item {
                Column {
                    Text(
                        text = "Thống Kê Tài Chính",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time Filter Chips Row (Scrollable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeFilter.values().forEach { timeFilter ->
                            val isSelected = timeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTimeFilter(timeFilter) },
                                label = {
                                    Text(
                                        text = timeFilter.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Date/Month/Year Navigator Control
                    when (filter) {
                        TimeFilter.TODAY, TimeFilter.SPECIFIC_DAY -> {
                            DateNavigatorCard(
                                title = DateUtils.getRelativeDateString(selectedDayMillis),
                                onPrev = { viewModel.navigateDay(-1) },
                                onNext = { viewModel.navigateDay(1) },
                                onClickCenter = { showDatePickerDialog = true }
                            )
                        }
                        TimeFilter.THIS_MONTH, TimeFilter.SPECIFIC_MONTH -> {
                            DateNavigatorCard(
                                title = "Tháng %02d/%d".format(selectedMonth, selectedYear),
                                onPrev = { viewModel.navigateMonth(-1) },
                                onNext = { viewModel.navigateMonth(1) },
                                onClickCenter = { showMonthPickerDialog = true }
                            )
                        }
                        TimeFilter.SPECIFIC_YEAR -> {
                            DateNavigatorCard(
                                title = "Năm %d".format(selectedYear),
                                onPrev = { viewModel.navigateYear(-1) },
                                onNext = { viewModel.navigateYear(1) },
                                onClickCenter = { showYearPickerDialog = true }
                            )
                        }
                        else -> { /* No extra date bar for ALL / THIS_WEEK */ }
                    }
                }
            }

            // Financial Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tổng Quan Dòng Tiền",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tỷ lệ tích lũy: ${savingsRate.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tổng Thu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatVnd(totalIncome),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            Column {
                                Text("Tổng Chi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatVnd(totalExpense),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Dư Tích Lũy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatVnd(savings),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (savings >= 0) EmeraldPrimary else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { (savingsRate / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // View Mode Switcher Tabs
            item {
                SecondaryTabRow(
                    selectedTabIndex = viewMode.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = EmeraldPrimary
                ) {
                    AnalyticsViewMode.values().forEach { mode ->
                        Tab(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (mode == AnalyticsViewMode.CATEGORY) Icons.Default.PieChart else Icons.Default.ViewList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                    }
                }
            }

            // View Mode Content
            if (viewMode == AnalyticsViewMode.CATEGORY) {
                // Donut Chart
                item {
                    ExpenseDonutChart(
                        categorySummaries = categorySummaries,
                        totalExpense = totalExpense
                    )
                }

                // Category Ranking Header
                item {
                    Text(
                        text = "Xếp Hạng Chi Tiêu Cao Nhất",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Category Detail List
                if (categorySummaries.isEmpty()) {
                    item {
                        Text(
                            text = "Chưa có dữ liệu chi tiêu trong khoảng thời gian này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(categorySummaries) { summary ->
                        CategoryRankingItem(summary = summary)
                    }
                }
            } else {
                // Timeline Breakdown Content
                item {
                    Text(
                        text = if (filter == TimeFilter.SPECIFIC_YEAR || filter == TimeFilter.ALL)
                            "Chi Tiết Theo Các Tháng"
                        else
                            "Chi Tiết Theo Các Ngày Trong Kỳ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (periodBreakdowns.isEmpty()) {
                    item {
                        Text(
                            text = "Không có giao dịch nào trong khoảng thời gian đã chọn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(periodBreakdowns) { breakdown ->
                        PeriodBreakdownCard(
                            item = breakdown,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        CustomDatePickerDialog(
            initialDateMillis = selectedDayMillis,
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { millis ->
                viewModel.setSelectedDay(millis)
                showDatePickerDialog = false
            }
        )
    }

    // Month & Year Picker Dialog
    if (showMonthPickerDialog) {
        MonthYearPickerDialog(
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            onDismiss = { showMonthPickerDialog = false },
            onConfirm = { m, y ->
                viewModel.setSelectedMonthYear(m, y)
                showMonthPickerDialog = false
            }
        )
    }

    // Year Picker Dialog
    if (showYearPickerDialog) {
        YearPickerDialog(
            initialYear = selectedYear,
            onDismiss = { showYearPickerDialog = false },
            onConfirm = { y ->
                viewModel.setSelectedYear(y)
                showYearPickerDialog = false
            }
        )
    }
}

@Composable
fun PeriodBreakdownCard(
    item: PeriodBreakdownItem,
    onDeleteTransaction: (Transaction) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.subTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (item.totalIncome > 0) {
                            Text(
                                text = "+${CurrencyUtils.formatVnd(item.totalIncome)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                        if (item.totalExpense > 0) {
                            Text(
                                text = "-${CurrencyUtils.formatVnd(item.totalExpense)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Chi tiết",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    item.transactions.forEach { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onDelete = onDeleteTransaction
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRankingItem(summary: CategoryExpenseSummary) {
    val color = IconUtils.parseHexColor(summary.colorHex)
    val icon = IconUtils.getCategoryIcon(summary.iconKey)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = summary.categoryName,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = summary.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.percentage.toInt()}% tổng chi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = CurrencyUtils.formatVnd(summary.totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { summary.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
    }
}

