package com.payas.quelle.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.formatMinutes
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {
    const val CHANNEL_ID = "ders-hatirlatici"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Ders hatırlatıcıları", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Dersler ve etkinlikler başlamadan önce haber verir."
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Bir sonraki hatırlatmayı kurar; önceki kurulu alarmın yerine geçer. */
    fun schedule(context: Context, items: List<ScheduleItem>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val upcoming = nextReminder(items, LocalDateTime.now())
        val intent = Intent(context, ReminderReceiver::class.java)
        if (upcoming == null) {
            alarmManager.cancel(pendingIntent(context, intent))
            return
        }
        val item = upcoming.item
        intent.putExtra(EXTRA_TITLE, "${item.title} $REMINDER_LEAD_MINUTES dakika sonra başlıyor")
        intent.putExtra(EXTRA_TEXT, "${item.kind.label} · ${formatMinutes(item.startMinutes)}–${formatMinutes(item.endMinutes)}")
        val pending = pendingIntent(context, intent)
        val at = upcoming.triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun pendingIntent(context: Context, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
