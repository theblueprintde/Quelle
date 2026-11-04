package com.payas.quelle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.AppData
import com.payas.quelle.data.habitCount
import com.payas.quelle.data.mondayOf
import com.payas.quelle.data.moodFor
import com.payas.quelle.data.reviewFor
import com.payas.quelle.data.weekStats
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.Water
import com.payas.quelle.ui.theme.WaterDeep
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekLocale = Locale.forLanguageTag("tr")
private val DAY_LETTERS = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

@Composable
fun WeekScreen() {
    val store = rememberStore()
    val data by store.data.collectAsState()
    val today = rememberNow().toLocalDate()
    var weekStart by remember { mutableStateOf(mondayOf(today)) }
    val stats = data.weekStats(weekStart, today)
    val format = DateTimeFormatter.ofPattern("d MMMM", weekLocale)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${weekStart.format(format)} – ${weekStart.plusDays(6).format(format)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                    )
                    IconButton(onClick = { weekStart = weekStart.minusWeeks(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki hafta")
                    }
                    IconButton(onClick = { weekStart = weekStart.plusWeeks(1) }, enabled = weekStart < mondayOf(today)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki hafta")
                    }
                }
                Text("Haftan nasıl geçti?", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
            StatBox("${stats.tasksDone}/${stats.tasksTotal}", "GÖREV", WaterDeep)
            StatBox("${stats.focusSessions}", "ODAK OTURUMU", GreenDeep)
            StatBox("${stats.bestStreak} gün", "EN UZUN SERİ", WaterDeep)
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            QuelleCard("Tamamlanan görevler", Modifier.weight(1f).fillMaxHeight()) {
                val max = (stats.doneByDay.maxOrNull() ?: 0).coerceAtLeast(1)
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    stats.doneByDay.forEachIndexed { i, count ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = InkMuted)
                            Box(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.75f * count / max + 0.02f)
                                    .background(if (weekStart.plusDays(i.toLong()) == today) Water else Color(0xFF7FC3DE), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            )
                            Text(DAY_LETTERS[i], fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = InkMuted)
                        }
                    }
                }
                Text("RUH HALİ", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = InkMuted)
                Row {
                    for (i in 0..6) {
                        val mood = data.moodFor(weekStart.plusDays(i.toLong()).toString())
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .background(mood?.let { Color(it.color) } ?: Color(0xFFEEF2F3), CircleShape)
                            )
                        }
                    }
                }
            }

            HabitGrid(data, weekStart, Modifier.weight(1f).fillMaxHeight())

            ReflectionCard(
                data = data,
                weekStart = weekStart,
                onSave = store::saveReview,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, color: Color) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = InkMuted)
    }
}

@Composable
private fun HabitGrid(data: AppData, weekStart: LocalDate, modifier: Modifier) {
    QuelleCard("Alışkanlıklar", modifier) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (data.habits.isEmpty()) {
                Text("Bugün ekranından alışkanlık ekleyince burada haftalık tablosu görünür.", color = InkMuted)
            } else {
                Row {
                    DAY_LETTERS.forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                    }
                }
            }
            data.habits.forEach { habit ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(habit.name, fontWeight = FontWeight.ExtraBold)
                    Row {
                        for (i in 0..6) {
                            val day = weekStart.plusDays(i.toLong()).toString()
                            val done = data.habitCount(habit.id, day) >= habit.target
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(if (done) Color(0xFF3E7D52) else Color(0xFFE4EEF0), RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionCard(
    data: AppData,
    weekStart: LocalDate,
    onSave: (com.payas.quelle.data.WeeklyReview) -> Unit,
    modifier: Modifier,
) {
    val saved = data.reviewFor(weekStart.toString())
    var achieved by remember(saved) { mutableStateOf(saved.achieved) }
    var missed by remember(saved) { mutableStateOf(saved.missed) }
    var nextGoal by remember(saved) { mutableStateOf(saved.nextGoal) }
    val changed = achieved != saved.achieved || missed != saved.missed || nextGoal != saved.nextGoal

    QuelleCard("Kendine sor", modifier, container = BabyBlueLight) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(achieved, { achieved = it }, label = { Text("Bu hafta neyi başardım?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(missed, { missed = it }, label = { Text("Neyi aksattım?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nextGoal, { nextGoal = it }, label = { Text("Gelecek haftanın ana hedefi") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
        Button(
            onClick = { onSave(saved.copy(achieved = achieved.trim(), missed = missed.trim(), nextGoal = nextGoal.trim())) },
            enabled = changed,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(if (changed) "Haftayı kaydet" else "Kaydedildi", fontWeight = FontWeight.ExtraBold)
        }
    }
}
