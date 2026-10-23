package com.example.greenhouse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.util.JalaliCalendar

enum class MetricType {
    INCOME,
    EXPENSE,
    NET,
    DEBT
}

@Composable
fun MetricCard(
    title: String,
    amount: Long,
    type: MetricType,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val (primaryColor, bgColor, icon, tag) = when (type) {
        MetricType.INCOME -> Quad(
            IncomeGreen,
            Emerald50,
            Icons.Default.ArrowDownward,
            "metric_income"
        )
        MetricType.EXPENSE -> Quad(
            ExpenseRed,
            Color(0xFFFEF2F2),
            Icons.Default.ArrowUpward,
            "metric_expense"
        )
        MetricType.NET -> Quad(
            if (amount >= 0) Emerald700 else ExpenseRed,
            if (amount >= 0) Emerald100 else Color(0xFFFEE2E2),
            Icons.Default.TrendingUp,
            "metric_net"
        )
        MetricType.DEBT -> Quad(
            DebtAmber,
            Color(0xFFFFFBEB),
            Icons.Default.AccountBalanceWallet,
            "metric_debt"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = JalaliCalendar.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
