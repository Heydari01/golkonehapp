package com.example.greenhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greenhouse.data.model.Debt
import com.example.greenhouse.data.model.DebtStatus
import com.example.greenhouse.data.model.DebtType
import com.example.greenhouse.ui.components.PersianDatePickerDialog
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

@Composable
fun DebtsManagerScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val debts by viewModel.debts.collectAsState()

    var filterType by remember { mutableStateOf<DebtType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentTargetDebt by remember { mutableStateOf<Debt?>(null) }
    var deleteCandidate by remember { mutableStateOf<Debt?>(null) }

    // Summary calculations
    val (totalDebtor, totalCreditor) = remember(debts) {
        val debtorRemaining = debts.filter { it.type == DebtType.DEBTOR && it.status == DebtStatus.PENDING }
            .sumOf { it.remainingAmount }
        val creditorRemaining = debts.filter { it.type == DebtType.CREDITOR && it.status == DebtStatus.PENDING }
            .sumOf { it.remainingAmount }
        debtorRemaining to creditorRemaining
    }

    val filteredDebts = remember(debts, filterType) {
        if (filterType == null) debts else debts.filter { it.type == filterType }
    }

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف طرف حساب") },
            text = { Text("آیا از حذف «${deleteCandidate?.name}» از دفتر بدهی و طلب اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate?.let { viewModel.deleteDebt(it.id) }
                        deleteCandidate = null
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_debt")
                ) {
                    Text("حذف", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("انصراف") } }
        )
    }

    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { name, type, amt, desc, date, dueDate ->
                viewModel.addDebt(name, type, amt, desc, date, dueDate)
                showAddDialog = false
            }
        )
    }

    if (paymentTargetDebt != null) {
        RegisterPaymentDialog(
            debt = paymentTargetDebt!!,
            onDismiss = { paymentTargetDebt = null },
            onSubmit = { amt, date, note ->
                viewModel.registerDebtPayment(paymentTargetDebt!!.id, amt, date, note)
                paymentTargetDebt = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DebtAmber,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_debt")
            ) {
                Icon(Icons.Default.Add, contentDescription = "ثبت طرف حساب جدید")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Summary Cards Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("طلب‌های ما (بدهکاران)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(totalDebtor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بدهی‌های ما (بستانکاران)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(totalCreditor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = { Text("همه (${JalaliCalendar.toPersianDigits(debts.size)})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = filterType == DebtType.DEBTOR,
                    onClick = { filterType = DebtType.DEBTOR },
                    label = { Text("طلبکاری‌ها") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = filterType == DebtType.CREDITOR,
                    onClick = { filterType = DebtType.CREDITOR },
                    label = { Text("بدهکاری‌ها") },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredDebts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("موردی در این دسته ثبت نشده است.", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            onRegisterPayment = { paymentTargetDebt = debt },
                            onDelete = { deleteCandidate = debt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItemCard(
    debt: Debt,
    onRegisterPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val isDebtor = debt.type == DebtType.DEBTOR
    val isSettled = debt.status == DebtStatus.PAID
    val badgeColor = if (isDebtor) IncomeGreen else ExpenseRed

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("debt_card_${debt.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSettled) Emerald100 else (if (isDebtor) Emerald50 else Color(0xFFFEF2F2))
                    ) {
                        Text(
                            text = if (isSettled) "تسویه شده" else (if (isDebtor) "به ما بدهکار است" else "ما بدهکاریم"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSettled) Emerald800 else badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amounts Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "کل مبلغ: ${JalaliCalendar.formatCurrency(debt.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "مانده: ${JalaliCalendar.formatCurrency(debt.remainingAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSettled) TextMuted else badgeColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Repayment Progress Bar
            val progress = if (debt.amount > 0) (debt.totalPaid.toFloat() / debt.amount).coerceIn(0f, 1f) else 1f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (isSettled) Emerald600 else badgeColor,
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سررسید: ${JalaliCalendar.formatToJalali(debt.dueDate, "medium")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                if (!isSettled) {
                    Button(
                        onClick = onRegisterPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_pay_${debt.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت واریز / قسط", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, type: DebtType, amount: Long, description: String, date: String, dueDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DebtType.DEBTOR) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(JalaliCalendar.getTodayGregorianString()) }
    var dueDateStr by remember { mutableStateOf(JalaliCalendar.getTodayGregorianString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountLong = remember(amountStr) {
        JalaliCalendar.toEnglishDigits(amountStr).toLongOrNull() ?: 0L
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            initialDateStr = dueDateStr,
            onDateSelected = {
                dueDateStr = it
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت بدهکاری یا طلبکاری جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == DebtType.DEBTOR,
                        onClick = { type = DebtType.DEBTOR },
                        label = { Text("طلب ما (به ما بدهکار است)") }
                    )
                    FilterChip(
                        selected = type == DebtType.CREDITOR,
                        onClick = { type = DebtType.CREDITOR },
                        label = { Text("بدهی ما (ما به او بدهکاریم)") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام شخص یا طرف حساب") },
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_name")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ کل به تومان") },
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_amount")
                )

                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تاریخ سررسید: ${JalaliCalendar.formatToJalali(dueDateStr, "medium")}")
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald600)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("شرح و شرایط پرداخت") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && amountLong > 0) {
                        onSubmit(name, type, amountLong, description, dateStr, dueDateStr)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DebtAmber),
                modifier = Modifier.testTag("btn_submit_debt")
            ) {
                Text("ثبت حساب")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun RegisterPaymentDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onSubmit: (amount: Long, date: String, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("قسط / واریزی") }
    var dateStr by remember { mutableStateOf(JalaliCalendar.getTodayGregorianString()) }

    val amountLong = remember(amountStr) {
        JalaliCalendar.toEnglishDigits(amountStr).toLongOrNull() ?: 0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداختی برای ${debt.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("مانده بدهی جاری: ${JalaliCalendar.formatCurrency(debt.remainingAmount)}", color = DebtAmber, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ پرداختی این قسط (تومان)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_payment_amount")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("شرح و شماره چک / رسید") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "توجه: این مبلغ به صورت خودکار به عنوان تراکنش مالی در دفاتر ثبت خواهد شد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald700
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountLong > 0) {
                        onSubmit(amountLong, dateStr, note)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier.testTag("btn_confirm_payment")
            ) {
                Text("ثبت پرداختی")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
