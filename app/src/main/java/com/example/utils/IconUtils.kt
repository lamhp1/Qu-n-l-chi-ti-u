package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {

    fun getCategoryIcon(iconKey: String): ImageVector {
        return when (iconKey) {
            "restaurant" -> Icons.Default.Restaurant
            "shopping_bag" -> Icons.Default.ShoppingBag
            "directions_car" -> Icons.Default.DirectionsCar
            "sports_esports" -> Icons.Default.SportsEsports
            "receipt_long" -> Icons.Default.ReceiptLong
            "medical_services" -> Icons.Default.MedicalServices
            "school" -> Icons.Default.School
            "payments" -> Icons.Default.Payments
            "card_giftcard" -> Icons.Default.CardGiftcard
            "storefront" -> Icons.Default.Storefront
            "trending_up" -> Icons.Default.TrendingUp
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            else -> Icons.Default.Category
        }
    }

    fun parseHexColor(hex: String): Color {
        return try {
            val colorString = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            Color(0xFF00897B)
        }
    }
}
