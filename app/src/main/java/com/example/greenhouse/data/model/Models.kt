package com.example.greenhouse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentItem(
    val id: String,
    val amount: Long,
    val date: String, // YYYY-MM-DD
    val note: String
)

@Serializable
enum class DebtType {
    DEBTOR, // بدهکار (طلب ما - به ما بدهکار است)
    CREDITOR // بستانکار (بدهی ما - ما به او بدهکاریم)
}

@Serializable
enum class DebtStatus {
    PENDING, // جاری / تسویه نشده
    PAID // تسویه شده کامل
}

@Serializable
data class Debt(
    val id: String,
    val name: String, // نام شخص، شرکت یا طرف حساب
    val type: DebtType,
    val amount: Long, // کل مبلغ اصلی
    val description: String = "",
    val date: String, // تاریخ ثبت YYYY-MM-DD
    val dueDate: String, // تاریخ سررسید YYYY-MM-DD
    val status: DebtStatus = DebtStatus.PENDING,
    val payments: List<PaymentItem> = emptyList()
) {
    val totalPaid: Long
        get() = payments.sumOf { it.amount }

    val remainingAmount: Long
        get() = (amount - totalPaid).coerceAtLeast(0)

    val isSettled: Boolean
        get() = totalPaid >= amount
}

@Serializable
enum class TransactionType {
    INCOME, // درآمد
    EXPENSE // هزینه
}

@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Long, // به تومان
    val date: String, // YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss
    val category: String, // تگ دسته‌بندی
    val description: String = "",
    val relatedDebtId: String? = null,
    val harvestLogId: String? = null,
    val cropType: String? = null // نوع محصول مرتبط در صورت تخصیص هزینه یا درآمد
)

@Serializable
data class HarvestLog(
    val id: String,
    val date: String, // YYYY-MM-DD
    val cropType: String, // نوع محصول (خیار، گوجه‌فرنگی، توت‌فرنگی و...)
    val totalHarvestWeight: Double, // کیلوگرم
    val grade1Weight: Double,
    val grade1PricePerKg: Long,
    val grade2Weight: Double,
    val grade2PricePerKg: Long,
    val totalRevenue: Long,
    val note: String = "",
    val linkedTransactionId: String? = null
)

@Serializable
data class SecurityState(
    val hasPassword: Boolean = false,
    val passwordHash: String = "",
    val securityQuestion: String = "",
    val securityAnswerHash: String = ""
)

@Serializable
data class CropUnitEconomics(
    val cropType: String,
    val totalHarvestWeightKg: Double,
    val totalRevenue: Long,
    val totalAllocatedExpenses: Long,
    val avgSellingPricePerKg: Long,
    val costPerKg: Long,
    val netProfitPerKg: Long,
    val profitMarginPct: Int,
    val totalNetProfit: Long
)

@Serializable
data class AppBackupData(
    val transactions: List<Transaction> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val harvestLogs: List<HarvestLog> = emptyList(),
    val incomeTags: List<String> = emptyList(),
    val expenseTags: List<String> = emptyList(),
    val crops: List<String> = emptyList()
)
