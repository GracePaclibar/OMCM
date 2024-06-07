package com.bscpe.omcmapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class NotificationHelper(private val context: Context) {

    var notifContent = ""
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
    }

    fun startNotificationTimer(isAutoMode: Boolean) {
        val startTime = System.currentTimeMillis()
        sharedPreferences.edit().putLong("start_time", startTime).apply()
        val intent = Intent(context, NotificationService::class.java).apply {
            putExtra("notifContent", notifContent)
            putExtra("startTime", startTime)
            putExtra("isAutoMode", isAutoMode)
        }
        context.startService(intent)
    }

    fun stopNotificationTimer() {
        sharedPreferences.edit().remove("start_time").apply()
        val intent = Intent(context, NotificationService::class.java)
        context.stopService(intent)
    }

    fun isNotificationRunning(): Boolean {
        return sharedPreferences.contains("start_time")
    }

    fun getStartTime(): Long {
        return sharedPreferences.getLong("start_time", 0L)
    }
}
