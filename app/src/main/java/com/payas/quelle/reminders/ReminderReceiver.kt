package com.payas.quelle.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.payas.quelle.MainActivity
import com.payas.quelle.QuelleApplication
import com.payas.quelle.R

/** Hatırlatma zamanı gelince bildirimi gösterir ve sıradakini kurar. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Quelle")
                .setContentText(intent.getStringExtra(ReminderScheduler.EXTRA_TEXT))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(System.currentTimeMillis().toInt(), notification)
        }

        val store = (context.applicationContext as QuelleApplication).store
        ReminderScheduler.schedule(context, store.data.value.schedule)
    }
}

/** Tablet yeniden başlayınca hatırlatıcıyı tekrar kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = (context.applicationContext as QuelleApplication).store
        ReminderScheduler.schedule(context, store.data.value.schedule)
    }
}
