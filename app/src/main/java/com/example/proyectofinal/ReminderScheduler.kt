package com.example.proyectofinal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object ReminderScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.proyectofinal.SHOW_REMINDER"
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_DESCRIPTION", reminder.description)
            putExtra("REMINDER_CATEGORY", reminder.category)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val dateObj = sdf.parse("${reminder.dueDate} ${reminder.dueTime}")
            if (dateObj != null) {
                var triggerTime = dateObj.time
                
                // Si la hora ya pasó y es repetitiva, buscar la siguiente ocurrencia
                if (triggerTime <= System.currentTimeMillis() && reminder.repeatType != "None") {
                    triggerTime = getNextOccurrence(triggerTime, reminder.repeatType)
                }

                if (triggerTime > System.currentTimeMillis()) {
                    when (reminder.repeatType) {
                        "Daily" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, pendingIntent)
                        "Weekly" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY * 7, pendingIntent)
                        "Monthly" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY * 30, pendingIntent)
                        else -> {
                            val showIntent = Intent(context, RemindersActivity::class.java)
                            val showPendingIntent = PendingIntent.getActivity(
                                context, reminder.id, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                            alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                        }
                    }
                    Log.d("Reminders", "Alarma programada (${reminder.repeatType}) para ID: ${reminder.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("Reminders", "Error en scheduler: ${e.message}")
        }
    }

    private fun getNextOccurrence(startTime: Long, repeatType: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        val now = Calendar.getInstance()
        
        while (calendar.before(now)) {
            when (repeatType) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                else -> return startTime // No debería llegar aquí si repeatType != None
            }
        }
        return calendar.timeInMillis
    }

    fun cancel(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
