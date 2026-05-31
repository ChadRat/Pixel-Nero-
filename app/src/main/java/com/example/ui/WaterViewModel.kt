package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WaterDatabase
import com.example.data.WaterLog
import com.example.data.WaterRepository
import com.example.data.HealthConnectManager
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.Build
import android.app.WallpaperManager
import androidx.compose.ui.graphics.Color
import java.util.*

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
    private val database = WaterDatabase.getDatabase(application)
    private val repository = WaterRepository(database.waterLogDao())

    private val _currentDate = MutableStateFlow(getCurrentDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // Health Connect Authorization Status State
    val isHealthConnectAvailable = HealthConnectManager.isSdkAvailable(application)
    private val _isHealthConnectAuthorized = MutableStateFlow(false)
    val isHealthConnectAuthorized: StateFlow<Boolean> = _isHealthConnectAuthorized.asStateFlow()

    // Preferences exposed as Flow state
    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "DEFAULT") ?: "DEFAULT")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _appThemePaletteIndex = MutableStateFlow(prefs.getInt("app_theme_palette_index", 0))
    val appThemePaletteIndex: StateFlow<Int> = _appThemePaletteIndex.asStateFlow()

    val wallpaperThemeColors: StateFlow<List<Int>> = flow {
        emit(getWallpaperSeedColors(getApplication()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(
        0xFF1D5AAB.toInt(), // Sky Blue
        0xFF4B5563.toInt(), // Slate Gray
        0xFFD97706.toInt(), // Warm Amber / Orange
        0xFF3B82F6.toInt(), // Deep Royal Blue
        0xFFEC4899.toInt()  // Hot Pink
    ))

    private val _oledModeEnabled = MutableStateFlow(prefs.getBoolean("oled_mode_enabled", false))
    val oledModeEnabled: StateFlow<Boolean> = _oledModeEnabled.asStateFlow()

    private val _dailyGoalMl = MutableStateFlow(prefs.getInt("daily_goal", 2000))
    val dailyGoalMl: StateFlow<Int> = _dailyGoalMl.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean("reminders_enabled", true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _reminderInterval = MutableStateFlow(prefs.getInt("reminder_interval", 2))
    val reminderInterval: StateFlow<Int> = _reminderInterval.asStateFlow()

    private val _startHour = MutableStateFlow(prefs.getInt("reminder_start_hour", 8))
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _endHour = MutableStateFlow(prefs.getInt("reminder_end_hour", 22))
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    // Haptics customization preferences
    private val _hapticsSlidersEnabled = MutableStateFlow(prefs.getBoolean("haptics_sliders_enabled", true))
    val hapticsSlidersEnabled: StateFlow<Boolean> = _hapticsSlidersEnabled.asStateFlow()

    private val _hapticsButtonsEnabled = MutableStateFlow(prefs.getBoolean("haptics_buttons_enabled", true))
    val hapticsButtonsEnabled: StateFlow<Boolean> = _hapticsButtonsEnabled.asStateFlow()

    private val _hapticsGoalEnabled = MutableStateFlow(prefs.getBoolean("haptics_goal_enabled", true))
    val hapticsGoalEnabled: StateFlow<Boolean> = _hapticsGoalEnabled.asStateFlow()

    private val _vibrationDurationMs = MutableStateFlow(prefs.getInt("vibration_duration_ms", 27))
    val vibrationDurationMs: StateFlow<Int> = _vibrationDurationMs.asStateFlow()

    private val _vibrationGapMs = MutableStateFlow(prefs.getInt("vibration_gap_ms", 10))
    val vibrationGapMs: StateFlow<Int> = _vibrationGapMs.asStateFlow()

    private val _vibrationStrength = MutableStateFlow(prefs.getInt("vibration_strength", 80))
    val vibrationStrength: StateFlow<Int> = _vibrationStrength.asStateFlow()

    // Logs for selected/current date
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<WaterLog>> = _currentDate
        .flatMapLatest { date -> repository.getLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated total intake for the day
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalIntakeToday: StateFlow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(date) }
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // All logs for historical visualization
    val allLogs: StateFlow<List<WaterLog>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Streak count
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    init {
        // Migrate legacy "OLED" theme selection to DEFAULT with OLED mode enabled
        if (_appTheme.value == "OLED") {
            _appTheme.value = "DEFAULT"
            prefs.edit().putString("app_theme", "DEFAULT").apply()
            _oledModeEnabled.value = true
            prefs.edit().putBoolean("oled_mode_enabled", true).apply()
        }
        // Initial setup for alarms if enabled
        syncReminders()
        calculateStreak()
        checkHealthConnectPermissions()
    }

    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            _isHealthConnectAuthorized.value = HealthConnectManager.hasAllPermissions(getApplication())
        }
    }

    fun syncToHealthConnect(amountMl: Double, timestamp: Long) {
        viewModelScope.launch {
            if (HealthConnectManager.hasAllPermissions(getApplication())) {
                HealthConnectManager.writeHydration(getApplication(), amountMl, timestamp)
            }
        }
    }

    fun syncAllTodayToHealthConnect() {
        viewModelScope.launch {
            if (HealthConnectManager.hasAllPermissions(getApplication())) {
                todayLogs.value.forEach { log ->
                    HealthConnectManager.writeHydration(
                        getApplication(),
                        log.waterEquivalentMl.toDouble(),
                        log.timestamp
                    )
                }
            }
        }
    }

    fun setDate(dateStr: String) {
        _currentDate.value = dateStr
    }

    fun addWaterLog(amountMl: Int, beverageType: String = "Water", waterEquivalency: Float = 1.0f) {
        viewModelScope.launch {
            val beforeIntake = totalIntakeToday.value
            val equivalentMl = (amountMl * waterEquivalency).toInt()
            val nextTotal = beforeIntake + equivalentMl
            val goal = dailyGoalMl.value

            val newLog = WaterLog(
                amountMl = amountMl,
                beverageType = beverageType,
                waterEquivalency = waterEquivalency,
                waterEquivalentMl = equivalentMl,
                dateString = getCurrentDateString(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertLog(newLog)
            // Recalculate streak in case a day's goal has been reached/restored
            calculateStreak()
            // Auto-sync to Health Connect
            syncToHealthConnect(equivalentMl.toDouble(), newLog.timestamp)

            // Trigger corresponding haptics
            if (beforeIntake < goal && nextTotal >= goal) {
                triggerGoalHaptic()
            } else {
                triggerButtonHaptic()
            }
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
            calculateStreak()
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        _dailyGoalMl.value = goalMl
        prefs.edit().putInt("daily_goal", goalMl).apply()
        calculateStreak() // Goal change can affect streak
    }

    fun updateReminders(enabled: Boolean) {
        _remindersEnabled.value = enabled
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        syncReminders()
    }

    fun updateReminderInterval(intervalHours: Int) {
        _reminderInterval.value = intervalHours
        prefs.edit().putInt("reminder_interval", intervalHours).apply()
        syncReminders()
    }

    fun updateSleepWindow(start: Int, end: Int) {
        _startHour.value = start
        _endHour.value = end
        prefs.edit()
            .putInt("reminder_start_hour", start)
            .putInt("reminder_end_hour", end)
            .apply()
        syncReminders()
    }

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme).apply()
    }

    fun updateAppThemePaletteIndex(index: Int) {
        _appThemePaletteIndex.value = index
        prefs.edit().putInt("app_theme_palette_index", index).apply()
        triggerButtonHaptic()
    }

    fun getSwatchColors(seedColorInt: Int): Triple<Color, Color, Color> {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(seedColorInt, hsl)
        val hue = hsl[0]
        val sat = hsl[1]
        
        // Top color: Light container/surface style (surface variant look)
        val topColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.1f, 0.4f), 0.78f)))
        
        // Bottom-left color: Strong primary accent
        val botLeftColor = Color(seedColorInt or (0xFF.toInt() shl 24)) // Ensure opaque
        
        // Bottom-right color: Shifted tertiary hue color
        val botRightColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 120f) % 360f, sat.coerceIn(0.3f, 0.6f), 0.65f)))
        
        return Triple(topColor, botLeftColor, botRightColor)
    }

    @android.annotation.SuppressLint("NewApi")
    private fun getWallpaperSeedColors(context: Context): List<Int> {
        val defaultSeeds = listOf(
            0xFF1D5AAB.toInt(), // Ice Blue / Sky Blue
            0xFF4B5563.toInt(), // Slate Gray
            0xFFD97706.toInt(), // Warm Amber / Orange
            0xFF3B82F6.toInt(), // Deep Royal Blue
            0xFFEC4899.toInt()  // Hot Pink
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val list = mutableListOf<Int>()
                    list.add(colors.primaryColor.toArgb())
                    colors.secondaryColor?.let { list.add(it.toArgb()) }
                    colors.tertiaryColor?.let { list.add(it.toArgb()) }
                    
                    val primaryArgb = colors.primaryColor.toArgb()
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(primaryArgb, hsl)
                    
                    while (list.size < 5) {
                        val shiftRatio = list.size * 72f
                        val newHsl = floatArrayOf((hsl[0] + shiftRatio) % 360f, hsl[1], hsl[2])
                        val generatedColor = androidx.core.graphics.ColorUtils.HSLToColor(newHsl)
                        list.add(generatedColor)
                    }
                    return list.take(5)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error extracting wallpaper colors", e)
            }
        }
        return defaultSeeds
    }

    fun updateOledMode(enabled: Boolean) {
        _oledModeEnabled.value = enabled
        prefs.edit().putBoolean("oled_mode_enabled", enabled).apply()
    }

    private val _isHapticTestActive = MutableStateFlow(false)
    val isHapticTestActive: StateFlow<Boolean> = _isHapticTestActive.asStateFlow()

    fun updateHapticsSlidersEnabled(enabled: Boolean) {
        _hapticsSlidersEnabled.value = enabled
        prefs.edit().putBoolean("haptics_sliders_enabled", enabled).apply()
    }

    fun updateHapticsButtonsEnabled(enabled: Boolean) {
        _hapticsButtonsEnabled.value = enabled
        prefs.edit().putBoolean("haptics_buttons_enabled", enabled).apply()
    }

    fun updateHapticsGoalEnabled(enabled: Boolean) {
        _hapticsGoalEnabled.value = enabled
        prefs.edit().putBoolean("haptics_goal_enabled", enabled).apply()
    }

    fun updateVibrationDurationMs(ms: Int) {
        _vibrationDurationMs.value = ms
        prefs.edit().putInt("vibration_duration_ms", ms).apply()
        if (_isHapticTestActive.value) {
            startContinuousHapticTest()
        }
    }

    fun updateVibrationGapMs(ms: Int) {
        _vibrationGapMs.value = ms
        prefs.edit().putInt("vibration_gap_ms", ms).apply()
        if (_isHapticTestActive.value) {
            startContinuousHapticTest()
        }
    }

    fun updateVibrationStrength(strength: Int) {
        _vibrationStrength.value = strength
        prefs.edit().putInt("vibration_strength", strength).apply()
        if (_isHapticTestActive.value) {
            startContinuousHapticTest()
        }
    }

    fun resetHapticsToDefault() {
        updateVibrationDurationMs(27)
        updateVibrationGapMs(10)
        updateVibrationStrength(80)
    }

    fun triggerButtonHaptic() {
        if (_hapticsButtonsEnabled.value) {
            vibrateSinglePulse(_vibrationDurationMs.value, _vibrationStrength.value)
        }
    }

    fun triggerSliderHaptic() {
        if (_hapticsSlidersEnabled.value) {
            vibrateSinglePulse(12, (_vibrationStrength.value * 0.7f).toInt().coerceIn(10, 100))
        }
    }

    fun triggerGoalHaptic() {
        if (_hapticsGoalEnabled.value) {
            val duration = _vibrationDurationMs.value
            val gap = _vibrationGapMs.value
            val strength = _vibrationStrength.value
            vibratePattern(
                longArrayOf(0, duration.toLong(), gap.toLong(), duration.toLong()),
                intArrayOf(0, strength, 0, strength)
            )
        }
    }

    fun startContinuousHapticTest() {
        _isHapticTestActive.value = true
        val vibrator = getVibrator() ?: return
        val duration = _vibrationDurationMs.value.toLong()
        val gap = _vibrationGapMs.value.toLong()
        val strength = _vibrationStrength.value
        val amplitude = (strength / 100f * 255).toInt().coerceIn(1, 255)

        try {
            vibrator.cancel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, duration, gap)
                val amplitudes = intArrayOf(0, amplitude, 0)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                val timings = longArrayOf(0, duration, gap)
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, 0)
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error in startContinuousHapticTest: ${e.message}", e)
        }
    }

    fun stopContinuousHapticTest() {
        _isHapticTestActive.value = false
        try {
            getVibrator()?.cancel()
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error in stopContinuousHapticTest: ${e.message}", e)
        }
    }

    private fun getVibrator(): Vibrator? {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrateSinglePulse(durationMs: Int, strengthPercent: Int) {
        val vibrator = getVibrator() ?: return
        val amplitude = (strengthPercent / 100f * 255).toInt().coerceIn(1, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error in vibrateSinglePulse: ${e.message}", e)
        }
    }

    private fun vibratePattern(timings: LongArray, strengths: IntArray) {
        val vibrator = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = strengths.map { (it / 100f * 255).toInt().coerceIn(0, 255) }.toIntArray()
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error in vibratePattern: ${e.message}", e)
        }
    }

    private fun syncReminders() {
        if (_remindersEnabled.value) {
            NotificationHelper.createNotificationChannel(getApplication())
            NotificationHelper.scheduleNextReminder(getApplication())
        } else {
            NotificationHelper.cancelAlarms(getApplication())
        }
    }

    fun triggerTestNotification() {
        NotificationHelper.showReminderNotification(getApplication())
    }

    // Smart logic to calculate hydration streaks
    fun calculateStreak() {
        viewModelScope.launch {
            repository.getAllLogs().take(1).collect { allLogs ->
                if (allLogs.isEmpty()) {
                    _streak.value = 0
                    return@collect
                }

                // Group history by date
                val dailyTotals = allLogs.groupBy { it.dateString }
                    .mapValues { entry -> entry.value.sumOf { it.waterEquivalentMl } }

                val goal = _dailyGoalMl.value
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = getCurrentDateString()
                
                var currentStreak = 0
                val calendar = Calendar.getInstance()

                // If today's objective is completed, start streak check from today
                // Otherwise start check from yesterday
                val todayTotal = dailyTotals[todayStr] ?: 0
                if (todayTotal >= goal) {
                    currentStreak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    // Check if they had logs today. If today is still partially logged, 
                    // check yesterday for a streak continuation
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Keep counting backward as long as days met the goal
                while (true) {
                    val dateKey = sdf.format(calendar.time)
                    val daysIntake = dailyTotals[dateKey] ?: 0
                    if (daysIntake >= goal) {
                        currentStreak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }

                _streak.value = currentStreak
            }
        }
    }

    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun changeDateOffset(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val parsedDate = sdf.parse(_currentDate.value) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = parsedDate
            calendar.add(Calendar.DAY_OF_YEAR, days)
            _currentDate.value = sdf.format(calendar.time)
        } catch (e: Exception) {
            _currentDate.value = getCurrentDateString()
        }
    }

    fun seedMockData() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            // Generate mock records for the past 30 days
            for (i in 0 until 30) {
                val dateStr = sdf.format(calendar.time)
                // Delete existing logs for this mock day first to prevent duplication / pile-up
                repository.getLogsForDate(dateStr).take(1).collect { existing ->
                    existing.forEach { repository.deleteLog(it) }
                }

                // Generates randomized target completion
                val shouldMeetGoal = (0..5).random() > 1 // 66% chance to exceed or meet goal
                val goal = _dailyGoalMl.value
                val dayTarget = if (shouldMeetGoal) {
                    goal + (100..600).random()
                } else {
                    goal - (200..800).random()
                }

                var currentTotal = 0
                val logCount = (2..5).random()
                for (j in 0 until logCount) {
                    val remaining = dayTarget - currentTotal
                    if (remaining <= 0) break
                    
                    val segmentAmount = if (j == logCount - 1) {
                        remaining
                    } else {
                        val base = (remaining / (logCount - j)).coerceAtLeast(100)
                        ((base + (-50..50).random()) / 50 * 50).coerceIn(100, 1000)
                    }
                    
                    currentTotal += segmentAmount
                    // Distribute throughout the awake hours
                    val logTime = calendar.timeInMillis + (3600 * 1000 * 9) + (j * 3 * 3600 * 1000)
                    val beverages = listOf(
                        Triple("Water", 1.00f, "Water"),
                        Triple("Tea", 0.90f, "Tea"),
                        Triple("Juice", 0.85f, "Juice"),
                        Triple("Milk", 0.88f, "Milk")
                    )
                    val bev = beverages.random()
                    val log = WaterLog(
                        amountMl = segmentAmount,
                        dateString = dateStr,
                        timestamp = logTime,
                        beverageType = bev.first,
                        waterEquivalency = bev.second,
                        waterEquivalentMl = (segmentAmount * bev.second).toInt()
                    )
                    repository.insertLog(log)
                }
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            calculateStreak()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.deleteAllLogs()
            calculateStreak()
        }
    }
}
