package com.payas.quelle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.greetingFor
import com.payas.quelle.ui.theme.BabyBlue
import com.payas.quelle.ui.theme.GreenLight
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("tr"))

@Composable
fun TodayScreen() {
    // Her dakika saati yenile ki selamlama ve tarih kendiliğinden değişsin.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text(
                now.format(dateFormat).replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr")) },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${greetingFor(now.hour)}, Payas",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            QuelleCard("Günün sözü", Modifier.weight(1f), container = BabyBlue) {
                Text(
                    "“Bin millik yolculuk tek bir adımla başlar.”",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text("Lao Tzu", fontWeight = FontWeight.Bold)
            }
            QuelleCard("Saat saat program", Modifier.weight(1f)) {
                Text("Ders programın burada görünecek.")
            }
            QuelleCard("Görevler", Modifier.weight(1f), container = GreenLight) {
                Text("Günlük görev listen burada olacak.")
            }
        }
    }
}
