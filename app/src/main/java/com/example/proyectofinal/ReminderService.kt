package com.example.proyectofinal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    companion object {
        const val CHANNEL_ID = "reminder_service_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Notificación de prioridad mínima para que no se vea en la barra de estado si no se expande
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(null)
            .setContentText(null)
            .setSmallIcon(R.drawable.ic_notifications) // Icono discreto
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Segundo plano",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                description = "Mantiene los recordatorios activos"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
