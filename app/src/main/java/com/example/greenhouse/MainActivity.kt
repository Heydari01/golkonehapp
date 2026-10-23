package com.example.greenhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenhouse.ui.screens.*
import com.example.greenhouse.ui.theme.*
import com.example.greenhouse.ui.viewmodel.AppTab
import com.example.greenhouse.ui.viewmodel.GreenhouseViewModel
import com.example.greenhouse.util.JalaliCalendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GreenhouseApp
        val repo = app.repository
        val networkMonitor = app.networkMonitor

        setContent {
            GreenhouseTheme {
                val vm = viewModel { GreenhouseViewModel(repo) }
                val isUnlocked by vm.isUnlocked.collectAsState()
                val securityState by vm.securityState.collectAsState()
                val activeTab by vm.activeTab.collectAsState()
                val isOnline by networkMonitor.isOnline.collectAsState()

                if (!isUnlocked) {
                    LockScreen(viewModel = vm)
                } else {
                    BackHandler(enabled = activeTab != AppTab.DASHBOARD) {
                        vm.selectTab(AppTab.DASHBOARD)
                    }

                    Scaffold(
                        topBar = {
                            TopGreenhouseAppBar(
                                hasPassword = securityState.hasPassword,
                                isOnline = isOnline,
                                onLockClick = { vm.lockApp() },
                                onSettingsClick = { vm.selectTab(AppTab.BACKUP) }
                            )
                        },
                        bottomBar = {
                            GreenhouseBottomBar(
                                activeTab = activeTab,
                                onSelectTab = { vm.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (!isOnline) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = "آفلاین",
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "حالت آفلاین فعال است: تمامی اطلاعات در حافظه داخلی دستگاه شما ثبت و نگهداری می‌شوند.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (activeTab) {
                                    AppTab.DASHBOARD -> DashboardScreen(viewModel = vm)
                                    AppTab.TRANSACTIONS -> TransactionsScreen(viewModel = vm)
                                    AppTab.ADD -> TransactionFormScreen(viewModel = vm)
                                    AppTab.HARVEST -> HarvestManagerScreen(viewModel = vm)
                                    AppTab.DEBTS -> DebtsManagerScreen(viewModel = vm)
                                    AppTab.ASSISTANT -> AIAssistantScreen(viewModel = vm)
                                    AppTab.BACKUP -> BackupSecurityScreen(viewModel = vm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGreenhouseAppBar(
    hasPassword: Boolean,
    isOnline: Boolean,
    onLockClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val todayJalali = remember { JalaliCalendar.getTodayJalali() }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceCard
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald600,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "گلخانه بهارنارنج",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${todayJalali.jd} ${todayJalali.monthName} ${todayJalali.jy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald700
                    )
                }
            }
        },
        actions = {
            // Offline/Online status badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isOnline) Emerald50 else Color(0xFFFEF3C7),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = if (isOnline) "آنلاین" else "آفلاین",
                        tint = if (isOnline) IncomeGreen else Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isOnline) "حافظه ابری/محلی" else "آفلاین",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOnline) Emerald900 else Color(0xFF92400E)
                    )
                }
            }

            if (hasPassword) {
                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier.testTag("btn_top_lock")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل کردن فوری",
                        tint = DebtAmber
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("btn_top_backup")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "تنظیمات و پشتیبان",
                    tint = TextSecondary
                )
            }
        }
    )
}

@Composable
fun GreenhouseBottomBar(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceCard,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = activeTab == AppTab.DASHBOARD,
            onClick = { onSelectTab(AppTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "داشبورد") },
            label = { Text("داشبورد", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )

        NavigationBarItem(
            selected = activeTab == AppTab.TRANSACTIONS,
            onClick = { onSelectTab(AppTab.TRANSACTIONS) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "تراکنش‌ها") },
            label = { Text("تراکنش‌ها", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )

        NavigationBarItem(
            selected = activeTab == AppTab.ADD,
            onClick = { onSelectTab(AppTab.ADD) },
            icon = { Icon(Icons.Default.AddCircle, contentDescription = "ثبت") },
            label = { Text("ثبت", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )

        NavigationBarItem(
            selected = activeTab == AppTab.HARVEST,
            onClick = { onSelectTab(AppTab.HARVEST) },
            icon = { Icon(Icons.Default.Scale, contentDescription = "برداشت") },
            label = { Text("برداشت", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )

        NavigationBarItem(
            selected = activeTab == AppTab.DEBTS,
            onClick = { onSelectTab(AppTab.DEBTS) },
            icon = { Icon(Icons.Default.People, contentDescription = "بدهی/طلب") },
            label = { Text("بدهی‌ها", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )

        NavigationBarItem(
            selected = activeTab == AppTab.ASSISTANT,
            onClick = { onSelectTab(AppTab.ASSISTANT) },
            icon = { Icon(Icons.Default.SmartToy, contentDescription = "دستیار") },
            label = { Text("دستیار", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald600,
                selectedTextColor = Emerald800,
                indicatorColor = Emerald100
            )
        )
    }
}
