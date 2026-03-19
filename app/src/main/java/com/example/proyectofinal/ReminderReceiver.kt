package com.example.proyectofinal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaskEzz:ReminderWakeLock")
        wakeLock.acquire(5000)

        val pendingResult = goAsync()

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            reprogramAllAlarms(context, pendingResult)
            return
        }

        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "Recordatorio"
        val reminderCategory = intent.getStringExtra("REMINDER_CATEGORY") ?: "General"

        if (reminderId != -1) {
            showNotification(context, reminderId, reminderTitle, reminderCategory)
        }
        
        pendingResult.finish()
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun reprogramAllAlarms(context: Context, pendingResult: PendingResult) {
        val database = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = database.reminderDao().getAllReminders().first()
                reminders.forEach { reminder ->
                    if (!reminder.isCompleted) {
                        ReminderScheduler.schedule(context, reminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, category: String) {
        val channelId = "reminders_discrete_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Cambiamos a IMPORTANCE_DEFAULT para que NO aparezca en pantalla (heads-up)
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones discretas de tus tareas"
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, RemindersActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logoapp)
            .setContentTitle(title)
            .setContentText("Tarea pendiente: $category")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Prioridad normal para evitar banner
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
