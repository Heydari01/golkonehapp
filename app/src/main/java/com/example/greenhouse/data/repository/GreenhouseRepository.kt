package com.example.greenhouse.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.greenhouse.data.model.*
import com.example.greenhouse.util.JalaliCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

class GreenhouseRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("greenhouse_accounting_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    private val _harvestLogs = MutableStateFlow<List<HarvestLog>>(emptyList())
    val harvestLogs: StateFlow<List<HarvestLog>> = _harvestLogs.asStateFlow()

    private val _incomeTags = MutableStateFlow<List<String>>(Constants.PRESET_INCOME_TAGS)
    val incomeTags: StateFlow<List<String>> = _incomeTags.asStateFlow()

    private val _expenseTags = MutableStateFlow<List<String>>(Constants.PRESET_EXPENSE_TAGS)
    val expenseTags: StateFlow<List<String>> = _expenseTags.asStateFlow()

    private val _crops = MutableStateFlow<List<String>>(Constants.DEFAULT_CROP_OPTIONS)
    val crops: StateFlow<List<String>> = _crops.asStateFlow()

    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    private val _isUnlocked = MutableStateFlow(true)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        loadFromPrefs()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadFromPrefs() {
        val txJson = prefs.getString("transactions", null)
        if (!txJson.isNullOrEmpty()) {
            try {
                _transactions.value = json.decodeFromString(txJson)
            } catch (_: Exception) {}
        }

        val debtsJson = prefs.getString("debts", null)
        if (!debtsJson.isNullOrEmpty()) {
            try {
                _debts.value = json.decodeFromString(debtsJson)
            } catch (_: Exception) {}
        }

        val harvestJson = prefs.getString("harvest_logs", null)
        if (!harvestJson.isNullOrEmpty()) {
            try {
                _harvestLogs.value = json.decodeFromString(harvestJson)
            } catch (_: Exception) {}
        }

        val incomeTagsJson = prefs.getString("income_tags", null)
        if (!incomeTagsJson.isNullOrEmpty()) {
            try {
                _incomeTags.value = json.decodeFromString(incomeTagsJson)
            } catch (_: Exception) {}
        }

        val expenseTagsJson = prefs.getString("expense_tags", null)
        if (!expenseTagsJson.isNullOrEmpty()) {
            try {
                _expenseTags.value = json.decodeFromString(expenseTagsJson)
            } catch (_: Exception) {}
        }

        val cropsJson = prefs.getString("crops", null)
        if (!cropsJson.isNullOrEmpty()) {
            try {
                val loadedCrops: List<String> = json.decodeFromString(cropsJson)
                if (loadedCrops.isNotEmpty()) {
                    // Ensure default strawberry and others are present
                    val combined = (loadedCrops + Constants.DEFAULT_CROP_OPTIONS).distinct()
                    _crops.value = combined
                }
            } catch (_: Exception) {}
        }

        val secJson = prefs.getString("security_state", null)
        if (!secJson.isNullOrEmpty()) {
            try {
                val sec: SecurityState = json.decodeFromString(secJson)
                _securityState.value = sec
                _isUnlocked.value = !sec.hasPassword
            } catch (_: Exception) {
                _isUnlocked.value = true
            }
        } else {
            _isUnlocked.value = true
        }
    }

    private fun saveTransactions(list: List<Transaction>) {
        _transactions.value = list
        prefs.edit().putString("transactions", json.encodeToString(list)).apply()
    }

    private fun saveDebts(list: List<Debt>) {
        _debts.value = list
        prefs.edit().putString("debts", json.encodeToString(list)).apply()
    }

    private fun saveHarvestLogs(list: List<HarvestLog>) {
        _harvestLogs.value = list
        prefs.edit().putString("harvest_logs", json.encodeToString(list)).apply()
    }

    private fun saveIncomeTags(list: List<String>) {
        _incomeTags.value = list
        prefs.edit().putString("income_tags", json.encodeToString(list)).apply()
    }

    private fun saveExpenseTags(list: List<String>) {
        _expenseTags.value = list
        prefs.edit().putString("expense_tags", json.encodeToString(list)).apply()
    }

    private fun saveCrops(list: List<String>) {
        _crops.value = list
        prefs.edit().putString("crops", json.encodeToString(list)).apply()
    }

    private fun saveSecurityState(state: SecurityState) {
        _securityState.value = state
        prefs.edit().putString("security_state", json.encodeToString(state)).apply()
    }

    // Crops Management
    fun addCustomCrop(cropName: String) {
        val clean = cropName.trim()
        if (clean.isNotEmpty() && !_crops.value.contains(clean)) {
            val updated = _crops.value + clean
            saveCrops(updated)
            // Also add a default income tag for this crop
            addCustomTag(TransactionType.INCOME, "فروش محصول $clean")
        }
    }

    // Transactions CRUD
    fun addTransaction(
        type: TransactionType,
        amount: Long,
        date: String,
        category: String,
        description: String,
        relatedDebtId: String? = null,
        harvestLogId: String? = null,
        cropType: String? = null
    ): Transaction {
        val newTx = Transaction(
            id = "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            type = type,
            amount = amount,
            date = date,
            category = category,
            description = description,
            relatedDebtId = relatedDebtId,
            harvestLogId = harvestLogId,
            cropType = cropType
        )
        saveTransactions(listOf(newTx) + _transactions.value)
        return newTx
    }

    fun updateTransaction(updatedTx: Transaction) {
        saveTransactions(_transactions.value.map { if (it.id == updatedTx.id) updatedTx else it })
    }

    fun deleteTransaction(id: String) {
        saveTransactions(_transactions.value.filter { it.id != id })
    }

    // Debts CRUD
    fun addDebt(
        name: String,
        type: DebtType,
        amount: Long,
        description: String,
        date: String,
        dueDate: String
    ): Debt {
        val newDebt = Debt(
            id = "debt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            name = name,
            type = type,
            amount = amount,
            description = description,
            date = date,
            dueDate = dueDate,
            status = DebtStatus.PENDING,
            payments = emptyList()
        )
        saveDebts(listOf(newDebt) + _debts.value)
        return newDebt
    }

    fun deleteDebt(id: String) {
        saveDebts(_debts.value.filter { it.id != id })
    }

    fun registerDebtPayment(debtId: String, amount: Long, date: String, note: String) {
        val currentDebt = _debts.value.find { it.id == debtId } ?: return
        val newPayment = PaymentItem(
            id = "pm_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            amount = amount,
            date = date,
            note = note
        )
        val updatedPayments = currentDebt.payments + newPayment
        val totalPaid = updatedPayments.sumOf { it.amount }
        val isSettled = totalPaid >= currentDebt.amount

        val updatedDebt = currentDebt.copy(
            payments = updatedPayments,
            status = if (isSettled) DebtStatus.PAID else DebtStatus.PENDING
        )
        saveDebts(_debts.value.map { if (it.id == debtId) updatedDebt else it })

        // Auto ledger corresponding transaction!
        val txType = if (currentDebt.type == DebtType.DEBTOR) TransactionType.INCOME else TransactionType.EXPENSE
        val txCategory = if (currentDebt.type == DebtType.DEBTOR) "وصول طلبکاری / اقساط" else "پرداخت بدهی / اقساط"
        val txDesc = "${currentDebt.name} - $note"

        addTransaction(
            type = txType,
            amount = amount,
            date = date,
            category = txCategory,
            description = txDesc,
            relatedDebtId = debtId
        )
    }

    // Harvest Logs CRUD
    fun addHarvestLog(
        date: String,
        cropType: String,
        totalHarvestWeight: Double,
        grade1Weight: Double,
        grade1PricePerKg: Long,
        grade2Weight: Double,
        grade2PricePerKg: Long,
        totalRevenue: Long,
        note: String,
        autoCreateTx: Boolean
    ): HarvestLog {
        val logId = "harvest_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"
        var linkedTxId: String? = null

        if (autoCreateTx && totalRevenue > 0) {
            val category = when (cropType) {
                "توت‌فرنگی" -> "فروش محصول توت‌فرنگی"
                "خیار" -> "فروش محصول خیار"
                "گوجه‌فرنگی" -> "فروش محصول گوجه‌فرنگی"
                "فلفل دلمه‌ای" -> "فروش فلفل دلمه‌ای"
                "بادمجان" -> "فروش محصول بادمجان"
                "صیفی‌جات" -> "فروش صیفی‌جات"
                "گل و گیاه زینتی" -> "فروش گل و گیاه زینتی"
                "نشاء و قلمه" -> "فروش نشاء و قلمه"
                else -> "فروش محصول $cropType"
            }
            val soldWeight = grade1Weight + grade2Weight
            val txDesc = "فروش ${JalaliCalendar.toPersianDigits(soldWeight)} کیلو $cropType " +
                    "(درجه۱: ${JalaliCalendar.toPersianDigits(grade1Weight)}ک، درجه۲: ${JalaliCalendar.toPersianDigits(grade2Weight)}ک)" +
                    if (note.isNotBlank()) " - $note" else ""

            val tx = addTransaction(
                type = TransactionType.INCOME,
                amount = totalRevenue,
                date = "${date}T12:00:00",
                category = category,
                description = txDesc,
                relatedDebtId = null,
                harvestLogId = logId,
                cropType = cropType
            )
            linkedTxId = tx.id
        }

        val newLog = HarvestLog(
            id = logId,
            date = date,
            cropType = cropType,
            totalHarvestWeight = totalHarvestWeight,
            grade1Weight = grade1Weight,
            grade1PricePerKg = grade1PricePerKg,
            grade2Weight = grade2Weight,
            grade2PricePerKg = grade2PricePerKg,
            totalRevenue = totalRevenue,
            note = note,
            linkedTransactionId = linkedTxId
        )
        saveHarvestLogs(listOf(newLog) + _harvestLogs.value)
        return newLog
    }

    fun deleteHarvestLog(id: String) {
        val target = _harvestLogs.value.find { it.id == id }
        if (target?.linkedTransactionId != null) {
            deleteTransaction(target.linkedTransactionId)
        }
        saveHarvestLogs(_harvestLogs.value.filter { it.id != id })
    }

    // Custom Tags
    fun addCustomTag(type: TransactionType, tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return
        if (type == TransactionType.INCOME) {
            if (!_incomeTags.value.contains(cleanTag)) {
                saveIncomeTags(_incomeTags.value + cleanTag)
            }
        } else {
            if (!_expenseTags.value.contains(cleanTag)) {
                saveExpenseTags(_expenseTags.value + cleanTag)
            }
        }
    }

    // Security & App Lock
    fun setPassword(password: String, question: String, answer: String): Boolean {
        if (password.length < 4) return false
        val state = SecurityState(
            hasPassword = true,
            passwordHash = hashString(password),
            securityQuestion = question.trim(),
            securityAnswerHash = hashString(answer.trim().lowercase())
        )
        saveSecurityState(state)
        _isUnlocked.value = true
        return true
    }

    fun clearPassword(currentPassword: String): Boolean {
        if (_securityState.value.hasPassword) {
            if (hashString(currentPassword) != _securityState.value.passwordHash) {
                return false
            }
        }
        saveSecurityState(SecurityState())
        _isUnlocked.value = true
        return true
    }

    fun unlockWithPassword(password: String): Boolean {
        val expected = _securityState.value.passwordHash
        if (expected.isEmpty() || hashString(password) == expected) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun unlockWithRecovery(answer: String): Boolean {
        val expected = _securityState.value.securityAnswerHash
        if (expected.isNotEmpty() && hashString(answer.trim().lowercase()) == expected) {
            saveSecurityState(SecurityState())
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun lockApp() {
        if (_securityState.value.hasPassword) {
            _isUnlocked.value = false
        }
    }

    // Backup & Export
    fun exportBackupJson(): String {
        val data = AppBackupData(
            transactions = _transactions.value,
            debts = _debts.value,
            harvestLogs = _harvestLogs.value,
            incomeTags = _incomeTags.value,
            expenseTags = _expenseTags.value,
            crops = _crops.value
        )
        return json.encodeToString(data)
    }

    fun importBackupJson(backupJson: String): Boolean {
        return try {
            val data: AppBackupData = json.decodeFromString(backupJson)
            saveTransactions(data.transactions)
            saveDebts(data.debts)
            saveHarvestLogs(data.harvestLogs)
            if (data.incomeTags.isNotEmpty()) saveIncomeTags(data.incomeTags)
            if (data.expenseTags.isNotEmpty()) saveExpenseTags(data.expenseTags)
            if (data.crops.isNotEmpty()) saveCrops((data.crops + Constants.DEFAULT_CROP_OPTIONS).distinct())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearAllData() {
        saveTransactions(emptyList())
        saveDebts(emptyList())
        saveHarvestLogs(emptyList())
        saveIncomeTags(Constants.PRESET_INCOME_TAGS)
        saveExpenseTags(Constants.PRESET_EXPENSE_TAGS)
        saveCrops(Constants.DEFAULT_CROP_OPTIONS)
    }
}
