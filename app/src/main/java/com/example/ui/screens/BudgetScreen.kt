package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BudgetProgressCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.BudgetStatus
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.IconUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.monthlyBudgets.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var selectedCategoryForBudget by remember { mutableStateOf("ALL") }
    var customBudgetName by remember { mutableStateOf("") }
    var selectedCustomIcon by remember { mutableStateOf("account_balance_wallet") }
    var selectedCustomColor by remember { mutableStateOf("#00897B") }
    var inputAmountText by remember { mutableStateOf("") }

    val standardCategoryKeys = remember(expenseCategories) {
        listOf("ALL") + expenseCategories.map { it.name }
    }
    val categoryNames = remember(expenseCategories) {
        listOf(
            "ALL" to "Tất cả (Tổng ngân sách hàng tháng)",
            "CUSTOM" to "Tùy chọn (Đặt tên ngân sách riêng)..."
        ) + expenseCategories.map { it.name to it.name }
    }

    Scaffold(
        floatingActionButton = {
            Button(
                onClick = {
                    selectedCategoryForBudget = "ALL"
                    customBudgetName = ""
                    inputAmountText = ""
                    showEditDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_budget_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Thiết Lập Hạn Mức", fontWeight = FontWeight.Bold)
            }
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

            item {
                Column {
                    Text(
                        text = "Quản Lý Ngân Sách Hàng Tháng",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Thiết lập hạn mức chi tiêu để kiểm soát tài chính chủ động hơn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Text(
                        text = "Chưa có hạn mức nào được đặt. Hãy bấm nút phía dưới để bắt đầu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(
                    items = budgets.distinctBy { it.categoryName },
                    key = { it.categoryName }
                ) { status ->
                    BudgetProgressCard(
                        budgetStatus = status,
                        onEditBudget = {
                            if (standardCategoryKeys.contains(it.categoryName)) {
                                selectedCategoryForBudget = it.categoryName
                                customBudgetName = ""
                            } else {
                                selectedCategoryForBudget = "CUSTOM"
                                customBudgetName = it.categoryName
                            }
                            val existingCat = expenseCategories.find { c -> c.name.equals(it.categoryName, ignoreCase = true) }
                            if (existingCat != null) {
                                selectedCustomIcon = existingCat.iconKey
                                selectedCustomColor = existingCat.colorHex
                            } else {
                                selectedCustomIcon = "account_balance_wallet"
                                selectedCustomColor = "#00897B"
                            }
                            inputAmountText = CurrencyUtils.formatNumber(it.limitAmount)
                            showEditDialog = true
                        },
                        onDeleteBudget = {
                            categoryToDelete = it.categoryName
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Edit/Add Budget Dialog
    if (showEditDialog) {
        var expandedDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Cài Đặt Hạn Mức Chi Tiêu",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Category Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        val currentLabel = categoryNames.firstOrNull { it.first == selectedCategoryForBudget }?.second
                            ?: if (selectedCategoryForBudget == "CUSTOM") "Tùy chọn (Đặt tên ngân sách riêng)..." else selectedCategoryForBudget

                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Áp dụng cho") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            categoryNames.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedCategoryForBudget = key
                                        val existingCat = expenseCategories.find { c -> c.name.equals(key, ignoreCase = true) }
                                        if (existingCat != null) {
                                            selectedCustomIcon = existingCat.iconKey
                                            selectedCustomColor = existingCat.colorHex
                                        }
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Custom Budget Name Field
                    if (selectedCategoryForBudget == "CUSTOM") {
                        OutlinedTextField(
                            value = customBudgetName,
                            onValueChange = { customBudgetName = it },
                            label = { Text("Tên ngân sách tùy chỉnh") },
                            placeholder = { Text("VD: Tiền đi du lịch, Quỹ mua sắm...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_budget_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Icon & Color selector (available for any category except ALL)
                    if (selectedCategoryForBudget != "ALL") {
                        // Icon selector
                        Column {
                            Text(
                                text = "Biểu tượng danh mục",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val availableIcons = listOf(
                                "account_balance_wallet", "shopping_bag", "restaurant",
                                "directions_car", "sports_esports", "card_giftcard",
                                "school", "medical_services", "receipt_long"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableIcons.forEach { iconKey ->
                                    val isSelected = selectedCustomIcon == iconKey
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedCustomIcon = iconKey },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconUtils.getCategoryIcon(iconKey),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Color selector
                        Column {
                            Text(
                                text = "Màu sắc danh mục",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val availableColors = listOf(
                                "#00897B", "#E91E63", "#FB8C00", "#1E88E5",
                                "#8E24AA", "#D32F2F", "#00ACC1", "#FFB300"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableColors.forEach { hex ->
                                    val isSelected = selectedCustomColor == hex
                                    val c = IconUtils.parseHexColor(hex)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedCustomColor = hex }
                                    )
                                }
                            }
                        }
                    }

                    // Limit Amount Input
                    OutlinedTextField(
                        value = inputAmountText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            inputAmountText = if (clean.isNotBlank()) {
                                val num = clean.toDoubleOrNull() ?: 0.0
                                CurrencyUtils.formatNumber(num)
                            } else ""
                        },
                        label = { Text("Hạn mức tối đa (VND)") },
                        suffix = { Text("₫") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCategoryName = if (selectedCategoryForBudget == "CUSTOM") {
                            customBudgetName.trim()
                        } else {
                            selectedCategoryForBudget
                        }
                        val limit = CurrencyUtils.parseAmount(inputAmountText)
                        if (finalCategoryName.isNotBlank() && limit > 0) {
                            viewModel.setBudget(
                                categoryName = finalCategoryName,
                                limitAmount = limit,
                                iconKey = selectedCustomIcon,
                                colorHex = selectedCustomColor
                            )
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Lưu Hạn Mức")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedCategoryForBudget != "ALL") {
                        TextButton(
                            onClick = {
                                val target = if (selectedCategoryForBudget == "CUSTOM") customBudgetName.trim() else selectedCategoryForBudget
                                if (target.isNotBlank()) {
                                    categoryToDelete = target
                                    showEditDialog = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                        ) {
                            Text("Xóa Ngân Sách")
                        }
                    }
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Hủy")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val targetName = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Xác Nhận Xóa Ngân Sách", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa ngân sách cho '$targetName' không?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBudget(targetName)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
