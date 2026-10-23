package com.example.greenhouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel

@Composable
fun LockScreen(
    viewModel: GreenhouseViewModel,
    modifier: Modifier = Modifier
) {
    val securityState by viewModel.securityState.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Branding Header
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Emerald600,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "سامانه حسابداری گلخانه بهارنارنج",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "پورتال اختصاصی مدیریت مالی و دفاتر حسابرسی",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!isRecoveryMode) {
                    // PIN Entry Form
                    Text(
                        text = "رمز عبور یا PIN امنیتی را وارد کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = ""
                        },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lock_pin")
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed
                        )
                    }

                    Button(
                        onClick = {
                            val ok = viewModel.unlockWithPassword(passwordInput)
                            if (!ok) {
                                errorMessage = "رمز عبور نادرست است. لطفاً دوباره تلاش کنید."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_unlock_app")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("باز کردن قفل سامانه")
                    }

                    if (securityState.securityQuestion.isNotBlank()) {
                        TextButton(
                            onClick = {
                                isRecoveryMode = true
                                errorMessage = ""
                            }
                        ) {
                            Text(
                                text = "رمز عبور را فراموش کرده‌اید؟",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald700
                            )
                        }
                    }
                } else {
                    // Recovery via Security Question
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = DebtAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سوال امنیتی:", style = MaterialTheme.typography.labelSmall, color = DebtAmber)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = securityState.securityQuestion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = recoveryAnswerInput,
                        onValueChange = {
                            recoveryAnswerInput = it
                            errorMessage = ""
                        },
                        placeholder = { Text("پاسخ سوال امنیتی") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_recovery_answer")
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val ok = viewModel.unlockWithRecovery(recoveryAnswerInput)
                                if (!ok) {
                                    errorMessage = "پاسخ وارد شده نادرست است."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأیید و بازیابی")
                        }

                        OutlinedButton(
                            onClick = { isRecoveryMode = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("بازگشت")
                        }
                    }
                }
            }
        }
    }
}
