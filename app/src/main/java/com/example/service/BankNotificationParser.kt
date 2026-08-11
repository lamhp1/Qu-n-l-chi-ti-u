package com.example.service

import com.example.data.model.TransactionType
import java.util.UUID
import java.util.regex.Pattern

data class ParsedBankTransaction(
    val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    val amount: Double,
    val type: TransactionType,
    val rawText: String,
    val suggestedTitle: String,
    val suggestedCategory: String,
    val note: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

object BankNotificationParser {

    private val BANK_PACKAGES = mapOf(
        "com.VCB" to "Vietcombank",
        "com.vcb.digibank" to "Vietcombank",
        "vn.com.techcombank.bb.app" to "Techcombank",
        "com.mbbank.mbbank" to "MB Bank",
        "com.mbmobile" to "MB Bank",
        "com.vnpay.momo" to "MoMo",
        "com.mservice.momostore" to "MoMo",
        "com.mservice.momo" to "MoMo",
        "com.zalopay.merchant" to "ZaloPay",
        "vn.com.vpbank.neo" to "VPBank",
        "com.acb.mobile" to "ACB",
        "com.bidv.smartbanking" to "BIDV",
        "vn.vnpay.agribank3g" to "Agribank",
        "com.tpb.mb.gprsauto" to "TPBank",
        "com.vib.smart" to "VIB",
        "com.vietinbank.ipro" to "VietinBank"
    )

    fun isBankOrSmsNotification(packageName: String, title: String, text: String): Boolean {
        if (BANK_PACKAGES.containsKey(packageName)) return true
        val fullText = "$title $text".lowercase()
        val bankKeywords = listOf(
            "sd:", "so du", "số dư", "biến động", "bien dong", "tk ", "tai khoan", "tài khoản",
            "chuyen tien", "chuyển tiền", "nhan tien", "nhận tiền", "vcb", "mbbank", "techcombank",
            "vpbank", "tpbank", "bidv", "agribank", "vietinbank", "momo", "zalopay", "acb", "vib",
            "thanh toan", "thanh toán", "+", "-", "vnd", "đ", "vnd"
        )
        return bankKeywords.any { fullText.contains(it) }
    }

    fun parse(title: String, text: String, packageName: String = ""): ParsedBankTransaction? {
        val fullText = "$title $text"
        val bankName = BANK_PACKAGES[packageName] ?: detectBankName(fullText) ?: "Ngân hàng"

        val type = detectTransactionType(fullText)
        val amount = extractAmount(fullText) ?: return null

        val note = extractNote(fullText)
        val suggestedTitle = "Giao dịch $bankName"
        val suggestedCategory = guessCategory(fullText, type)

        return ParsedBankTransaction(
            bankName = bankName,
            amount = amount,
            type = type,
            rawText = fullText,
            suggestedTitle = suggestedTitle,
            suggestedCategory = suggestedCategory,
            note = note
        )
    }

    private fun detectBankName(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("vietcombank") || lower.contains("vcb") -> "Vietcombank"
            lower.contains("techcombank") || lower.contains("tcb") -> "Techcombank"
            lower.contains("mbbank") || lower.contains("mb bank") || lower.contains("mb ") -> "MB Bank"
            lower.contains("momo") -> "MoMo"
            lower.contains("zalopay") -> "ZaloPay"
            lower.contains("vpbank") -> "VPBank"
            lower.contains("acb") -> "ACB"
            lower.contains("bidv") -> "BIDV"
            lower.contains("agribank") -> "Agribank"
            lower.contains("tpbank") -> "TPBank"
            lower.contains("vib") -> "VIB"
            lower.contains("vietinbank") -> "VietinBank"
            else -> null
        }
    }

    private fun detectTransactionType(text: String): TransactionType {
        val lower = text.lowercase()
        if (lower.contains("nhan tien") || lower.contains("nhận tiền") ||
            lower.contains("nhan tu") || lower.contains("nhận từ") ||
            lower.contains("cong tk") || lower.contains("+ ") || lower.contains("biến động (+)") ||
            lower.contains("bien dong (+)") || lower.contains("chuyen den") || lower.contains("chuyển đến") ||
            lower.contains("tang ") || lower.contains("tăng ")
        ) {
            return TransactionType.INCOME
        }
        return TransactionType.EXPENSE
    }

    private fun extractAmount(text: String): Double? {
        // Find amounts like +100,000VND or 50.000d or -250,000 VND or 1,200,000đ
        val pattern = Pattern.compile("([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)\\s*(?:VND|vnd|đ|d)?")
        val matcher = pattern.matcher(text)

        val candidates = mutableListOf<Double>()
        while (matcher.find()) {
            val match = matcher.group(1)?.replace(" ", "")?.replace("+", "")?.replace("-", "") ?: continue
            val clean = match.replace(".", "").replace(",", "")
            val value = clean.toDoubleOrNull()
            if (value != null && value >= 1000) {
                candidates.add(value)
            }
        }
        // Usually the largest parsed number > 1000 is the transaction amount
        return candidates.maxOrNull()
    }

    private fun extractNote(text: String): String {
        val lower = text.lowercase()
        val keywords = listOf("nd:", "noi dung:", "nội dung:", "ref:", "ly do:", "lý do:", "thanh toan", "thanh toán")
        for (kw in keywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1) {
                val extracted = text.substring(idx + kw.length).trim()
                return extracted.take(80)
            }
        }
        return text.take(60)
    }

    private fun guessCategory(text: String, type: TransactionType): String {
        val lower = text.lowercase()
        if (type == TransactionType.INCOME) {
            return when {
                lower.contains("luong") || lower.contains("lương") || lower.contains("salary") -> "Lương"
                lower.contains("thuong") || lower.contains("thưởng") || lower.contains("bonus") -> "Thưởng"
                else -> "Thu nhập khác"
            }
        } else {
            return when {
                lower.contains("an uong") || lower.contains("ăn uống") || lower.contains("cafe") ||
                lower.contains("coffee") || lower.contains("quan an") || lower.contains("nhà hàng") ||
                lower.contains("tra sua") || lower.contains("trà sữa") -> "Ăn uống"

                lower.contains("sieu thi") || lower.contains("siêu thị") || lower.contains("mua sam") ||
                lower.contains("mua sắm") || lower.contains("shopee") || lower.contains("lazada") ||
                lower.contains("tiki") || lower.contains("mart") -> "Mua sắm"

                lower.contains("xang") || lower.contains("xăng") || lower.contains("grab") ||
                lower.contains("be ") || lower.contains("gojek") || lower.contains("taxi") ||
                lower.contains("ve xe") || lower.contains("vé xe") -> "Di chuyển"

                lower.contains("game") || lower.contains("phim") || lower.contains("netflix") ||
                lower.contains("cgv") || lower.contains("giai tri") || lower.contains("giải trí") -> "Giải trí"

                lower.contains("dien") || lower.contains("điện") || lower.contains("nuoc") ||
                lower.contains("nước") || lower.contains("internet") || lower.contains("wifi") ||
                lower.contains("hoa don") || lower.contains("hóa đơn") -> "Hóa đơn"

                lower.contains("benh vien") || lower.contains("bệnh viện") || lower.contains("thuoc") ||
                lower.contains("thuốc") || lower.contains("y te") || lower.contains("y tế") -> "Y tế"

                else -> "Chi tiêu khác"
            }
        }
    }
}
