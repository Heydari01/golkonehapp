package com.example.greenhouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenhouse.data.model.*
import com.example.greenhouse.data.repository.GreenhouseRepository
import com.example.greenhouse.data.service.GeminiAssistantService
import com.example.greenhouse.data.service.GeminiParsedAction
import com.example.greenhouse.util.JalaliCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    TRANSACTIONS,
    ADD,
    HARVEST,
    DEBTS,
    BACKUP,
    ASSISTANT
}

data class SummaryMetrics(
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val netProfit: Long = 0,
    val activeReceivable: Long = 0
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedAction: ProposedAction? = null
)

data class ProposedAction(
    val title: String,
    val details: String,
    val execute: () -> Unit
)

class GreenhouseViewModel(
    private val repository: GreenhouseRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.transactions
    val debts: StateFlow<List<Debt>> = repository.debts
    val harvestLogs: StateFlow<List<HarvestLog>> = repository.harvestLogs
    val incomeTags: StateFlow<List<String>> = repository.incomeTags
    val expenseTags: StateFlow<List<String>> = repository.expenseTags
    val crops: StateFlow<List<String>> = repository.crops
    val securityState: StateFlow<SecurityState> = repository.securityState
    val isUnlocked: StateFlow<Boolean> = repository.isUnlocked

    private val _activeTab = MutableStateFlow(AppTab.DASHBOARD)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()

    // Calculated summary metrics
    val summaryMetrics: StateFlow<SummaryMetrics> = combine(transactions, debts) { txList, debtList ->
        var income = 0L
        var expense = 0L
        txList.forEach {
            if (it.type == TransactionType.INCOME) income += it.amount
            else expense += it.amount
        }
        val net = income - expense

        val activeRec = debtList
            .filter { it.type == DebtType.DEBTOR && it.status == DebtStatus.PENDING }
            .sumOf { it.remainingAmount }

        SummaryMetrics(
            totalIncome = income,
            totalExpense = expense,
            netProfit = net,
            activeReceivable = activeRec
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryMetrics())

    // Unit Economics per crop (Cost per Kg & Profit per Kg)
    val unitEconomicsList: StateFlow<List<CropUnitEconomics>> = combine(transactions, harvestLogs, crops) { txList, hList, cropList ->
        val result = mutableListOf<CropUnitEconomics>()

        // 1. Calculate for each specific crop
        val totalGeneralExpenses = txList.filter { it.type == TransactionType.EXPENSE && it.cropType == null }.sumOf { it.amount }
        val totalAllHarvestWeight = hList.sumOf { it.totalHarvestWeight }

        cropList.forEach { crop ->
            val cropLogs = hList.filter { it.cropType == crop }
            val totalWeight = cropLogs.sumOf { it.totalHarvestWeight }
            val cropRevenueFromLogs = cropLogs.sumOf { it.totalRevenue }
            val cropRevenueFromTx = txList.filter {
                it.type == TransactionType.INCOME && it.cropType == crop && it.harvestLogId == null
            }.sumOf { it.amount }
            val totalRevenue = cropRevenueFromLogs + cropRevenueFromTx

            // Direct expenses specifically assigned to this crop (or matching name)
            val directExpenses = txList.filter {
                it.type == TransactionType.EXPENSE && (it.cropType == crop || it.category.contains(crop) || it.description.contains(crop))
            }.sumOf { it.amount }

            // Allocated general overhead expenses (proportional by weight or fallback equal split)
            val allocatedGeneral = if (totalAllHarvestWeight > 0 && totalWeight > 0) {
                ((totalGeneralExpenses * totalWeight) / totalAllHarvestWeight).toLong()
            } else 0L

            val totalExpenses = directExpenses + allocatedGeneral

            val avgSellingPrice = if (totalWeight > 0) (totalRevenue / totalWeight).toLong() else 0L
            val costPerKg = if (totalWeight > 0) (totalExpenses / totalWeight).toLong() else 0L
            val netProfitPerKg = avgSellingPrice - costPerKg
            val marginPct = if (avgSellingPrice > 0) ((netProfitPerKg * 100) / avgSellingPrice).toInt() else 0
            val totalNet = totalRevenue - totalExpenses

            if (totalWeight > 0 || totalRevenue > 0 || totalExpenses > 0) {
                result.add(
                    CropUnitEconomics(
                        cropType = crop,
                        totalHarvestWeightKg = totalWeight,
                        totalRevenue = totalRevenue,
                        totalAllocatedExpenses = totalExpenses,
                        avgSellingPricePerKg = avgSellingPrice,
                        costPerKg = costPerKg,
                        netProfitPerKg = netProfitPerKg,
                        profitMarginPct = marginPct,
                        totalNetProfit = totalNet
                    )
                )
            }
        }

        // If list is empty or to have a combined overview, add default "توت‌فرنگی" and "خیار" if not present
        if (result.isEmpty()) {
            result.add(
                CropUnitEconomics(
                    cropType = "توت‌فرنگی",
                    totalHarvestWeightKg = 0.0,
                    totalRevenue = 0L,
                    totalAllocatedExpenses = 0L,
                    avgSellingPricePerKg = 0L,
                    costPerKg = 0L,
                    netProfitPerKg = 0L,
                    profitMarginPct = 0,
                    totalNetProfit = 0L
                )
            )
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Assistant Chat Messages & Status
    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "سلام و درود! من دستیار هوشمند گلخانه متصل به هوش مصنوعی Gemini هستم.\nمی‌توانید به زبان گفتاری بنویسید (مثلاً: «۵۰۰ کیلو توت‌فرنگی چیدیم کیلویی ۷۰ هزار تومن» یا «خرید سم قارچ‌کش ۱ میلیون و ۴۰۰ هزار تومان»)، سوالات باغبانی و اقلیم بپرسید یا تحلیل بهای تمام‌شده و سود هر کیلو را بخواهید."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun selectTab(tab: AppTab) {
        if (tab != AppTab.ADD) {
            _editingTransaction.value = null
        }
        _activeTab.value = tab
    }

    fun startEditingTransaction(transaction: Transaction) {
        _editingTransaction.value = transaction
        _activeTab.value = AppTab.ADD
    }

    fun cancelEditing() {
        _editingTransaction.value = null
        _activeTab.value = AppTab.TRANSACTIONS
    }

    // Crops
    fun addCustomCrop(cropName: String) {
        repository.addCustomCrop(cropName)
    }

    // Transactions
    fun addTransaction(
        type: TransactionType,
        amount: Long,
        date: String,
        category: String,
        description: String,
        cropType: String? = null
    ) {
        repository.addTransaction(
            type = type,
            amount = amount,
            date = date,
            category = category,
            description = description,
            cropType = cropType
        )
        _activeTab.value = AppTab.TRANSACTIONS
    }

    fun updateTransaction(updatedTx: Transaction) {
        repository.updateTransaction(updatedTx)
        _editingTransaction.value = null
        _activeTab.value = AppTab.TRANSACTIONS
    }

    fun deleteTransaction(id: String) {
        repository.deleteTransaction(id)
    }

    // Debts
    fun addDebt(
        name: String,
        type: DebtType,
        amount: Long,
        description: String,
        date: String,
        dueDate: String
    ) {
        repository.addDebt(
            name = name,
            type = type,
            amount = amount,
            description = description,
            date = date,
            dueDate = dueDate
        )
    }

    fun deleteDebt(id: String) {
        repository.deleteDebt(id)
    }

    fun registerDebtPayment(debtId: String, amount: Long, date: String, note: String) {
        repository.registerDebtPayment(debtId, amount, date, note)
    }

    // Harvest Logs
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
    ) {
        repository.addHarvestLog(
            date = date,
            cropType = cropType,
            totalHarvestWeight = totalHarvestWeight,
            grade1Weight = grade1Weight,
            grade1PricePerKg = grade1PricePerKg,
            grade2Weight = grade2Weight,
            grade2PricePerKg = grade2PricePerKg,
            totalRevenue = totalRevenue,
            note = note,
            autoCreateTx = autoCreateTx
        )
    }

    fun deleteHarvestLog(id: String) {
        repository.deleteHarvestLog(id)
    }

    // Custom Tags
    fun addCustomTag(type: TransactionType, tag: String) {
        repository.addCustomTag(type, tag)
    }

    // Security
    fun setPassword(password: String, question: String, answer: String): Boolean {
        return repository.setPassword(password, question, answer)
    }

    fun clearPassword(currentPassword: String): Boolean {
        return repository.clearPassword(currentPassword)
    }

    fun unlockWithPassword(password: String): Boolean {
        return repository.unlockWithPassword(password)
    }

    fun unlockWithRecovery(answer: String): Boolean {
        return repository.unlockWithRecovery(answer)
    }

    fun lockApp() {
        repository.lockApp()
    }

    // Backup
    fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(jsonStr: String): Boolean = repository.importBackupJson(jsonStr)

    fun clearAllData() {
        repository.clearAllData()
        _activeTab.value = AppTab.DASHBOARD
    }

    // AI Assistant Natural Language Processing with Real Gemini 3.5 Flash & Offline Fallback
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            isUser = true,
            text = userText
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                val context = buildGreenhouseContext()
                val result = GeminiAssistantService.queryAssistant(userText, context)

                if (result.isSuccess) {
                    val aiResp = result.getOrThrow()
                    val proposedAction = createProposedActionFromParsed(aiResp.parsedAction, userText)

                    val reply = ChatMessage(
                        id = "ai_${System.currentTimeMillis()}",
                        isUser = false,
                        text = aiResp.replyText,
                        proposedAction = proposedAction
                    )
                    _chatMessages.value = _chatMessages.value + reply
                } else {
                    // Fall back to robust offline natural language parsing
                    parseAndRespondOffline(userText)
                }
            } catch (e: Exception) {
                parseAndRespondOffline(userText)
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    private fun buildGreenhouseContext(): String {
        val metrics = summaryMetrics.value
        val cropsList = crops.value
        val econList = unitEconomicsList.value
        val recentHarvests = harvestLogs.value.take(3)
        val recentTxs = transactions.value.take(4)

        return buildString {
            appendLine("- کل درآمدهای ثبت‌شده: ${JalaliCalendar.formatCurrency(metrics.totalIncome)}")
            appendLine("- کل هزینه‌های ثبت‌شده: ${JalaliCalendar.formatCurrency(metrics.totalExpense)}")
            appendLine("- سود خالص کلی: ${JalaliCalendar.formatCurrency(metrics.netProfit)}")
            appendLine("- محصولات فعال در گلخانه: ${cropsList.joinToString("، ")}")
            if (econList.isNotEmpty()) {
                appendLine("- تحلیل بهای تمام‌شده و سود هر کیلو:")
                econList.forEach { e ->
                    appendLine("  * ${e.cropType}: وزن کل=${e.totalHarvestWeightKg}kg، بهای تمام‌شده هر کیلو=${e.costPerKg} تومان، میانگین فی فروش=${e.avgSellingPricePerKg} تومان، سود خالص هر کیلو=${e.netProfitPerKg} تومان (${e.profitMarginPct}%)")
                }
            }
            if (recentHarvests.isNotEmpty()) {
                appendLine("- آخرین بارهای برداشت شده:")
                recentHarvests.forEach { h ->
                    appendLine("  * ${h.cropType} (${h.date}): ${h.totalHarvestWeight}kg، عایدی=${h.totalRevenue} تومان")
                }
            }
        }
    }

    private fun createProposedActionFromParsed(parsed: GeminiParsedAction?, prompt: String): ProposedAction? {
        if (parsed == null) return null

        return when (parsed.actionType) {
            "CREATE_HARVEST" -> {
                val crop = if (!parsed.cropType.isNullOrBlank()) parsed.cropType else "توت‌فرنگی"
                val weight = if (parsed.grade1Weight > 0) parsed.grade1Weight else (if (parsed.totalRevenue > 0 && parsed.grade1Price > 0) (parsed.totalRevenue / parsed.grade1Price).toDouble() else 500.0)
                val price = if (parsed.grade1Price > 0) parsed.grade1Price else (if (crop == "توت‌فرنگی") 65000L else 28000L)
                val revenue = if (parsed.totalRevenue > 0) parsed.totalRevenue else (weight * price).toLong()

                ProposedAction(
                    title = "ثبت بار $crop (پیشنهاد هوش مصنوعی)",
                    details = "وزن: ${JalaliCalendar.toPersianDigits(weight.toInt())} کیلو | فی: ${JalaliCalendar.formatCurrency(price)} | جمع عایدی: ${JalaliCalendar.formatCurrency(revenue)}",
                    execute = {
                        addHarvestLog(
                            date = JalaliCalendar.getTodayGregorianString(),
                            cropType = crop,
                            totalHarvestWeight = weight,
                            grade1Weight = weight,
                            grade1PricePerKg = price,
                            grade2Weight = parsed.grade2Weight,
                            grade2PricePerKg = parsed.grade2Price,
                            totalRevenue = revenue,
                            note = parsed.note.ifEmpty { "ثبت هوشمند با دستیار جمینای" },
                            autoCreateTx = true
                        )
                    }
                )
            }
            "CREATE_TRANSACTION" -> {
                val isIncome = parsed.transactionType.equals("INCOME", ignoreCase = true)
                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                val amount = if (parsed.amount > 0) parsed.amount else 500000L
                val category = parsed.category.ifEmpty { if (isIncome) "فروش محصول" else "خرید نهاده‌ها و ملزومات" }
                val crop = parsed.cropType

                val titleType = if (isIncome) "درآمد" else "هزینه"
                ProposedAction(
                    title = "ثبت $titleType $category (پیشنهاد هوش مصنوعی)",
                    details = "مبلغ: ${JalaliCalendar.formatCurrency(amount)}" + (if (!crop.isNullOrBlank()) " | محصول: $crop" else ""),
                    execute = {
                        addTransaction(
                            type = type,
                            amount = amount,
                            date = JalaliCalendar.getTodayGregorianString(),
                            category = category,
                            description = parsed.note.ifEmpty { prompt },
                            cropType = crop
                        )
                    }
                )
            }
            else -> null
        }
    }

    private fun parseAndRespondOffline(input: String) {
        val clean = JalaliCalendar.toEnglishDigits(input).lowercase()
        val digits = Regex("\\d+").findAll(clean).map { it.value.toLong() }.toList()

        // Check for harvest mention
        if (clean.contains("برداشت") || clean.contains("کیلو") || clean.contains("چیدن") || clean.contains("فروش بار")) {
            val crop = when {
                clean.contains("توت") -> "توت‌فرنگی"
                clean.contains("خیار") -> "خیار"
                clean.contains("گوجه") -> "گوجه‌فرنگی"
                clean.contains("فلفل") -> "فلفل دلمه‌ای"
                clean.contains("بادمجان") -> "بادمجان"
                clean.contains("نشاء") -> "نشاء و قلمه"
                clean.contains("گل") -> "گل و گیاه زینتی"
                else -> "توت‌فرنگی"
            }
            val weight = if (digits.isNotEmpty()) digits[0].toDouble() else 350.0
            val price = if (digits.size >= 2) digits[1] else (if (crop == "توت‌فرنگی") 65000L else 28000L)
            val totalRev = (weight * price).toLong()

            val action = ProposedAction(
                title = "ثبت گزارش برداشت $crop (حالت آفلاین)",
                details = "وزن: ${JalaliCalendar.toPersianDigits(weight)} کیلو | فی: ${JalaliCalendar.formatCurrency(price)} | جمع: ${JalaliCalendar.formatCurrency(totalRev)}",
                execute = {
                    addHarvestLog(
                        date = JalaliCalendar.getTodayGregorianString(),
                        cropType = crop,
                        totalHarvestWeight = weight,
                        grade1Weight = weight,
                        grade1PricePerKg = price,
                        grade2Weight = 0.0,
                        grade2PricePerKg = 0L,
                        totalRevenue = totalRev,
                        note = "ثبت شده توسط دستیار آفلاین",
                        autoCreateTx = true
                    )
                }
            )

            val reply = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                isUser = false,
                text = "یک ثبت برداشت برای محصول «$crop» آماده کردم. با لمس دکمه زیر تایید فرمایید:",
                proposedAction = action
            )
            _chatMessages.value = _chatMessages.value + reply
            return
        }

        // Check for expense or income
        val isExpense = clean.contains("خرید") || clean.contains("هزینه") || clean.contains("کود") ||
                clean.contains("سم") || clean.contains("حقوق") || clean.contains("کارگر") ||
                clean.contains("قبض") || clean.contains("پلاستیک") || clean.contains("گاز") || clean.contains("بذر")

        val isIncome = clean.contains("فروش") || clean.contains("درآمد") || clean.contains("دریافت") || clean.contains("واریز")

        if (digits.isNotEmpty() && (isExpense || isIncome)) {
            val amount = digits.maxOrNull() ?: 100000L
            val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
            val targetCrop = when {
                clean.contains("توت") -> "توت‌فرنگی"
                clean.contains("خیار") -> "خیار"
                clean.contains("گوجه") -> "گوجه‌فرنگی"
                clean.contains("فلفل") -> "فلفل دلمه‌ای"
                else -> null
            }

            val category = if (type == TransactionType.INCOME) {
                when {
                    clean.contains("توت") -> "فروش محصول توت‌فرنگی"
                    clean.contains("خیار") -> "فروش محصول خیار"
                    clean.contains("گوجه") -> "فروش محصول گوجه‌فرنگی"
                    clean.contains("فلفل") -> "فروش فلفل دلمه‌ای"
                    else -> "سایر درآمدهای گلخانه"
                }
            } else {
                when {
                    clean.contains("کود") || clean.contains("سم") -> "خرید کود و سموم"
                    clean.contains("کارگر") || clean.contains("دستمزد") || clean.contains("حقوق") -> "حقوق و دستمزد کارگران"
                    clean.contains("بذر") || clean.contains("نشاء") -> "خرید بذر و نشاء"
                    clean.contains("قبض") || clean.contains("برق") || clean.contains("گاز") -> "قبوض انرژی (آب، برق، گاز)"
                    clean.contains("نایلون") || clean.contains("پلاستیک") -> "پوشش پلاستیک و نایلون گلخانه"
                    else -> "سایر هزینه‌های تولید"
                }
            }

            val typeTitle = if (type == TransactionType.INCOME) "درآمد" else "هزینه"
            val action = ProposedAction(
                title = "ثبت $typeTitle $category",
                details = "مبلغ: ${JalaliCalendar.formatCurrency(amount)} | شرح: $input" + if (targetCrop != null) " | محصول: $targetCrop" else "",
                execute = {
                    addTransaction(
                        type = type,
                        amount = amount,
                        date = JalaliCalendar.getTodayGregorianString(),
                        category = category,
                        description = input,
                        cropType = targetCrop
                    )
                }
            )

            val reply = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                isUser = false,
                text = "یک تراکنش $typeTitle برای دسته‌بندی «$category» تنظیم کردم. جهت اعمال در دفاتر، دکمه تایید را لمس کنید:",
                proposedAction = action
            )
            _chatMessages.value = _chatMessages.value + reply
            return
        }

        // Check for unit economics / cost per kg inquiry
        if (clean.contains("بهای تمام") || clean.contains("تمام شده") || clean.contains("هر کیلو") || clean.contains("کیلو چند")) {
            val econ = unitEconomicsList.value.firstOrNull()
            val text = if (econ != null && econ.totalHarvestWeightKg > 0) {
                "📊 تحلیل بهای تمام‌شده محصول «${econ.cropType}»:\n\n" +
                        "• کل بار برداشت شده: ${JalaliCalendar.toPersianDigits(econ.totalHarvestWeightKg.toInt())} کیلوگرم\n" +
                        "• بهای تمام‌شده هر کیلو: ${JalaliCalendar.formatCurrency(econ.costPerKg)}\n" +
                        "• میانگین فی فروش هر کیلو: ${JalaliCalendar.formatCurrency(econ.avgSellingPricePerKg)}\n" +
                        "• سود خالص در هر کیلو: ${JalaliCalendar.formatCurrency(econ.netProfitPerKg)} (${JalaliCalendar.toPersianDigits(econ.profitMarginPct)}٪)"
            } else {
                "برای محاسبه دقیق بهای تمام‌شده، کافیست بارهای برداشت شده و فاکتورهای هزینه‌ای مربوط به محصول (مثل توت‌فرنگی یا خیار) را ثبت نمایید."
            }
            val reply = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                isUser = false,
                text = text
            )
            _chatMessages.value = _chatMessages.value + reply
            return
        }

        // General summary inquiry
        if (clean.contains("وضعیت") || clean.contains("خلاصه") || clean.contains("سود") || clean.contains("چقدر")) {
            val metrics = summaryMetrics.value
            val text = "📊 خلاصه وضعیت مالی گلخانه:\n\n" +
                    "• کل درآمدها: ${JalaliCalendar.formatCurrency(metrics.totalIncome)}\n" +
                    "• کل هزینه‌ها: ${JalaliCalendar.formatCurrency(metrics.totalExpense)}\n" +
                    "• سود خالص: ${JalaliCalendar.formatCurrency(metrics.netProfit)}\n" +
                    "• طلب‌های معوق: ${JalaliCalendar.formatCurrency(metrics.activeReceivable)}"
            val reply = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                isUser = false,
                text = text
            )
            _chatMessages.value = _chatMessages.value + reply
            return
        }

        // Fallback friendly reply
        val fallback = ChatMessage(
            id = "ai_${System.currentTimeMillis()}",
            isUser = false,
            text = "در خدمت شما هستم. برای ثبت سریع یا تحلیل مالی بنویسید:\n" +
                    "«برداشت ۴۵۰ کیلو توت‌فرنگی با فی ۶۵۰۰۰ تومان» یا «چگونه مصرف کود فسفر و بهای تمام‌شده را بهینه کنم؟»"
        )
        _chatMessages.value = _chatMessages.value + fallback
    }
}
