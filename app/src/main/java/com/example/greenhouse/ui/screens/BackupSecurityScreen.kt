package com.example.greenhouse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

@Composable
fun BackupSecurityScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val securityState by viewModel.securityState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val harvestLogs by viewModel.harvestLogs.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("پاکسازی کامل پایگاه داده") },
            text = { Text("آیا مطمئن هستید؟ تمامی تراکنش‌ها، برداشت‌ها و بدهی‌های ثبت شده حذف خواهند شد و قابل بازگشت نخواهند بود.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "تمامی اطلاعات با موفقیت پاکسازی شدند", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_confirm_reset_all")
                ) {
                    Text("بله، پاکسازی کامل", color = ExpenseRed)
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("انصراف") } }
        )
    }

    if (showPasswordDialog) {
        PasswordSetupDialog(
            isCurrentlySet = securityState.hasPassword,
            existingQuestion = securityState.securityQuestion,
            onDismiss = { showPasswordDialog = false },
            onSetPassword = { pass, q, a ->
                val success = viewModel.setPassword(pass, q, a)
                if (success) {
                    Toast.makeText(context, "رمز عبور با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                    showPasswordDialog = false
                }
            },
            onClearPassword = { currPass ->
                val ok = viewModel.clearPassword(currPass)
                if (ok) {
                    Toast.makeText(context, "رمز عبور با موفقیت برداشته شد", Toast.LENGTH_SHORT).show()
                    showPasswordDialog = false
                } else {
                    Toast.makeText(context, "رمز عبور فعلی نادرست است", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showImportDialog) {
        ImportBackupDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonStr ->
                val ok = viewModel.importBackupJson(jsonStr)
                if (ok) {
                    Toast.makeText(context, "فایل پشتیبان با موفقیت بازیابی شد", Toast.LENGTH_SHORT).show()
                    showImportDialog = false
                } else {
                    Toast.makeText(context, "فرمت فایل پشتیبان نامعتبر است", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Security & Lock Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Emerald600)
                        Text("امنیت و رمز عبور سامانه", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    if (securityState.hasPassword) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Emerald100) {
                            Text("رمز فعال است", style = MaterialTheme.typography.labelSmall, color = Emerald800, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF3F4F6)) {
                            Text("بدون رمز عبور", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "جهت محافظت از ارقام مالی گلخانه می‌توانید یک PIN یا رمز عبور و سوال امنیتی جهت بازیابی تنظیم نمایید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showPasswordDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_manage_password")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (securityState.hasPassword) "تغییر یا حذف رمز" else "تنظیم رمز جدید")
                    }

                    if (securityState.hasPassword) {
                        OutlinedButton(
                            onClick = { viewModel.lockApp() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_lock_now")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = DebtAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قفل فوری", color = DebtAmber)
                        }
                    }
                }
            }
        }

        // 2. Backup & Restore Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = BlueAccent)
                    Text("پشتیبان‌گیری و انتقال داده‌ها", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تعداد تراکنش‌ها: ${JalaliCalendar.toPersianDigits(transactions.size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("•", color = TextMuted)
                    Text("بدهی‌ها: ${JalaliCalendar.toPersianDigits(debts.size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("•", color = TextMuted)
                    Text("بارها: ${JalaliCalendar.toPersianDigits(harvestLogs.size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val json = viewModel.exportBackupJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Greenhouse Backup", json))
                            Toast.makeText(context, "فایل پشتیبان در حافظه کلیپ‌بورد کپی شد", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_backup")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروجی کلیپ‌بورد")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_import_backup")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازیابی داده")
                    }
                }
            }
        }

        // 3. Danger Zone Reset
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("منطقه پرخطر: پاکسازی دفاتر", style = MaterialTheme.typography.titleMedium, color = ExpenseRed)
                Spacer(modifier = Modifier.height(6.dp))
                Text("در صورت نیاز به شروع فصل جدید و صفر کردن دفاتر حسابداری، می‌توانید اطلاعات را کاملاً پاکسازی کنید.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_reset_all_data")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("صفر کردن و بازنشانی کلیه دفاتر")
                }
            }
        }
    }
}

@Composable
fun PasswordSetupDialog(
    isCurrentlySet: Boolean,
    existingQuestion: String,
    onDismiss: () -> Unit,
    onSetPassword: (password: String, question: String, answer: String) -> Unit,
    onClearPassword: (currentPassword: String) -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(existingQuestion.ifEmpty { "نام گلخانه شما چیست؟" }) }
    var answer by remember { mutableStateOf("") }
    var currentPassForClear by remember { mutableStateOf("") }
    var isClearingMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isClearingMode) "حذف رمز عبور" else "تنظیم رمز و سوال امنیتی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isClearingMode) {
                    OutlinedTextField(
                        value = currentPassForClear,
                        onValueChange = { currentPassForClear = it },
                        label = { Text("رمز عبور فعلی جهت تایید") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("رمز عبور جدید (حداقل ۴ رقم)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("سوال امنیتی (جهت بازیابی رمز)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("پاسخ سوال امنیتی") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isClearingMode) {
                Button(
                    onClick = { onClearPassword(currentPassForClear) },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("حذف نهایی رمز")
                }
            } else {
                Button(
                    onClick = {
                        if (pass.length >= 4 && answer.isNotBlank()) {
                            onSetPassword(pass, question, answer)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("ذخیره رمز")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCurrentlySet && !isClearingMode) {
                    TextButton(onClick = { isClearingMode = true }) {
                        Text("حذف رمز", color = ExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
fun ImportBackupDialog(
    onDismiss: () -> Unit,
    onImport: (jsonStr: String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بازیابی فایل پشتیبان") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("متن JSON فایل پشتیبان را در کادر زیر جای‌گذاری کنید:", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("{\"transactions\": [...], ...}") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (jsonText.isNotBlank()) onImport(jsonText) },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text("بازیابی")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
