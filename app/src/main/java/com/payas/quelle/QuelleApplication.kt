package com.payas.quelle

import android.app.Application
import com.payas.quelle.data.QuelleStore
import java.io.File

class QuelleApplication : Application() {
    val store: QuelleStore by lazy { QuelleStore(File(filesDir, "quelle.json")) }
}
