package com.example.proyectofinal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object ReminderScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Intent para disparar el Receiver
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.proyectofinal.SHOW_REMINDER"
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_CATEGORY", reminder.category)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para que el sistema sepa qué pantalla abrir si el usuario toca la alarma en el reloj
        val showIntent = Intent(context, RemindersActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val dateObj = sdf.parse("${reminder.dueDate} ${reminder.dueTime}")
            if (dateObj != null && dateObj.time > System.currentTimeMillis()) {
                val alarmInfo = AlarmManager.AlarmClockInfo(dateObj.time, showPendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                Log.d("Reminders", "Alarma programada con éxito para ID: ${reminder.id}")
            }
        } catch (e: Exception) {
            Log.e("Reminders", "Error en scheduler: ${e.message}")
        }
    }

    fun cancel(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.proyectofinal.SHOW_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
