package com.payas.quelle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.payas.quelle.ui.QuelleApp
import com.payas.quelle.ui.theme.QuelleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuelleTheme {
                QuelleApp()
            }
        }
    }
}
