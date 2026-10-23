package com.example.greenhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greenhouse.data.model.TransactionType
import com.example.greenhouse.ui.components.PersianDatePickerDialog
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val editingTx by viewModel.editingTransaction.collectAsState()
    val incomeTags by viewModel.incomeTags.collectAsState()
    val expenseTags by viewModel.expenseTags.collectAsState()
    val crops by viewModel.crops.collectAsState()

    var type by remember(editingTx) {
        mutableStateOf(editingTx?.type ?: TransactionType.INCOME)
    }

    var selectedCrop by remember(editingTx) {
        mutableStateOf(editingTx?.cropType)
    }

    var amountStr by remember(editingTx) {
        mutableStateOf(editingTx?.amount?.toString() ?: "")
    }

    var dateStr by remember(editingTx) {
        mutableStateOf(editingTx?.date?.split("T")?.getOrNull(0) ?: JalaliCalendar.getTodayGregorianString())
    }

    var category by remember(editingTx, type) {
        mutableStateOf(
            editingTx?.category ?: if (type == TransactionType.INCOME) {
                incomeTags.firstOrNull() ?: ""
            } else {
                expenseTags.firstOrNull() ?: ""
            }
        )
    }

    var description by remember(editingTx) {
        mutableStateOf(editingTx?.description ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val currentTags = if (type == TransactionType.INCOME) incomeTags else expenseTags

    val amountLong = remember(amountStr) {
        val clean = JalaliCalendar.toEnglishDigits(amountStr).replace(Regex("[^0-9]"), "")
        clean.toLongOrNull() ?: 0L
    }

    val amountInWords = remember(amountLong) {
        if (amountLong > 0) JalaliCalendar.numberToPersianWords(amountLong) + " تومان" else ""
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            initialDateStr = dateStr,
            onDateSelected = {
                dateStr = it
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("افزودن دسته‌بندی جدید") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("عنوان تگ، مثلا: خرید کود ریزمغذی") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_tag")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newTagInput.trim()
                        if (clean.isNotEmpty()) {
                            viewModel.addCustomTag(type, clean)
                            category = clean
                            newTagInput = ""
                            showAddTagDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("ثبت تگ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (editingTx != null) "ویرایش تراکنش جاری" else "ثبت تراکنش مالی جدید",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                // Type Toggle: Income vs Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = TransactionType.INCOME
                                if (!incomeTags.contains(category)) {
                                    category = incomeTags.firstOrNull() ?: ""
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "درآمد (واریز)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (type == TransactionType.INCOME) Color.White else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = TransactionType.EXPENSE
                                if (!expenseTags.contains(category)) {
                                    category = expenseTags.firstOrNull() ?: ""
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tab_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هزینه (برداشت)",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (type == TransactionType.EXPENSE) Color.White else TextSecondary
                        )
                    }
                }

                // Amount Input
                Column {
                    Text(
                        text = "مبلغ به تومان:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            errorMessage = ""
                        },
                        placeholder = { Text("مثال: ۱۲۵۰۰۰۰") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                        )
                    )
                    if (amountInWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "معادل: $amountInWords",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (type == TransactionType.INCOME) Emerald700 else ExpenseRed
                        )
                    }
                }

                // Shamsi Date Selection
                Column {
                    Text(
                        text = "تاریخ تراکنش (شمسی):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_pick_date")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = JalaliCalendar.formatToJalali(dateStr, "long"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "انتخاب تاریخ",
                                tint = Emerald600
                            )
                        }
                    }
                }

                // Category Tag Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دسته‌بندی یا تگ:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = { showAddTagDialog = true },
                            modifier = Modifier.testTag("btn_add_custom_tag")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تگ سفارشی", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTags.forEach { tag ->
                            val isSelected = tag == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = tag },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (type == TransactionType.INCOME) Emerald100 else Color(0xFFFEE2E2),
                                    selectedLabelColor = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Description
                Column {
                    Text(
                        text = "توضیحات و شرح فاکتور (اختیاری):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("مثال: فاکتور خرید ۵ کیسه کود پتاسیم از فروشگاه کشاورزی") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_description"),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Crop Assignment
                Column {
                    Text(
                        text = "مربوط به کدام محصول؟ (جهت محاسبه دقیق بهای تمام‌شده هر کیلو):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCrop == null,
                            onClick = { selectedCrop = null },
                            label = { Text("عمومی کل گلخانه", style = MaterialTheme.typography.labelSmall) }
                        )
                        crops.forEach { crop ->
                            FilterChip(
                                selected = selectedCrop == crop,
                                onClick = { selectedCrop = crop },
                                label = { Text(crop, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed
                    )
                }

                // Submit & Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (amountLong <= 0) {
                                errorMessage = "لطفاً مبلغ معتبری وارد نمایید."
                                return@Button
                            }
                            if (category.isBlank()) {
                                errorMessage = "لطفاً دسته‌بندی را مشخص کنید."
                                return@Button
                            }

                            if (editingTx != null) {
                                viewModel.updateTransaction(
                                    editingTx!!.copy(
                                        type = type,
                                        amount = amountLong,
                                        date = dateStr,
                                        category = category,
                                        description = description,
                                        cropType = selectedCrop
                                    )
                                )
                            } else {
                                viewModel.addTransaction(
                                    type = type,
                                    amount = amountLong,
                                    date = dateStr,
                                    category = category,
                                    description = description,
                                    cropType = selectedCrop
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_submit_tx")
                    ) {
                        Text(
                            text = if (editingTx != null) "ذخیره تغییرات" else "ثبت نهایی تراکنش",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }

                    if (editingTx != null) {
                        OutlinedButton(
                            onClick = { viewModel.cancelEditing() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("انصراف")
                        }
                    }
                }
            }
        }
    }
}
