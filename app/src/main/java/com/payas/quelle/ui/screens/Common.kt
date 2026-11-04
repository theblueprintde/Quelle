package com.payas.quelle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tasarımdaki beyaz, yuvarlak köşeli kartlar. */
@Composable
fun QuelleCard(
    title: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/** Henüz kodlanmamış ekranlar için geçici içerik. */
@Composable
fun ComingSoon(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        QuelleCard("Yakında") {
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Saati her 30 saniyede yeniler; selamlama, tarih ve "şimdi" işareti kendiliğinden değişir. */
@androidx.compose.runtime.Composable
fun rememberNow(): java.time.LocalDateTime {
    var now by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(java.time.LocalDateTime.now()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = java.time.LocalDateTime.now()
        }
    }
    return now
}

@androidx.compose.runtime.Composable
fun rememberStore(): com.payas.quelle.data.QuelleStore {
    val context = androidx.compose.ui.platform.LocalContext.current
    return (context.applicationContext as com.payas.quelle.QuelleApplication).store
}
