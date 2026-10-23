package com.example.greenhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.greenhouse.data.model.Constants
import com.example.greenhouse.data.model.HarvestLog
import com.example.greenhouse.ui.components.PersianDatePickerDialog
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HarvestManagerScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val harvestLogs by viewModel.harvestLogs.collectAsState()
    val crops by viewModel.crops.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<HarvestLog?>(null) }

    // Summary calculations
    val (totalHarvested, totalSold, totalRevenue, avgPrice) = remember(harvestLogs) {
        var hWeight = 0.0
        var sWeight = 0.0
        var rev = 0L
        harvestLogs.forEach {
            hWeight += it.totalHarvestWeight
            sWeight += (it.grade1Weight + it.grade2Weight)
            rev += it.totalRevenue
        }
        val avg = if (sWeight > 0) (rev / sWeight).toLong() else 0L
        Quad(hWeight, sWeight, rev, avg)
    }

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف رکورد برداشت") },
            text = { Text("آیا از حذف این گزارش برداشت اطمینان دارید؟ در صورت اتصال به درآمد، تراکنش متصل نیز حذف خواهد شد.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate?.let { viewModel.deleteHarvestLog(it.id) }
                        deleteCandidate = null
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_harvest")
                ) {
                    Text("حذف", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showAddDialog) {
        AddHarvestLogDialog(
            availableCrops = crops,
            onAddCrop = { viewModel.addCustomCrop(it) },
            onDismiss = { showAddDialog = false },
            onSubmit = { date, crop, totalW, g1W, g1P, g2W, g2P, rev, note, autoTx ->
                viewModel.addHarvestLog(date, crop, totalW, g1W, g1P, g2W, g2P, rev, note, autoTx)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0D9488),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_harvest")
            ) {
                Icon(Icons.Default.Add, contentDescription = "ثبت بار جدید")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "دفتر آمار برداشت و فروش بار",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.Scale, contentDescription = null, tint = Color(0xFF0D9488))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(
                                label = "کل برداشت",
                                value = "${JalaliCalendar.toPersianDigits(totalHarvested.toInt())} ک",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "کل فروش",
                                value = "${JalaliCalendar.toPersianDigits(totalSold.toInt())} ک",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "میانگین فی",
                                value = JalaliCalendar.formatCurrency(avgPrice),
                                modifier = Modifier.weight(1.3f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald50,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "کل درآمد فروش این فصل:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Emerald900
                                )
                                Text(
                                    text = JalaliCalendar.formatCurrency(totalRevenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }
            }

            // List of Logs
            if (harvestLogs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("هنوز باری در این دفتر ثبت نشده است.", color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                            ) {
                                Text("ثبت اولین بار برداشت شده")
                            }
                        }
                    }
                }
            } else {
                items(harvestLogs, key = { it.id }) { log ->
                    HarvestItemCard(
                        log = log,
                        onDelete = { deleteCandidate = log }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF3F4F6),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun HarvestItemCard(
    log: HarvestLog,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("harvest_card_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald100,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald800, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = "محصول: ${log.cropType}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("btn_delete_harvest_${log.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Breakdown
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "درجه ۱: ${JalaliCalendar.toPersianDigits(log.grade1Weight)} ک (فی: ${JalaliCalendar.toPersianDigits(log.grade1PricePerKg)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (log.grade2Weight > 0) {
                    Text(
                        text = "| درجه ۲: ${JalaliCalendar.toPersianDigits(log.grade2Weight)} ک",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = JalaliCalendar.formatToJalali(log.date, "medium"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Text(
                    text = JalaliCalendar.formatCurrency(log.totalRevenue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }

            if (log.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = log.note, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHarvestLogDialog(
    availableCrops: List<String>,
    onAddCrop: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        date: String,
        crop: String,
        totalHarvestWeight: Double,
        grade1Weight: Double,
        grade1Price: Long,
        grade2Weight: Double,
        grade2Price: Long,
        totalRevenue: Long,
        note: String,
        autoCreateTx: Boolean
    ) -> Unit
) {
    var dateStr by remember { mutableStateOf(JalaliCalendar.getTodayGregorianString()) }
    var cropType by remember(availableCrops) { 
        mutableStateOf(if (availableCrops.contains("توت‌فرنگی")) "توت‌فرنگی" else availableCrops.firstOrNull() ?: "خیار") 
    }
    var totalHarvestWeightStr by remember { mutableStateOf("") }
    var grade1WeightStr by remember { mutableStateOf("") }
    var grade1PriceStr by remember { mutableStateOf("") }
    var grade2WeightStr by remember { mutableStateOf("") }
    var grade2PriceStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var autoCreateTx by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNewCropDialog by remember { mutableStateOf(false) }
    var newCropNameInput by remember { mutableStateOf("") }

    val g1W = JalaliCalendar.toEnglishDigits(grade1WeightStr).toDoubleOrNull() ?: 0.0
    val g1P = JalaliCalendar.toEnglishDigits(grade1PriceStr).toLongOrNull() ?: 0L
    val g2W = JalaliCalendar.toEnglishDigits(grade2WeightStr).toDoubleOrNull() ?: 0.0
    val g2P = JalaliCalendar.toEnglishDigits(grade2PriceStr).toLongOrNull() ?: 0L
    val calculatedRevenue = (g1W * g1P).toLong() + (g2W * g2P).toLong()

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

    if (showNewCropDialog) {
        AlertDialog(
            onDismissRequest = { showNewCropDialog = false },
            title = { Text("تعریف نوع محصول جدید") },
            text = {
                OutlinedTextField(
                    value = newCropNameInput,
                    onValueChange = { newCropNameInput = it },
                    placeholder = { Text("نام محصول (مثال: توت‌فرنگی، فلفل، رز)...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_add_crop_in_harvest")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCropNameInput.trim()
                        if (clean.isNotEmpty()) {
                            onAddCrop(clean)
                            cropType = clean
                            newCropNameInput = ""
                            showNewCropDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("ثبت محصول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCropDialog = false }) { Text("انصراف") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت بار و برداشت روزانه") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Crop Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نوع محصول:", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { showNewCropDialog = true }, contentPadding = PaddingValues(0.dp)) {
                        Text("+ افزودن محصول", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableCrops.forEach { crop ->
                        FilterChip(
                            selected = cropType == crop,
                            onClick = { cropType = crop },
                            label = { Text(crop, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chip_crop_${crop}")
                        )
                    }
                }

                // Date Picker Button
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(JalaliCalendar.formatToJalali(dateStr, "medium"), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald600)
                    }
                }

                // Grade 1
                OutlinedTextField(
                    value = grade1WeightStr,
                    onValueChange = { grade1WeightStr = it },
                    label = { Text("وزن درجه ۱ (کیلو)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_g1_weight")
                )
                OutlinedTextField(
                    value = grade1PriceStr,
                    onValueChange = { grade1PriceStr = it },
                    label = { Text("قیمت هر کیلو درجه ۱ (تومان)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_g1_price")
                )

                // Grade 2
                OutlinedTextField(
                    value = grade2WeightStr,
                    onValueChange = { grade2WeightStr = it },
                    label = { Text("وزن درجه ۲ (کیلو - اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grade2PriceStr,
                    onValueChange = { grade2PriceStr = it },
                    label = { Text("قیمت هر کیلو درجه ۲ (تومان)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Revenue Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("درآمد کل محاسبه شده:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            JalaliCalendar.formatCurrency(calculatedRevenue),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Auto Create Ledger Tx
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoCreateTx,
                        onCheckedChange = { autoCreateTx = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ثبت خودکار در درآمدها و دفاتر", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات یا یادداشت خریدار") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totW = (g1W + g2W).coerceAtLeast(1.0)
                    onSubmit(dateStr, cropType, totW, g1W, g1P, g2W, g2P, calculatedRevenue, note, autoCreateTx)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                modifier = Modifier.testTag("btn_submit_harvest")
            ) {
                Text("ثبت بار")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
