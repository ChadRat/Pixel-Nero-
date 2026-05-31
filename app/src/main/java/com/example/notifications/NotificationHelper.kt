package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.receiver.WaterReminderReceiver
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "water_intake_reminders"
    private const val CHANNEL_NAME = "Water Intake Reminders"
    private const val CHANNEL_DESC = "Notifications to remind you to drink water"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE = 2002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminderNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Random engaging messages
        val titles = listOf(
            "Time to take a sip! 💧",
            "Hydration Check! 🌊",
            "Be kind to your body 🧘‍♂️",
            "Feeling thirsty? 🥤"
        )
        val messages = listOf(
            "It's time for some water. Stay energized and healthy!",
            "Log your latest water intake to stay on track with your goal.",
            "Water supports focus, skin health, and energy. Drink up!",
            "Take a quick water break. Your future self will thank you!"
        )

        val randomIndex = (titles.indices).random()
        val title = titles[randomIndex]
        val message = messages[randomIndex]

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Will map beautifully or use an ic_notification drawable if custom
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun scheduleNextReminder(context: Context) {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminders_enabled", true)
        if (!enabled) {
            cancelAlarms(context)
            return
        }

        val intervalHours = prefs.getInt("reminder_interval", 2)
        val startHour = prefs.getInt("reminder_start_hour", 8)
        val endHour = prefs.getInt("reminder_end_hour", 22)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val triggerTimeMs: Long
        if (currentHour < startHour) {
            // Schedule for start hour today
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            triggerTimeMs = calendar.timeInMillis
        } else if (currentHour >= endHour) {
            // Schedule for start hour tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            triggerTimeMs = calendar.timeInMillis
        } else {
            // Schedule for current_time + interval_hours
            calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
            val scheduledHour = calendar.get(Calendar.HOUR_OF_DAY)
            if (scheduledHour >= endHour) {
                // If interval slips into sleep hours, schedule for tomorrow start hour
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, startHour)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            triggerTimeMs = calendar.timeInMillis
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel previous one first to avoid duplicates
        alarmManager.cancel(pendingIntent)

        // Schedule inexact repeating or just a single alarm that will self-reschedule on receive.
        // Single schedules are much more precise and resilient to various versions of Android background limits.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.d("NotificationHelper", "Reminder scheduled at: ${calendar.time}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to schedule alarm", e)
        }
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("NotificationHelper", "Reminders cancelled")
    }
}
