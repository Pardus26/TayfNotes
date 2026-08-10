package com.eldora25.tayfnotes.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eldora25.tayfnotes.MainActivity
import com.eldora25.tayfnotes.R

class NotificationHelper(private val context: Context) {
    private val channelId = "tayfnotes_reminders"
    private val channelName = "TayfNotes Hatırlatıcılar"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Not hatırlatıcı bildirimleri"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(noteId: String, noteTitle: String, noteContent: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_NOTE_ID", noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TayfNotes: $noteTitle")
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false) // Fix: Stays until action
            .setOngoing(true)    // Optional: for extreme persistence
            .setTicker(noteTitle)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            
        // Add Action Buttons
        val okIntent = Intent(context, MainActivity::class.java) // Simplified
        val okPendingIntent = PendingIntent.getActivity(context, 1, okIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(R.drawable.ic_launcher_foreground, "Tamam", okPendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(noteId.hashCode(), builder.build())
    }
}
