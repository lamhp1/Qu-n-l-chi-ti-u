package com.example.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BankNotificationManager {

    private val _pendingTransaction = MutableStateFlow<ParsedBankTransaction?>(null)
    val pendingTransaction: StateFlow<ParsedBankTransaction?> = _pendingTransaction.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private var lastProcessedKey: String? = null
    private var lastProcessedTime: Long = 0L

    fun postTransaction(transaction: ParsedBankTransaction, context: Context? = null) {
        val key = "${transaction.bankName}_${transaction.type}_${transaction.amount}_${transaction.note.trim()}"
        val now = System.currentTimeMillis()

        // Ignore identical duplicates within 3 seconds
        if (key == lastProcessedKey && (now - lastProcessedTime) < 3000) {
            return
        }

        lastProcessedKey = key
        lastProcessedTime = now

        _pendingTransaction.value = transaction
        context?.let { ctx ->
            NotificationHelper.triggerHeadsUpAndOpenApp(ctx, transaction)
        }
    }

    fun clearPendingTransaction(context: Context? = null) {
        _pendingTransaction.value = null
        lastProcessedKey = null
        context?.let { ctx ->
            NotificationHelper.cancelNotification(ctx)
        }
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }

    // Helper simulation samples for quick testing
    fun simulateSampleNotification(context: Context, sampleIndex: Int) {
        clearPendingTransaction(context)
        val samples = listOf(
            ParsedBankTransaction(
                bankName = "Vietcombank",
                amount = 15000000.0,
                type = com.example.data.model.TransactionType.INCOME,
                rawText = "SD TK 001100... +15,000,000VND luc 08:30. ND: Cong ty ABC thanh toan Luong thoi gian T8/2026",
                suggestedTitle = "Nhận lương Vietcombank",
                suggestedCategory = "Lương",
                note = "Công ty ABC thanh toán Lương T8/2026"
            ),
            ParsedBankTransaction(
                bankName = "Techcombank",
                amount = 125000.0,
                type = com.example.data.model.TransactionType.EXPENSE,
                rawText = "TK 1903... bien dong (-): -125,000 VND. ND: Thanh toan qua Quán Ăn Ngon Ha Noi",
                suggestedTitle = "Giao dịch Techcombank",
                suggestedCategory = "Ăn uống",
                note = "Thanh toán Quán Ăn Ngon Hà Nội"
            ),
            ParsedBankTransaction(
                bankName = "MB Bank",
                amount = 450000.0,
                type = com.example.data.model.TransactionType.EXPENSE,
                rawText = "TK 098...-450,000đ. ND: ShopeePay - Mua sam quan ao thoi trang",
                suggestedTitle = "Giao dịch MB Bank",
                suggestedCategory = "Mua sắm",
                note = "ShopeePay - Mua sắm quần áo thời trang"
            ),
            ParsedBankTransaction(
                bankName = "MoMo",
                amount = 35000.0,
                type = com.example.data.model.TransactionType.EXPENSE,
                rawText = "MoMo: Thanh toan thanh cong 35,000d cho Highland Coffee",
                suggestedTitle = "Thanh toán MoMo",
                suggestedCategory = "Ăn uống",
                note = "Thanh toán Highland Coffee"
            )
        )

        val selected = samples.getOrElse(sampleIndex % samples.size) { samples[0] }
        val freshTx = selected.copy(
            id = java.util.UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis()
        )
        postTransaction(freshTx, context)
    }
}
