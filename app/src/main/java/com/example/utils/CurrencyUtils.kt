package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    /**
     * Formats double to Vietnamese currency format string, e.g., "50.000 ₫"
     */
    fun formatVnd(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = formatter.format(amount)
        return "$formatted ₫"
    }

    /**
     * Formats double without symbol, e.g., "50.000"
     */
    fun formatNumber(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(amount)
    }

    /**
     * Parses clean numeric string into Double
     */
    fun parseAmount(input: String): Double {
        val clean = input.replace(".", "").replace(",", "").replace(" ", "").replace("₫", "")
        return clean.toDoubleOrNull() ?: 0.0
    }
}
