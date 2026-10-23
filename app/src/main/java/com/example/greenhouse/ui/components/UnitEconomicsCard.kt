package com.example.greenhouse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenhouse.data.model.CropUnitEconomics
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.util.JalaliCalendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnitEconomicsCard(
    economicsList: List<CropUnitEconomics>,
    cropsList: List<String>,
    onAddCrop: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCropName by remember(cropsList) {
        mutableStateOf(cropsList.firstOrNull() ?: "توت‌فرنگی")
    }

    var showAddCropDialog by remember { mutableStateOf(false) }
    var newCropInput by remember { mutableStateOf("") }

    // Find economics for selected crop or create placeholder
    val currentEcon = economicsList.find { it.cropType == selectedCropName } ?: CropUnitEconomics(
        cropType = selectedCropName,
        totalHarvestWeightKg = 0.0,
        totalRevenue = 0L,
        totalAllocatedExpenses = 0L,
        avgSellingPricePerKg = 0L,
        costPerKg = 0L,
        netProfitPerKg = 0L,
        profitMarginPct = 0,
        totalNetProfit = 0L
    )

    if (showAddCropDialog) {
        AlertDialog(
            onDismissRequest = { showAddCropDialog = false },
            title = { Text("افزودن نوع محصول جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نام و گونه محصول را وارد نمایید (مثال: توت‌فرنگی، بادمجان، گل رز):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newCropInput,
                        onValueChange = { newCropInput = it },
                        placeholder = { Text("نام محصول...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_crop")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCropInput.trim()
                        if (clean.isNotEmpty()) {
                            onAddCrop(clean)
                            selectedCropName = clean
                            newCropInput = ""
                            showAddCropDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    modifier = Modifier.testTag("btn_confirm_add_crop")
                ) {
                    Text("افزودن محصول")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCropDialog = false }) { Text("انصراف") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_unit_economics")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFDF2F8),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color(0xFFDB2777),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "بهای تمام‌شده هر کیلو محصول (Unit Economics)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "تحلیل هزینه تولید و سود خالص هر کیلوگرم به تفکیک محصول",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Crop Filter Chips & "+ افزودن محصول" button (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cropsList.forEach { crop ->
                    val isSelected = crop == selectedCropName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCropName = crop },
                        label = { Text(crop, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald100,
                            selectedLabelColor = Emerald900
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Add Custom Crop Chip
                AssistChip(
                    onClick = { showAddCropDialog = true },
                    label = { Text("+ محصول جدید", style = MaterialTheme.typography.labelSmall, color = Emerald700) },
                    shape = RoundedCornerShape(10.dp),
                    colors = AssistChipDefaults.assistChipColors(containerColor = Emerald50),
                    modifier = Modifier.testTag("btn_add_crop_chip")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Metrics for Selected Crop
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF9FAFB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Row 1: Selling Price vs Cost per Kg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cost per Kg Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "بهای تمام‌شده هر کیلو:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentEcon.costPerKg > 0) JalaliCalendar.formatCurrency(currentEcon.costPerKg) else "۰ تومان",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "هزینه تمام فاکتورها ÷ وزن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Avg Selling Price per Kg Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald50,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "میانگین فی فروش هر کیلو:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentEcon.avgSellingPricePerKg > 0) JalaliCalendar.formatCurrency(currentEcon.avgSellingPricePerKg) else "۰ تومان",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "فی میانگین بار فروخته شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Net Profit per Kg & Margin Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentEcon.netProfitPerKg >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "سود خالص در هر کیلو $selectedCropName:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (currentEcon.netProfitPerKg >= 0) Emerald900 else Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = JalaliCalendar.formatCurrency(currentEcon.netProfitPerKg),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentEcon.netProfitPerKg >= 0) Emerald700 else ExpenseRed
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentEcon.netProfitPerKg >= 0) Emerald600 else ExpenseRed
                            ) {
                                Text(
                                    text = "مارجین: ${JalaliCalendar.toPersianDigits(currentEcon.profitMarginPct)}٪",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Harvest and Financial Subtotals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "کل برداشت محصول: ${JalaliCalendar.toPersianDigits(currentEcon.totalHarvestWeightKg.toInt())} کیلوگرم",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "سود کل دوره: ${JalaliCalendar.formatCurrency(currentEcon.totalNetProfit)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentEcon.totalNetProfit >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Advisory Note / Breakeven Point
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = DebtAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (currentEcon.costPerKg > 0) {
                            "نقطه سر‌به‌سر: برای جلوگیری از زیان، محصول «$selectedCropName» نباید کمتر از ${JalaliCalendar.formatCurrency(currentEcon.costPerKg)} در هر کیلوگرم به فروش برسد."
                        } else {
                            "نکته: با ثبت بارهای چیده شده و هزینه‌های مربوط به $selectedCropName، بهای تمام‌شده هر کیلو به شکل خودکار محاسبه می‌شود."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
