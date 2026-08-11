package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CustomDatePickerDialog
import com.example.ui.components.DateNavigatorCard
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.YearPickerDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.TimeFilter
import com.example.utils.DateUtils

@Composable
fun HistoryScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val selectedDayMillis by viewModel.selectedDayMillis.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showYearPickerDialog by remember { mutableStateOf(false) }

    // Group transactions by Date String
    val groupedTransactions = transactions.groupBy { DateUtils.getRelativeDateString(it.dateMillis) }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = "Lịch Sử Giao Dịch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Tìm kiếm theo tên, ghi chú, danh mục...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa tìm kiếm"
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time Filter Row (Scrollable)
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

            // Date Navigator Card depending on selected filter
            when (filter) {
                TimeFilter.TODAY, TimeFilter.SPECIFIC_DAY -> {
                    DateNavigatorCard(
                        title = DateUtils.getRelativeDateString(selectedDayMillis),
                        onPrev = { viewModel.navigateDay(-1) },
                        onNext = { viewModel.navigateDay(1) },
                        onClickCenter = { showDatePickerDialog = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                TimeFilter.THIS_MONTH, TimeFilter.SPECIFIC_MONTH -> {
                    DateNavigatorCard(
                        title = "Tháng %02d/%d".format(selectedMonth, selectedYear),
                        onPrev = { viewModel.navigateMonth(-1) },
                        onNext = { viewModel.navigateMonth(1) },
                        onClickCenter = { showMonthPickerDialog = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                TimeFilter.SPECIFIC_YEAR -> {
                    DateNavigatorCard(
                        title = "Năm %d".format(selectedYear),
                        onPrev = { viewModel.navigateYear(-1) },
                        onNext = { viewModel.navigateYear(1) },
                        onClickCenter = { showYearPickerDialog = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                else -> {}
            }

            // Transaction List Grouped by Date
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy giao dịch tương ứng" else "Chưa có giao dịch nào trong khoảng thời gian này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (dateGroup, itemsInGroup) ->
                        item {
                            Text(
                                text = dateGroup,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(
                            items = itemsInGroup,
                            key = { it.id }
                        ) { transaction ->
                            TransactionItemCard(
                                transaction = transaction,
                                onDelete = { viewModel.deleteTransaction(it) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Specific Date Picker Dialog
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
