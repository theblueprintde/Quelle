package com.payas.quelle

import android.app.Application
import com.payas.quelle.data.QuelleStore
import com.payas.quelle.reminders.ReminderScheduler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class QuelleApplication : Application() {
    val store: QuelleStore by lazy { QuelleStore(File(filesDir, "quelle.json")) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        // Program her değiştiğinde bir sonraki hatırlatmayı yeniden kur.
        MainScope().launch {
            store.data.map { it.schedule }.distinctUntilChanged().collect {
                ReminderScheduler.schedule(this@QuelleApplication, it)
            }
        }
    }
}
