package com.example.greenhouse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.greenhouse.ui.theme.Emerald50
import com.example.greenhouse.ui.theme.Emerald600
import com.example.greenhouse.ui.theme.TextPrimary
import com.example.greenhouse.util.JalaliCalendar

@Composable
fun PersianDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialJalali = remember(initialDateStr) {
        JalaliCalendar.parseDateToJalali(initialDateStr)
    }

    var selectedYear by remember { mutableStateOf(initialJalali.jy) }
    var selectedMonth by remember { mutableStateOf(initialJalali.jm) }
    var selectedDay by remember { mutableStateOf(initialJalali.jd) }

    val daysInMonth = when {
        selectedMonth in 1..6 -> 31
        selectedMonth in 7..11 -> 30
        else -> 29 // Esfand
    }

    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("persian_date_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "تقویم",
                        tint = Emerald600,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "انتخاب تاریخ شمسی",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedYear > 1400) selectedYear-- },
                        modifier = Modifier.testTag("btn_prev_year")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "سال قبل")
                    }
                    Text(
                        text = "سال ${JalaliCalendar.toPersianDigits(selectedYear)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (selectedYear < 1410) selectedYear++ },
                        modifier = Modifier.testTag("btn_next_year")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "سال بعد")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedMonth > 1) selectedMonth-- },
                        modifier = Modifier.testTag("btn_prev_month")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                    }
                    Text(
                        text = JalaliCalendar.PERSIAN_MONTHS[selectedMonth - 1],
                        style = MaterialTheme.typography.titleMedium,
                        color = Emerald600,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (selectedMonth < 12) selectedMonth++ },
                        modifier = Modifier.testTag("btn_next_month")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days Grid (1..daysInMonth)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val rows = (daysInMonth + 6) / 7
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until 7) {
                                val dayNum = row * 7 + col + 1
                                if (dayNum <= daysInMonth) {
                                    val isSelected = dayNum == selectedDay
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (isSelected) Emerald600 else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedDay = dayNum }
                                    ) {
                                        Text(
                                            text = JalaliCalendar.toPersianDigits(dayNum),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(
                                selectedYear,
                                selectedMonth,
                                selectedDay
                            )
                            val gDateStr = "%04d-%02d-%02d".format(gy, gm, gd)
                            onDateSelected(gDateStr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_date")
                    ) {
                        Text("تأیید تاریخ", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                }
            }
        }
    }
}
