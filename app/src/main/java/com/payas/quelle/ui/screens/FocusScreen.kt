package com.payas.quelle.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.DAILY_FOCUS_GOAL
import com.payas.quelle.data.TimerMode
import com.payas.quelle.data.focusSessionsOn
import com.payas.quelle.ui.theme.BabyBlue
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.Green
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.Water
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun FocusScreen() {
    val store = rememberStore()
    val data by store.data.collectAsState()
    val today = LocalDate.now().toString()
    val timer = data.timer

    var mode by remember { mutableStateOf(timer?.mode ?: TimerMode.Odak) }
    var label by remember { mutableStateOf(timer?.label ?: "") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (timer != null && !timer.isPaused) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
        nowMs = System.currentTimeMillis()
    }

    val activeMode = timer?.mode ?: mode
    val total = activeMode.minutes * 60_000L
    val remaining = timer?.remaining(nowMs) ?: total
    val seconds = (remaining + 999) / 1000
    val sessions = data.focusSessionsOn(today)
    val context = LocalContext.current

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        QuelleCard("Odak zamanlayıcı", Modifier.weight(1f).fillMaxHeight(), container = BabyBlueLight) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerMode.entries.forEach { m ->
                        FilterChip(
                            selected = activeMode == m,
                            enabled = timer == null,
                            onClick = { mode = m },
                            label = { Text("${m.label} ${m.minutes} dk", fontWeight = FontWeight.Bold) },
                        )
                    }
                }
                Box(Modifier.size(340.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { if (total == 0L) 0f else remaining.toFloat() / total },
                        modifier = Modifier.fillMaxSize(),
                        color = Water,
                        trackColor = BabyBlue,
                        strokeWidth = 14.dp,
                        strokeCap = StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 76.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            (timer?.label ?: label).ifBlank { activeMode.label },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkMuted,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val (text, action) = when {
                        timer == null -> "Başlat" to { store.startTimer(mode, label) }
                        timer.isPaused -> "Devam et" to { store.resumeTimer() }
                        else -> "Duraklat" to { store.pauseTimer() }
                    }
                    Button(onClick = action, modifier = Modifier.width(180.dp).height(56.dp)) {
                        Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(onClick = { store.resetTimer() }, enabled = timer != null, modifier = Modifier.width(140.dp).height(56.dp)) {
                        Text("Sıfırla", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            QuelleCard("Bugün") {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${sessions.size}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "oturum · ${sessions.sumOf { it.minutes }} dk odak",
                        modifier = Modifier.padding(bottom = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until DAILY_FOCUS_GOAL) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(10.dp)
                                .background(if (i < sessions.size) Green else Color(0xFFD3E3E6), RoundedCornerShape(99.dp))
                        )
                    }
                }
                Text("Günlük hedef: $DAILY_FOCUS_GOAL oturum", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkMuted)
            }

            QuelleCard("Neye odaklanıyorsun?", Modifier.weight(1f).fillMaxWidth()) {
                val open = data.tasksFor(today).filterNot { it.done }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (open.isEmpty()) {
                        Text("Bugünkü görevlerin bitti ya da henüz görev eklemedin.", color = InkMuted)
                    }
                    open.forEach { task ->
                        val selected = (timer?.label ?: label) == task.title
                        Text(
                            task.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .border(2.dp, if (selected) Water else Color(0xFFE4EEF0), RoundedCornerShape(14.dp))
                                .clickable(enabled = timer == null, role = Role.RadioButton) {
                                    label = if (selected) "" else task.title
                                }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                        )
                    }
                }
                Button(
                    onClick = {
                        val uri = Uri.parse("https://music.youtube.com/search?q=lofi+study+music")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenLight, contentColor = GreenDeep),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Odak müziği aç (YouTube Music)", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
