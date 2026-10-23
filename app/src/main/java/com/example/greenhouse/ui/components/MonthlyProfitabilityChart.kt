package com.example.greenhouse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.util.JalaliCalendar
import kotlin.math.max

data class MonthProfitData(
    val monthIndex: Int,
    val monthName: String,
    val income: Long,
    val expense: Long
) {
    val netProfit: Long get() = income - expense
    val profitMarginPct: Int
        get() = if (income > 0) ((netProfit * 100) / income).toInt() else 0
}

@Composable
fun MonthlyProfitabilityChart(
    incomeArr: LongArray,
    expenseArr: LongArray,
    currentJalaliYear: Int,
    currentMonthIndex: Int,
    modifier: Modifier = Modifier
) {
    // 1. Prepare 12 months data
    val monthlyList = remember(incomeArr, expenseArr) {
        (0 until 12).map { i ->
            MonthProfitData(
                monthIndex = i,
                monthName = JalaliCalendar.PERSIAN_MONTHS[i],
                income = incomeArr.getOrElse(i) { 0L },
                expense = expenseArr.getOrElse(i) { 0L }
            )
        }
    }

    // Selected month for interactive Recharts-like Tooltip
    var selectedMonthIndex by remember { mutableStateOf(currentMonthIndex.coerceIn(0, 11)) }
    val selectedData = monthlyList[selectedMonthIndex]

    // Max value for scaling chart bars
    val maxBarValue = remember(monthlyList) {
        var m = 1L
        monthlyList.forEach {
            m = max(m, max(it.income, it.expense))
        }
        m
    }

    // Profitability analytics
    val totalYearIncome = remember(monthlyList) { monthlyList.sumOf { it.income } }
    val totalYearExpense = remember(monthlyList) { monthlyList.sumOf { it.expense } }
    val totalYearNet = totalYearIncome - totalYearExpense
    val overallMargin = if (totalYearIncome > 0) ((totalYearNet * 100) / totalYearIncome).toInt() else 0
    val bestMonth = remember(monthlyList) { monthlyList.maxByOrNull { it.netProfit } }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_recharts_bar_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "نمودار سودآوری و تراز مالی ماهانه (Recharts)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "مقایسه درآمد فروش بار در برابر هزینه‌ها و سود خالص سال ${JalaliCalendar.toPersianDigits(currentJalaliYear)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalYearNet >= 0) Emerald100 else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "حاشیه سود: ${JalaliCalendar.toPersianDigits(overallMargin)}٪",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalYearNet >= 0) Emerald900 else ExpenseRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "درآمد فروش",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Expense Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "هزینه‌های تولید",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Net Profit Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BlueAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سود خالص",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Recharts Interactive Dual-Bar Chart Canvas
            val chartHeight = 160.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 40.dp)
                    .background(Color(0xFFFAFCFA), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFF1F5F2), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Horizontal guideline grid lines (CartesianGrid)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Divider(
                            color = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            thickness = 0.8.dp
                        )
                    }
                }

                // 12 Monthly Dual Bars (Scrollable row)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyList.forEach { mData ->
                        val isSelected = mData.monthIndex == selectedMonthIndex
                        val isCurrentMonth = mData.monthIndex == currentMonthIndex

                        val incRatio = (mData.income.toFloat() / maxBarValue).coerceIn(0.02f, 1f)
                        val expRatio = (mData.expense.toFloat() / maxBarValue).coerceIn(0.02f, 1f)

                        Column(
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { selectedMonthIndex = mData.monthIndex }
                                .padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Net profit mini tag if month has data
                            if (mData.income > 0 || mData.expense > 0) {
                                val isProfitable = mData.netProfit >= 0
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isProfitable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = if (isProfitable) "▲" else "▼",
                                        fontSize = 8.sp,
                                        color = if (isProfitable) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Dual Bars: Income (Green) and Expense (Red)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(chartHeight - 40.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(incRatio)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (mData.income > 0) IncomeGreen else Color(0xFFE5E7EB))
                                )

                                // Expense Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(expRatio)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (mData.expense > 0) ExpenseRed else Color(0xFFE5E7EB))
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Month Label
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Emerald600 else (if (isCurrentMonth) Emerald100 else Color.Transparent)
                            ) {
                                Text(
                                    text = mData.monthName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else (if (isCurrentMonth) Emerald800 else TextSecondary),
                                    fontWeight = if (isSelected || isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts Interactive Tooltip Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recharts_tooltip_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                            Text(
                                text = "جزئیات سودآوری ماه ${selectedData.monthName} ${JalaliCalendar.toPersianDigits(currentJalaliYear)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Emerald950
                            )
                        }

                        val isPositive = selectedData.netProfit >= 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPositive) Color(0xFFBBF7D0) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (isPositive) "سودده" else "زیان‌ده",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPositive) Emerald900 else ExpenseRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("درآمد فروش بار:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = JalaliCalendar.formatCurrency(selectedData.income),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Column {
                            Text("کل هزینه‌ها:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = JalaliCalendar.formatCurrency(selectedData.expense),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }

                        Column {
                            Text("سود خالص:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = JalaliCalendar.formatCurrency(selectedData.netProfit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedData.netProfit >= 0) BlueAccent else ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profitability Performance Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Best Month Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "سودآورترین ماه:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bestMonth?.let { "${it.monthName} (${JalaliCalendar.formatCurrency(it.netProfit)})" } ?: "نامشخص",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Average Monthly Margin
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "میانگین سود ماهانه:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = JalaliCalendar.formatCurrency(totalYearNet / 12),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalYearNet >= 0) Emerald700 else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}
