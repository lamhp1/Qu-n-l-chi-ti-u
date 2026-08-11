package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean = true
) {
    companion object {
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            Category(id = 1, name = "Ăn uống", type = TransactionType.EXPENSE, iconKey = "restaurant", colorHex = "#FB8C00"),
            Category(id = 2, name = "Mua sắm", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorHex = "#E91E63"),
            Category(id = 3, name = "Di chuyển", type = TransactionType.EXPENSE, iconKey = "directions_car", colorHex = "#1E88E5"),
            Category(id = 4, name = "Giải trí", type = TransactionType.EXPENSE, iconKey = "sports_esports", colorHex = "#8E24AA"),
            Category(id = 5, name = "Hóa đơn & Tiện ích", type = TransactionType.EXPENSE, iconKey = "receipt_long", colorHex = "#3949AB"),
            Category(id = 6, name = "Sức khỏe", type = TransactionType.EXPENSE, iconKey = "medical_services", colorHex = "#D32F2F"),
            Category(id = 7, name = "Giáo dục", type = TransactionType.EXPENSE, iconKey = "school", colorHex = "#00ACC1"),
            Category(id = 8, name = "Khác", type = TransactionType.EXPENSE, iconKey = "more_horiz", colorHex = "#757575")
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            Category(id = 101, name = "Lương", type = TransactionType.INCOME, iconKey = "payments", colorHex = "#2E7D32"),
            Category(id = 102, name = "Thưởng", type = TransactionType.INCOME, iconKey = "card_giftcard", colorHex = "#FFB300"),
            Category(id = 103, name = "Bán hàng", type = TransactionType.INCOME, iconKey = "storefront", colorHex = "#00897B"),
            Category(id = 104, name = "Đầu tư", type = TransactionType.INCOME, iconKey = "trending_up", colorHex = "#3949AB"),
            Category(id = 105, name = "Thu nhập khác", type = TransactionType.INCOME, iconKey = "account_balance_wallet", colorHex = "#00ACC1")
        )
    }
}
