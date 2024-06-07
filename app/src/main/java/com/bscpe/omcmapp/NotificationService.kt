package com.bscpe.omcmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class NotificationService : Service() {

    private val CHANNEL_ID = "system_config_channel"
    private val REMINDER_CHANNEL_ID = "reminder_channel"
    private val notificationHandler = Handler(Looper.getMainLooper())
    private val reminderHandler = Handler(Looper.getMainLooper())
    private var notificationRunnable: Runnable? = null
    private var reminderRunnable: Runnable? = null
    private var startTime: Long = 0
    private var notifContent = ""
    private var isAutoMode = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifContent = intent?.getStringExtra("notifContent") ?: "Elapsed Time"
        startTime = intent?.getLongExtra("startTime", System.currentTimeMillis()) ?: System.currentTimeMillis()
        isAutoMode = intent?.getBooleanExtra("isAutoMode", true) ?: true
        val notification = createNotification("00:00:00")

        startForeground(1, notification.build())

        startNotificationTimer()
        startReminderTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationTimer()
        stopReminderTimer()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        sendDelayedReminder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startNotificationTimer() {
        if (notificationRunnable == null) {
            notificationRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val seconds = (elapsedTime / 1000) % 60
                    val minutes = (elapsedTime / (1000 * 60)) % 60
                    val hours = (elapsedTime / (1000 * 60 * 60)) % 24

                    val elapsedTimeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    val notification = createNotification(elapsedTimeFormatted)

                    startForeground(1, notification.build())
                    notificationHandler.postDelayed(this, 1000) // Update every second
                }
            }
            notificationHandler.post(notificationRunnable!!)
        }
    }

    private fun stopNotificationTimer() {
        notificationRunnable?.let {
            notificationHandler.removeCallbacks(it)
        }
        stopForeground(true)
    }

    private fun startReminderTimer() {
        if (reminderRunnable == null) {
            reminderRunnable = object : Runnable {
                override fun run() {
                    sendReminderNotification()
                    notificationHandler.postDelayed(this, 60000 * 30) // Repeat every minute
                }
            }
        }
    }

    private fun stopReminderTimer() {
        reminderRunnable?.let {
            notificationHandler.removeCallbacks(it)
        }
    }

    private fun sendDelayedReminder() {
        reminderHandler.postDelayed({
            sendReminderNotification()
        }, 5000) // Delay of 5 seconds
    }

    private fun createNotification(elapsedTime: String): NotificationCompat.Builder {
        val contentTitle = if (isAutoMode) "Automatic Mode" else "Manual Mode"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contentTitle)
            .setContentText("Elapsed Time: $elapsedTime")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun sendReminderNotification() {
        val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reminder")
            .setContentText("Do not forget to turn off the water!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Elapsed Time Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for showing elapsed time"
            }
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for water turn off reminders"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(channel, reminderChannel))
        }
    }
}
