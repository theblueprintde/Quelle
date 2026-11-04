package com.payas.quelle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.AppData
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.ScheduleKind
import com.payas.quelle.data.formatMinutes
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.Water
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val tr = Locale.forLanguageTag("tr")
private val SHORT_DAYS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
private const val FIRST_HOUR = 7
private const val LAST_HOUR = 23
private val HOUR_HEIGHT = 56.dp

@Composable
fun CalendarScreen() {
    val store = rememberStore()
    val data by store.data.collectAsState()
    val today = rememberNow().toLocalDate()

    var selected by remember { mutableStateOf(today) }
    var editing by remember { mutableStateOf<ScheduleItem?>(null) }
    var creating by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        WeekCard(
            data = data,
            selected = selected,
            today = today,
            onWeekChange = { selected = selected.plusWeeks(it.toLong()) },
            onSelectDay = { selected = it },
            onEdit = { editing = it },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            MonthCard(selected = selected, today = today, data = data, onSelect = { selected = it })
            DayAgendaCard(
                date = selected,
                data = data,
                onAdd = { creating = true },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }

    if (creating) {
        ScheduleDialog(
            initial = null,
            defaultDay = selected.dayOfWeek.value,
            onSave = { items -> items.forEach(store::saveSchedule); creating = false },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
    editing?.let { item ->
        ScheduleDialog(
            initial = item,
            defaultDay = item.dayOfWeek,
            onSave = { items -> items.forEach(store::saveSchedule); editing = null },
            onDelete = { store.deleteSchedule(item.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun WeekCard(
    data: AppData,
    selected: LocalDate,
    today: LocalDate,
    onWeekChange: (Int) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onEdit: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sunday = monday.plusDays(6)
    val rangeFormat = DateTimeFormatter.ofPattern("d MMMM", tr)

    QuelleCard("Haftalık program", modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${monday.dayOfMonth} – ${sunday.format(rangeFormat)}",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onWeekChange(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki hafta")
            }
            IconButton(onClick = { onWeekChange(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki hafta")
            }
        }

        // Gün başlıkları
        Row {
            Spacer(Modifier.width(48.dp))
            for (i in 0..6) {
                val day = monday.plusDays(i.toLong())
                val isToday = day == today
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .background(if (isToday) Water else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { onSelectDay(day) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${SHORT_DAYS[i]} ${day.dayOfMonth}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isToday) Color.White else InkMuted,
                    )
                }
            }
        }

        // Saat ızgarası
        Row(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.width(48.dp)) {
                for (h in FIRST_HOUR until LAST_HOUR) {
                    Text(
                        "%02d:00".format(h),
                        modifier = Modifier.height(HOUR_HEIGHT),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                    )
                }
            }
            for (i in 0..6) {
                val day = monday.plusDays(i.toLong())
                Box(
                    Modifier
                        .weight(1f)
                        .height(HOUR_HEIGHT * (LAST_HOUR - FIRST_HOUR))
                        .background(if (day == today) Color(0xFFF5FBFD) else Color.Transparent)
                ) {
                    // Saat çizgileri
                    for (h in 0 until (LAST_HOUR - FIRST_HOUR)) {
                        Box(
                            Modifier
                                .offset(y = HOUR_HEIGHT * h)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE4EEF0))
                        )
                    }
                    data.scheduleFor(day.dayOfWeek.value).forEach { item ->
                        WeekBlock(item, onClick = { onEdit(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBlock(item: ScheduleItem, onClick: () -> Unit) {
    val top = (item.startMinutes - FIRST_HOUR * 60).coerceAtLeast(0)
    val length = (item.endMinutes - item.startMinutes).coerceAtLeast(20)
    val (bg, fg) = kindColors(item.kind)
    Column(
        Modifier
            .offset(y = HOUR_HEIGHT * (top / 60f))
            .padding(horizontal = 3.dp)
            .fillMaxWidth()
            .height(HOUR_HEIGHT * (length / 60f))
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Düzenle", role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(formatMinutes(item.startMinutes), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun kindColors(kind: ScheduleKind): Pair<Color, Color> = when (kind) {
    ScheduleKind.Ders -> BabyBlueLight to MaterialTheme.colorScheme.onSurface
    ScheduleKind.Calisma, ScheduleKind.Spor -> GreenLight to GreenDeep
    ScheduleKind.Etkinlik -> Color(0xFFFCE9C8) to Color(0xFF7A4E0E)
    ScheduleKind.Diger -> Color(0xFFEEF2F3) to InkMuted
}

@Composable
private fun MonthCard(selected: LocalDate, today: LocalDate, data: AppData, onSelect: (LocalDate) -> Unit) {
    var month by remember(YearMonth.from(selected)) { mutableStateOf(YearMonth.from(selected)) }
    val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", tr)
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()

    QuelleCard("Takvim") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                first.format(monthFormat).replaceFirstChar { it.titlecase(tr) },
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki ay")
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki ay")
            }
        }
        Row {
            listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkMuted)
            }
        }
        for (row in 0 until (cells + 6) / 7) {
            Row {
                for (col in 0..6) {
                    val index = row * 7 + col - leading
                    if (index < 0 || index >= month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.atDay(index + 1)
                        val isSelected = date == selected
                        val hasTasks = data.tasks.any { it.date == date.toString() }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    when {
                                        isSelected -> Water
                                        date == today -> BabyBlueLight
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable(role = Role.Button) { onSelect(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${date.dayOfMonth}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                            if (hasTasks && !isSelected) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 3.dp)
                                        .size(5.dp)
                                        .background(GreenDeep, CircleShape)
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
private fun DayAgendaCard(date: LocalDate, data: AppData, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val title = DateTimeFormatter.ofPattern("d MMMM", tr)
    val items = data.scheduleFor(date.dayOfWeek.value)
    val tasks = data.tasksFor(date.toString())

    QuelleCard("${date.format(title)} günü neler var", modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (items.isEmpty() && tasks.isEmpty()) {
                Text("Bu gün için bir şey yok.", color = InkMuted)
            }
            items.forEach { item ->
                AgendaLine(kindColors(item.kind).first, "${formatMinutes(item.startMinutes)} ${item.title}")
            }
            tasks.forEach { task ->
                AgendaLine(if (task.done) GreenDeep else Color(0xFFE3B66B), if (task.done) "${task.title} (bitti)" else task.title)
            }
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ders veya etkinlik ekle", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AgendaLine(dot: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(10.dp).background(if (dot == BabyBlueLight) Color(0xFF7FC3DE) else dot, CircleShape))
        Text(text, fontWeight = FontWeight.Bold)
    }
}
