package com.payas.quelle.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.payas.quelle.MainActivity
import com.payas.quelle.R
import com.payas.quelle.data.ActiveTimer
import com.payas.quelle.data.TimerMode

/** Odak zamanlayıcısı bitince uygulama kapalı olsa bile bildirim gönderir. */
object FocusAlarm {
    private const val EXTRA_MODE = "mode"

    fun sync(context: Context, timer: ActiveTimer?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, FocusReceiver::class.java)
        if (timer == null || timer.isPaused) {
            alarmManager.cancel(pending(context, intent))
            return
        }
        intent.putExtra(EXTRA_MODE, timer.mode.name)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAt, pending(context, intent))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAt, pending(context, intent))
        }
    }

    private fun pending(context: Context, intent: Intent) = PendingIntent.getBroadcast(
        context,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun modeOf(intent: Intent): TimerMode =
        runCatching { TimerMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: "") }.getOrDefault(TimerMode.Odak)
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val mode = FocusAlarm.modeOf(intent)
        val (title, text) = if (mode == TimerMode.Odak) {
            "Odak süresi bitti" to "Harika iş! Şimdi kısa bir mola ver."
        } else {
            "Mola bitti" to "Hazırsan yeni bir odak oturumuna başla."
        }
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1, notification)
    }
}
