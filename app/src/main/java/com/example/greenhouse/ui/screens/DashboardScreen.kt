package com.example.greenhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greenhouse.data.model.TransactionType
import com.example.greenhouse.ui.components.MetricCard
import com.example.greenhouse.ui.components.MetricType
import com.example.greenhouse.ui.components.MonthlyProfitabilityChart
import com.example.greenhouse.ui.components.UnitEconomicsCard
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.AppTab
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

@Composable
fun DashboardScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.summaryMetrics.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val harvestLogs by viewModel.harvestLogs.collectAsState()
    val unitEconomicsList by viewModel.unitEconomicsList.collectAsState()
    val crops by viewModel.crops.collectAsState()

    // Monthly breakdown data for current Persian year
    val todayJalali = remember { JalaliCalendar.getTodayJalali() }
    val currentJy = todayJalali.jy

    val monthlyData = remember(transactions) {
        val monthIncome = LongArray(12)
        val monthExpense = LongArray(12)

        transactions.forEach { t ->
            val jalali = JalaliCalendar.parseDateToJalali(t.date)
            if (jalali.jy == currentJy && jalali.jm in 1..12) {
                if (t.type == TransactionType.INCOME) {
                    monthIncome[jalali.jm - 1] += t.amount
                } else {
                    monthExpense[jalali.jm - 1] += t.amount
                }
            }
        }
        monthIncome to monthExpense
    }

    // Category breakdown
    val categoryBreakdown = remember(transactions) {
        val expenseCatMap = mutableMapOf<String, Long>()
        val incomeCatMap = mutableMapOf<String, Long>()

        transactions.forEach { t ->
            if (t.type == TransactionType.EXPENSE) {
                expenseCatMap[t.category] = (expenseCatMap[t.category] ?: 0L) + t.amount
            } else {
                incomeCatMap[t.category] = (incomeCatMap[t.category] ?: 0L) + t.amount
            }
        }
        expenseCatMap.toList().sortedByDescending { it.second }.take(5) to
                incomeCatMap.toList().sortedByDescending { it.second }.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "کل درآمدهای گلخانه",
                    amount = metrics.totalIncome,
                    type = MetricType.INCOME,
                    subtitle = "مجموع فروش خیار، گوجه و صیفی‌جات"
                )
                MetricCard(
                    title = "کل هزینه‌های تولید",
                    amount = metrics.totalExpense,
                    type = MetricType.EXPENSE,
                    subtitle = "مجموع بذر، کود، کارگر و انرژی"
                )
                MetricCard(
                    title = "سود خالص گلخانه",
                    amount = metrics.netProfit,
                    type = MetricType.NET,
                    subtitle = "تفاوت درآمد و هزینه‌ها"
                )
                MetricCard(
                    title = "کل مطالبات باقی‌مانده",
                    amount = metrics.activeReceivable,
                    type = MetricType.DEBT,
                    subtitle = "طلب‌های تسویه نشده از خریداران"
                )
            }
        }

        // 2. Quick Actions Row
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "دسترسی و ثبت سریع",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.selectTab(AppTab.ADD) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_add_tx")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تراکنش", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { viewModel.selectTab(AppTab.HARVEST) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_harvest")
                        ) {
                            Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("برداشت", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { viewModel.selectTab(AppTab.DEBTS) },
                            colors = ButtonDefaults.buttonColors(containerColor = DebtAmber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_debt")
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بدهی/طلب", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 3. Unit Economics & Multi-Crop Analysis (بهای تمام‌شده هر کیلو محصول)
        item {
            UnitEconomicsCard(
                economicsList = unitEconomicsList,
                cropsList = crops,
                onAddCrop = { viewModel.addCustomCrop(it) }
            )
        }

        // 4. Monthly Profitability Bar Chart (Recharts style)
        item {
            val (incomeArr, expenseArr) = monthlyData
            MonthlyProfitabilityChart(
                incomeArr = incomeArr,
                expenseArr = expenseArr,
                currentJalaliYear = currentJy,
                currentMonthIndex = todayJalali.jm - 1
            )
        }

        // 4. Expense Breakdown Card
        item {
            val (topExpenses, _) = categoryBreakdown
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "بیشترین هزینه‌های گلخانه به تفکیک دسته",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topExpenses.isEmpty()) {
                        Text(
                            text = "هنوز هزینه‌ای ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        val totalExp = metrics.totalExpense.coerceAtLeast(1)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topExpenses.forEach { (cat, amt) ->
                                val pct = (amt * 100 / totalExp).toInt()
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "${JalaliCalendar.formatCurrency(amt)} (${JalaliCalendar.toPersianDigits(pct)}٪)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ExpenseRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = ExpenseRed,
                                        trackColor = Color(0xFFFEE2E2),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
