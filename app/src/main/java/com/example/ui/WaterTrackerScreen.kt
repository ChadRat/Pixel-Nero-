package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WaterLog
import com.example.data.HealthConnectManager
import androidx.health.connect.client.PermissionController
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTrackerScreen(viewModel: WaterViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val totalIntakeToday by viewModel.totalIntakeToday.collectAsStateWithLifecycle()
    val dailyGoalMl by viewModel.dailyGoalMl.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()

    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderInterval by viewModel.reminderInterval.collectAsStateWithLifecycle()
    val startHour by viewModel.startHour.collectAsStateWithLifecycle()
    val endHour by viewModel.endHour.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val isHcAuthorized by viewModel.isHealthConnectAuthorized.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val oledModeEnabled by viewModel.oledModeEnabled.collectAsStateWithLifecycle()
    val isOledActive = oledModeEnabled && isSystemInDarkTheme()

    // Haptics customization states
    val hapticsSlidersEnabled by viewModel.hapticsSlidersEnabled.collectAsStateWithLifecycle()
    val hapticsButtonsEnabled by viewModel.hapticsButtonsEnabled.collectAsStateWithLifecycle()
    val hapticsGoalEnabled by viewModel.hapticsGoalEnabled.collectAsStateWithLifecycle()
    val vibrationDurationMs by viewModel.vibrationDurationMs.collectAsStateWithLifecycle()
    val vibrationGapMs by viewModel.vibrationGapMs.collectAsStateWithLifecycle()
    val vibrationStrength by viewModel.vibrationStrength.collectAsStateWithLifecycle()
    val isHapticTestActive by viewModel.isHapticTestActive.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopContinuousHapticTest()
        }
    }

    var showCustomLogDialog by remember { mutableStateOf(false) }
    var showGoalSettingsDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // 0: Today, 1: Stats, 2: Reminders, 3: Settings

    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.checkHealthConnectPermissions()
    }

    LaunchedEffect(currentTab) {
        viewModel.checkHealthConnectPermissions()
    }

    // Permissions logic
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission enabled! 💧", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "reminders will only show if notifications are permitted.", Toast.LENGTH_LONG).show()
        }
    }

    // Refresh streak on launch
    LaunchedEffect(Unit) {
        viewModel.calculateStreak()
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(32.dp))
            ) {
                // Frosted glass blurred background layer
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (isOledActive) {
                        Color.Black.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    },
                    tonalElevation = if (isOledActive) 0.dp else 8.dp,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOledActive) {
                            Color.White.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        }
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .blur(16.dp)
                ) {}

                // Frontend content layer (unblurred elements)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(76.dp)
                     ) {
                        // Tab 1: Today
                        NavigationBarItem(
                            selected = (currentTab == 0),
                            onClick = { 
                                currentTab = 0
                                viewModel.setDate(viewModel.getCurrentDateString())
                                viewModel.triggerButtonHaptic()
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                            label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Tab 2: Stats (represented by standard star icon)
                        NavigationBarItem(
                            selected = (currentTab == 1),
                            onClick = { 
                                currentTab = 1 
                                viewModel.triggerButtonHaptic()
                            },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Stats") },
                            label = { Text("Stats", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Tab 3: Reminders
                        NavigationBarItem(
                            selected = (currentTab == 2),
                            onClick = { 
                                currentTab = 2 
                                viewModel.triggerButtonHaptic()
                            },
                            icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                            label = { Text("Reminders", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Tab 4: Settings
                        NavigationBarItem(
                            selected = (currentTab == 3),
                            onClick = { 
                                currentTab = 3 
                                viewModel.triggerButtonHaptic()
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { 
                        showCustomLogDialog = true 
                        viewModel.triggerButtonHaptic()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("fab_custom_log_btn")
                        .padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom volume logging",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Uppercase dynamic date, HydraTrack title, and custom avatar AK
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val headerDateText = remember(currentDate) {
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                                val date = inputFormat.parse(currentDate)
                                date?.let { outputFormat.format(it).uppercase() } ?: "TODAY"
                            } catch (e: Exception) {
                                "TODAY"
                            }
                        }
                        Text(
                            text = headerDateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentTab) {
                                1 -> "Hydration Stats"
                                2 -> "Reminders Setting"
                                3 -> "Personal Settings"
                                else -> "Pixel Nero"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    
                    // Profile/Initials Avatar Circle
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { currentTab = 3 }
                            .testTag("goal_settings_btn"),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AK", // Initial of user Antonis Kouroudis
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            when (currentTab) {
                0 -> {
                    // Navigation with Date Changer
                    item {
                        DateSelectorRow(
                            currentDate = currentDate,
                            onPrevDay = {
                                viewModel.changeDateOffset(-1)
                                viewModel.triggerButtonHaptic()
                            },
                            onNextDay = {
                                viewModel.changeDateOffset(1)
                                viewModel.triggerButtonHaptic()
                            },
                            onResetToToday = {
                                viewModel.setDate(viewModel.getCurrentDateString())
                                viewModel.triggerButtonHaptic()
                            }
                        )
                    }

                    // Central Hydration Progress Ring/Glass layout
                    item {
                        val progressPercent = if (dailyGoalMl > 0) {
                            (totalIntakeToday.toFloat() / dailyGoalMl.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        WaterGlassProgress(
                            percentage = progressPercent,
                            totalIntakeMl = totalIntakeToday,
                            goalMl = dailyGoalMl,
                            streakDays = streak
                        )
                    }

                    // Spectacular Info Grid Row (Next Alert & Last Intake cards)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Next Alert Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { currentTab = 2 },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alert status icon",
                                        tint = if (remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "NEXT ALERT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp
                                        )
                                        val nextAlertText = remember(remindersEnabled, reminderInterval, startHour, endHour) {
                                            if (remindersEnabled) {
                                                val now = Calendar.getInstance()
                                                val currentH = now.get(Calendar.HOUR_OF_DAY)
                                                if (currentH < startHour) {
                                                    val hDisplay = if (startHour > 12) startHour - 12 else if (startHour == 0) 12 else startHour
                                                    val amPm = if (startHour >= 12) "PM" else "AM"
                                                    String.format("%02d:00 %s", hDisplay, amPm)
                                                } else if (currentH >= endHour) {
                                                    val hDisplay = if (startHour > 12) startHour - 12 else if (startHour == 0) 12 else startHour
                                                    val amPm = if (startHour >= 12) "PM" else "AM"
                                                    String.format("Tomorrow %02d:00 %s", hDisplay, amPm)
                                                } else {
                                                    val nextH = currentH + reminderInterval
                                                    if (nextH > endHour) {
                                                        val hDisplay = if (startHour > 12) startHour - 12 else if (startHour == 0) 12 else startHour
                                                        val amPm = if (startHour >= 12) "PM" else "AM"
                                                        String.format("Tomorrow %02d:00 %s", hDisplay, amPm)
                                                    } else {
                                                        val hDisplay = if (nextH > 12) nextH - 12 else if (nextH == 0) 12 else nextH
                                                        val amPm = if (nextH >= 12) "PM" else "AM"
                                                        String.format("%02d:00 %s", hDisplay, amPm)
                                                    }
                                                }
                                            } else {
                                                "Disabled"
                                            }
                                        }
                                        Text(
                                            text = nextAlertText,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            // Last Intake Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Favorite health status icon",
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "LAST INTAKE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp
                                        )
                                        val lastIntakeText = remember(todayLogs) {
                                            val lastLog = todayLogs.maxByOrNull { it.timestamp }
                                            if (lastLog != null) "${lastLog.amountMl} ml" else "None"
                                        }
                                        Text(
                                            text = lastIntakeText,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Add Operations Button Row matching exactly the HTML theme
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "QUICK ADD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    QuickAddOption("150ml", 150, "add_150_btn", "🥤"),
                                    QuickAddOption("250ml", 250, "add_250_btn", "🥛"),
                                    QuickAddOption("500ml", 500, "add_500_btn", "🍼")
                                ).forEach { option ->
                                    Button(
                                        onClick = { 
                                            viewModel.addWaterLog(option.amount)
                                            Toast.makeText(context, "Added ${option.amount}ml!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .testTag(option.testTag),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(option.emoji, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${option.amount}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                // Custom Log Option matching editable HTML style
                                OutlinedButton(
                                    onClick = { 
                                        showCustomLogDialog = true 
                                        viewModel.triggerButtonHaptic()
                                    },
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(56.dp)
                                        .testTag("custom_log_btn"),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit or log custom volume",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Historic records header
                    item {
                        Text(
                            text = "Logs History for Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }

                    // Chronological Logs of Current Day
                    if (todayLogs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Empty Glass Icon",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No water logged for this date.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Tap a button above to start tracking!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        items(todayLogs, key = { it.id }) { log ->
                            WaterLogItem(log = log, onDelete = { 
                                viewModel.deleteWaterLog(log)
                                viewModel.triggerButtonHaptic()
                            })
                        }
                    }
                }
                1 -> {
                    renderStatsAndHistorySection(viewModel, allLogs, dailyGoalMl, context)
                }
                2 -> {
                    renderRemindersSection(
                        viewModel = viewModel,
                        remindersEnabled = remindersEnabled,
                        reminderInterval = reminderInterval,
                        startHour = startHour,
                        endHour = endHour,
                        hasNotificationPermission = hasNotificationPermission,
                        launcher = launcher,
                        context = context
                    )
                }
                3 -> {
                    renderSettingsSection(
                        viewModel = viewModel,
                        dailyGoalMl = dailyGoalMl,
                        onOpenGoalDialog = { showGoalSettingsDialog = true },
                        isHcAvailable = viewModel.isHealthConnectAvailable,
                        isHcAuthorized = isHcAuthorized,
                        onRequestPermissions = {
                            healthConnectLauncher.launch(HealthConnectManager.permissions)
                        },
                        onSyncAllToday = {
                            viewModel.syncAllTodayToHealthConnect()
                            Toast.makeText(context, "Initiated full daily syncing to Google Health Connect!", Toast.LENGTH_SHORT).show()
                        },
                        appTheme = appTheme,
                        oledModeEnabled = oledModeEnabled,
                        hapticsSlidersEnabled = hapticsSlidersEnabled,
                        hapticsButtonsEnabled = hapticsButtonsEnabled,
                        hapticsGoalEnabled = hapticsGoalEnabled,
                        vibrationDurationMs = vibrationDurationMs,
                        vibrationGapMs = vibrationGapMs,
                        vibrationStrength = vibrationStrength,
                        isHapticTestActive = isHapticTestActive
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Modal dialog for Logging Custom Volume
    if (showCustomLogDialog) {
        CustomBeverageDialog(
            onDismiss = { 
                showCustomLogDialog = false 
                viewModel.triggerButtonHaptic()
            },
            onConfirm = { amount, type, factor ->
                viewModel.addWaterLog(amount, type, factor)
                showCustomLogDialog = false
                val equivalentMl = (amount * factor).toInt()
                Toast.makeText(context, "Added $amount ml of $type (equivalent to ${equivalentMl}ml water)!", Toast.LENGTH_SHORT).show()
            },
            triggerButtonHaptic = { viewModel.triggerButtonHaptic() },
            triggerSliderHaptic = { viewModel.triggerSliderHaptic() }
        )
    }

    // Modal dialog for Daily Water Goal update
    if (showGoalSettingsDialog) {
        GoalSettingsDialog(
            currentGoal = dailyGoalMl,
            onDismiss = { 
                showGoalSettingsDialog = false 
                viewModel.triggerButtonHaptic()
            },
            onConfirm = { newGoal ->
                viewModel.updateDailyGoal(newGoal)
                viewModel.triggerButtonHaptic()
                showGoalSettingsDialog = false
                Toast.makeText(context, "Daily target changed to $newGoal ml!", Toast.LENGTH_SHORT).show()
            },
            triggerButtonHaptic = { viewModel.triggerButtonHaptic() }
        )
    }
}

// Data class mapping Quick Logging
private data class QuickAddOption(val label: String, val amount: Int, val testTag: String, val emoji: String)

@Composable
fun DateSelectorRow(
    currentDate: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToToday: () -> Unit
) {
    val displayDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val date = inputFormat.parse(currentDate)
        if (date != null) {
            val calendar = Calendar.getInstance()
            val todayStr = inputFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = inputFormat.format(calendar.time)

            when (currentDate) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> outputFormat.format(date)
            }
        } else {
            currentDate
        }
    } catch (e: Exception) {
        currentDate
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevDay,
                modifier = Modifier.testTag("prev_day_btn")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Load Previous Day")
            }

            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onResetToToday() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onNextDay,
                modifier = Modifier.testTag("next_day_btn")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Load Next Day")
            }
        }
    }
}

@Composable
fun WaterGlassProgress(
    percentage: Float,
    totalIntakeMl: Int,
    goalMl: Int,
    streakDays: Int
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day Streaks Badge
            if (streakDays > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 $streakDays Day Streak",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Circular progress wheel containing volume text exactly as specified by design
            Box(
                modifier = Modifier.size(192.dp),
                contentAlignment = Alignment.Center
            ) {
                val rimColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 12.dp.toPx()
                    val diameter = size.minDimension - strokeWidthPx
                    val radius = diameter / 2
                    val centerOffset = Offset(size.width / 2, size.height / 2)

                    // Draw Background Circle (Rim)
                    drawCircle(
                        color = rimColor,
                        radius = radius,
                        center = centerOffset,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                    )

                    // Draw Progressive Foreground Arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedPercentage * 360f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(diameter, diameter),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidthPx,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Centered stats details with large dynamic volume ml
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%,d", totalIntakeMl),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "ml / $goalMl",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Percentage Badge Pill from design HTML
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite status indicator badge",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}% Daily Goal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (percentage >= 1f) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🎉", fontSize = 28.sp)
                        Column {
                            Text(
                                text = "Daily Goal Achieved!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Awesome job! You reached 100% of your daily water intake goal.",
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterLogItem(log: WaterLog, onDelete: () -> Unit) {
    val formattedTime = try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    } catch (e: Exception) {
        ""
    }

    val iconEmoji = when {
        log.amountMl <= 150 -> "🥤"
        log.amountMl <= 300 -> "🥛"
        log.amountMl <= 550 -> "🍼"
        else -> "💧"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("water_log_item"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(iconEmoji, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${log.amountMl} ml",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_log_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@SuppressLint("UseOfNonDefaultType")
@Composable
fun CustomBeverageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Float) -> Unit,
    triggerButtonHaptic: () -> Unit = {},
    triggerSliderHaptic: () -> Unit = {}
) {
    var amountText by remember { mutableStateOf("") }
    var beverageType by remember { mutableStateOf("Water") }
    var waterEquivalency by remember { mutableStateOf(1.0f) }
    var isError by remember { mutableStateOf(false) }

    val amountInt = amountText.toIntOrNull() ?: 0
    val equivalentMl = (amountInt * waterEquivalency).toInt()

    val presets = listOf(
        Triple("Water", 1.00f, "💦"),
        Triple("Tea", 0.90f, "🍵"),
        Triple("Juice", 0.85f, "🧃"),
        Triple("Milk", 0.88f, "🥛"),
        Triple("Coffee", 0.90f, "☕"),
        Triple("Soda", 0.70f, "🥤")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Log Hydration", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Log customized beverage volume and tracking details.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Amount Text Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("Volume Amount (ml)") },
                    placeholder = { Text("e.g. 250, 400") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_log_input"),
                    isError = isError,
                    singleLine = true
                )

                if (isError) {
                    Text(
                        text = "Please write a valid amount (1 - 5000) ml.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                // Beverage Type Chips
                Text(
                    text = "Select Beverage Type Preset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (type, defFactor, emoji) ->
                        val isSelected = beverageType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                beverageType = type
                                waterEquivalency = defFactor
                                triggerButtonHaptic()
                            },
                            label = { Text("$emoji $type") },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                // Water Equivalency Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Water Equivalency Factor",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(waterEquivalency * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Adjust if this beverage hydrates less than pure water.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Slider(
                        value = waterEquivalency,
                        onValueChange = { newValue ->
                            val roundedValue = String.format("%.2f", newValue).toFloat()
                            if (roundedValue != waterEquivalency) {
                                waterEquivalency = roundedValue
                                triggerSliderHaptic()
                            }
                        },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Live Preview calculation
                if (amountInt > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pure Hydration Equivalence",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "💦 $equivalentMl ml",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountInt in 1..5000) {
                        triggerButtonHaptic()
                        onConfirm(amountInt, beverageType, waterEquivalency)
                    } else {
                        isError = true
                        triggerButtonHaptic()
                    }
                },
                modifier = Modifier.testTag("custom_log_confirm")
            ) {
                Text("Lock in hydration")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    triggerButtonHaptic()
                    onDismiss()
                },
                modifier = Modifier.testTag("custom_log_dismiss")
            ) {
                Text("Discard")
            }
        }
    )
}

@SuppressLint("UseOfNonDefaultType")
@Composable
fun GoalSettingsDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    triggerButtonHaptic: () -> Unit = {}
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Adjust Hydration Target", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Setting a consistent target is ideal for building healthy habits.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = {
                        goalText = it
                        isError = false
                    },
                    label = { Text("Daily Target Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_input"),
                    isError = isError,
                    singleLine = true
                )
                if (isError) {
                    Text(
                        text = "Please enter a valid target (500 - 10000) ml.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetInt = goalText.toIntOrNull()
                    if (targetInt != null && targetInt in 500..10000) {
                        triggerButtonHaptic()
                        onConfirm(targetInt)
                    } else {
                        isError = true
                        triggerButtonHaptic()
                    }
                },
                modifier = Modifier.testTag("goal_confirm_btn")
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    triggerButtonHaptic()
                    onDismiss()
                },
                modifier = Modifier.testTag("goal_dismiss_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

fun androidx.compose.foundation.lazy.LazyListScope.renderStatsAndHistorySection(
    viewModel: WaterViewModel,
    allLogs: List<WaterLog>,
    dailyGoalMl: Int,
    context: Context
) {
    item {
        var daysFilter by remember { mutableStateOf(7) } // 7 or 30 days

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History & Charts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(7 to "7 Days", 30 to "30 Days").forEach { (days, label) ->
                            val isSelected = daysFilter == days
                            Surface(
                                modifier = Modifier
                                    .clickable { daysFilter = days }
                                    .testTag("filter_${days}_days"),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Process logs to daily aggregates
                val dailyTotals = remember(allLogs) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val grouped = allLogs.groupBy { sdf.format(Date(it.timestamp)) }
                    grouped.map { (dateStr, dayLogs) ->
                        val total = dayLogs.sumOf { it.amountMl }
                        dateStr to total
                    }.sortedBy { it.first }
                }

                // Filter to user period
                val filteredTotals = remember(dailyTotals, daysFilter) {
                    if (dailyTotals.size > daysFilter) {
                        dailyTotals.takeLast(daysFilter)
                    } else {
                        dailyTotals
                    }
                }

                val avgIntake = remember(filteredTotals, daysFilter) {
                    if (filteredTotals.isEmpty()) 0 
                    else filteredTotals.sumOf { it.second } / daysFilter
                }

                val completionRate = remember(filteredTotals, dailyGoalMl, daysFilter) {
                    if (filteredTotals.isEmpty()) 0
                    else {
                        val metCount = filteredTotals.count { it.second >= dailyGoalMl }
                        (metCount * 100) / daysFilter
                    }
                }

                // Metric Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Average Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("DAILY AVG", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Text("$avgIntake ml", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    // Achievement Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("GOAL MET", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            Text("$completionRate%", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                if (filteredTotals.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No data icon",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No metrics found for this period.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Log some water or use the Seeding option below!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Custom Bar Chart Render
                    WaterBarChart(filteredTotals = filteredTotals, goalMl = dailyGoalMl)
                }
            }
        }
    }

    // Seeding & Test Data Utilities Section
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SANDBOX UTILITIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Quickly seed historical water metrics to test visualization over past weeks or clear database logs to reset habits.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.seedMockData()
                            Toast.makeText(context, "Seeded 30 days of hydration data successfully! 💦", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seed_mock_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Seed mock statistics data", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seed Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllLogs()
                            Toast.makeText(context, "All logs have been erased! 🧼", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_logs_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all database statistics logs", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WaterBarChart(filteredTotals: List<Pair<String, Int>>, goalMl: Int) {
    val maxIntake = remember(filteredTotals) {
        val maxAmount = filteredTotals.maxOfOrNull { it.second } ?: 1000
        if (maxAmount < goalMl) goalMl else maxAmount
    }

    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Horizontal Scroll Bar Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            filteredTotals.forEach { (dateStr, totalAmount) ->
                // Date formatting for label representation like "Mon 25"
                val label = remember(dateStr) {
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("E d", Locale.getDefault())
                        val date = inputFormat.parse(dateStr)
                        date?.let { outputFormat.format(it) } ?: dateStr
                    } catch (e: Exception) {
                        dateStr
                    }
                }

                val hasMetGoal = totalAmount >= goalMl
                val proportion = totalAmount.toFloat() / maxIntake.toFloat()
                val barHeight = (proportion * 140).coerceIn(12f, 160f).dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(44.dp)
                ) {
                    // Volume Value tooltip above the bar
                    Text(
                        text = "${totalAmount}ml",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasMetGoal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )

                    // Bar box layout representation
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(
                                if (hasMetGoal) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            )
                    )

                    // Vertical divider line
                    Spacer(modifier = Modifier.height(2.dp))

                    // Date label abbreviation
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.renderRemindersSection(
    viewModel: WaterViewModel,
    remindersEnabled: Boolean,
    reminderInterval: Int,
    startHour: Int,
    endHour: Int,
    hasNotificationPermission: Boolean,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context
) {
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission prompt
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permission missing icon",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alert Permission Missing",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Permission is required to receive water reminder push notifications.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Push Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get notified to drink water periodically during awake hours.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { viewModel.updateReminders(it) },
                        modifier = Modifier.testTag("reminder_toggle")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Interval slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Interval Frequency (${reminderInterval} hours)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = reminderInterval.toFloat(),
                        onValueChange = { viewModel.updateReminderInterval(it.toInt()) },
                        valueRange = 1f..6f,
                        steps = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interval_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1h (Aggressive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("3h (Moderate)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("6h (Spaced)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Wake window setting
                Text(
                    text = "Active Awake Window",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "No notification alerts will fire outside this waking range.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Alerts At", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val newStart = if (startHour > 0) startHour - 1 else 0
                                    if (newStart < endHour) viewModel.updateSleepWindow(newStart, endHour)
                                }
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = String.format("%02d:00", startHour),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            IconButton(
                                onClick = {
                                    val newStart = if (startHour < 23) startHour + 1 else 23
                                    if (newStart < endHour) viewModel.updateSleepWindow(newStart, endHour)
                                }
                            ) {
                                Icon(Icons.Default.Add, "Awake Start hour increment")
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stop Alerts At", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val newEnd = if (endHour > 1) endHour - 1 else 1
                                    if (newEnd > startHour) viewModel.updateSleepWindow(startHour, newEnd)
                                }
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = String.format("%02d:00", endHour),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            IconButton(
                                onClick = {
                                    val newEnd = if (endHour < 24) endHour + 1 else 24
                                    if (newEnd > startHour) viewModel.updateSleepWindow(startHour, newEnd)
                                }
                            ) {
                                Icon(Icons.Default.Add, "Awake End hour increment")
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sending test reminder action
                Button(
                    onClick = {
                        viewModel.triggerTestNotification()
                        Toast.makeText(context, "Mock alert scheduled and sent! Check your notification tray.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_push_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Trigger simulated test notification")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Instant Alert Notification", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.renderSettingsSection(
    viewModel: WaterViewModel,
    dailyGoalMl: Int,
    onOpenGoalDialog: () -> Unit,
    isHcAvailable: Boolean,
    isHcAuthorized: Boolean,
    onRequestPermissions: () -> Unit,
    onSyncAllToday: () -> Unit,
    appTheme: String,
    oledModeEnabled: Boolean,
    hapticsSlidersEnabled: Boolean,
    hapticsButtonsEnabled: Boolean,
    hapticsGoalEnabled: Boolean,
    vibrationDurationMs: Int,
    vibrationGapMs: Int,
    vibrationStrength: Int,
    isHapticTestActive: Boolean
) {
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Personalization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Theme Selection Selector UI Card
                Text(
                    text = "Theme Palette Style",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val palettes = listOf(
                        Triple("DYNAMIC", "Material You", "🎨"),
                        Triple("DEFAULT", "Classic Indigo", "💧")
                    )
                    
                    palettes.forEach { (type, name, emoji) ->
                        val isSelected = appTheme == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { 
                                    viewModel.updateAppTheme(type)
                                }
                                .testTag("theme_btn_$type"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                if (appTheme == "DYNAMIC") {
                    Text(
                        text = "Extracted Wallpaper Palettes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wallpaperColors by viewModel.wallpaperThemeColors.collectAsStateWithLifecycle()
                        val selectedPaletteIdx by viewModel.appThemePaletteIndex.collectAsStateWithLifecycle()
                        
                        wallpaperColors.forEachIndexed { idx, colorInt ->
                            val isSelected = selectedPaletteIdx == idx
                            
                            // Extract colors for the swatch preview
                            val (topColor, bottomLeftColor, bottomRightColor) = remember(colorInt) {
                                viewModel.getSwatchColors(colorInt)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(2.dp)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(if (isSelected) 4.dp else 0.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.updateAppThemePaletteIndex(idx)
                                    }
                                    .testTag("wallpaper_palette_btn_$idx")
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Draw top half
                                    drawArc(
                                        color = topColor,
                                        startAngle = 180f,
                                        sweepAngle = 180f,
                                        useCenter = true
                                    )
                                    // Draw bottom-left quadrant
                                    drawArc(
                                        color = bottomLeftColor,
                                        startAngle = 90f,
                                        sweepAngle = 90f,
                                        useCenter = true
                                    )
                                    // Draw bottom-right quadrant
                                    drawArc(
                                        color = bottomRightColor,
                                        startAngle = 0f,
                                        sweepAngle = 90f,
                                        useCenter = true
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OLED Pure Black Background",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Apply pitch-black backgrounds in dark mode to save battery and enhance contrast.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = oledModeEnabled,
                        onCheckedChange = { viewModel.updateOledMode(it) },
                        modifier = Modifier.testTag("oled_mode_toggle")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Editable Daily Target Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGoalDialog() }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Hydration Target", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Configure your absolute target goal limit in ml.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$dailyGoalMl ml",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Edit hydration daily target", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Google Health Connect Sync Card
                Text(
                    text = "Google Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!isHcAvailable) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Unusable Status Alert", tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Health Connect is unavailable on this device. Install Google Health Connect to enable automatic synchronization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Data Syncing", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (isHcAuthorized) "Sync status: Connected 💦" else "Sync status: Authorized permissions needed",
                                fontSize = 12.sp,
                                color = if (isHcAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        if (isHcAuthorized) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Synced Checkmark",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        } else {
                            Button(
                                onClick = onRequestPermissions,
                                modifier = Modifier.testTag("auth_sdk_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isHcAuthorized) {
                        OutlinedButton(
                            onClick = onSyncAllToday,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_day_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync refresh icon", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Force Sync Today's Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Professional weight suggestion guidelines
                Text(
                    text = "Hydration Guidance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "General health research advises drinking approximately 35ml of fresh water per kilogram of biological body weight daily. Ensure you increment intake if exercising or residing in higher warm climates.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // User profile card Antonis Kouroudis
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("AK", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Antonis Kouroudis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Premium Client Sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    // Toggle Preferences Card for Sliders, Buttons and Goal Completion
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📳",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Haptic Feedback Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Customize where and how you feel physical confirmation ticks in the app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Toggle 1: Button haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptics for Buttons",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Provides a crisp pulse when tapping tab icons and interactive buttons.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = hapticsButtonsEnabled,
                        onCheckedChange = { viewModel.updateHapticsButtonsEnabled(it) },
                        modifier = Modifier.testTag("button_haptics_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle 2: Slider haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptics for Sliders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Triggers rapid mechanical ticks while sliding values to fine-tune inputs.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = hapticsSlidersEnabled,
                        onCheckedChange = { viewModel.updateHapticsSlidersEnabled(it) },
                        modifier = Modifier.testTag("slider_haptics_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle 3: Goal Achieved haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptics for Goal Achieved",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Triggers an immersive celebratory double pulse exactly when you reach 100% of your daily intake.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = hapticsGoalEnabled,
                        onCheckedChange = { viewModel.updateHapticsGoalEnabled(it) },
                        modifier = Modifier.testTag("goal_haptics_switch")
                    )
                }
            }
        }
    }

    // Custom Vibration Pattern Card mimicking reference image
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vibration pattern",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Stop toggle button
                        FilledIconButton(
                            onClick = {
                                if (isHapticTestActive) {
                                    viewModel.stopContinuousHapticTest()
                                } else {
                                    viewModel.startContinuousHapticTest()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("haptics_play_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isHapticTestActive) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (isHapticTestActive) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isHapticTestActive) "Stop continuous haptics test" else "Play continuous haptics test",
                                tint = if (isHapticTestActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset button
                        FilledIconButton(
                            onClick = {
                                viewModel.resetHapticsToDefault()
                                if (!isHapticTestActive) {
                                    viewModel.triggerButtonHaptic()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("haptics_reset_btn"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset haptic properties",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Duration Slider row
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${vibrationDurationMs}ms",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = vibrationDurationMs.toFloat(),
                            onValueChange = { newValue ->
                                val intVal = newValue.toInt()
                                if (intVal != vibrationDurationMs) {
                                    viewModel.updateVibrationDurationMs(intVal)
                                    if (!isHapticTestActive) {
                                        viewModel.triggerSliderHaptic()
                                    }
                                }
                            },
                            valueRange = 1f..200f,
                            modifier = Modifier.weight(1f).testTag("duration_slider")
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Vibration pulse duration waves symbol",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Gap Slider row
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gap",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${vibrationGapMs}ms",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = vibrationGapMs.toFloat(),
                            onValueChange = { newValue ->
                                val intVal = newValue.toInt()
                                if (intVal != vibrationGapMs) {
                                    viewModel.updateVibrationGapMs(intVal)
                                    if (!isHapticTestActive) {
                                        viewModel.triggerSliderHaptic()
                                    }
                                }
                            },
                            valueRange = 0f..200f,
                            modifier = Modifier.weight(1f).testTag("gap_slider")
                        )
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Vibration pattern gap horizontal lines symbol",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Strength Slider row
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strength",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${vibrationStrength}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = vibrationStrength.toFloat(),
                            onValueChange = { newValue ->
                                val intVal = newValue.toInt()
                                if (intVal != vibrationStrength) {
                                    viewModel.updateVibrationStrength(intVal)
                                    if (!isHapticTestActive) {
                                        viewModel.triggerSliderHaptic()
                                    }
                                }
                            },
                            valueRange = 1f..100f,
                            modifier = Modifier.weight(1f).testTag("strength_slider")
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Vibration intensity lightning bolt symbol",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
