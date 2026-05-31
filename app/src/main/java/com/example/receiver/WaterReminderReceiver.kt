package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notifications.NotificationHelper

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("WaterReminderReceiver", "Alarm received!")
        
        // Show notification to the user
        NotificationHelper.showReminderNotification(context)
        
        // Self-perpetuate by scheduling the next alarm
        NotificationHelper.scheduleNextReminder(context)
    }
}
