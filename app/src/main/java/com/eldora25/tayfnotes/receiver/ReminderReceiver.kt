package com.eldora25.tayfnotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eldora25.tayfnotes.util.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("NOTE_TITLE") ?: "TayfNotes"
        val content = intent.getStringExtra("NOTE_CONTENT") ?: "Bir hatırlatıcınız var."
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showReminderNotification(title, content)
    }
}
