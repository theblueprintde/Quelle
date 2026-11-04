package com.payas.quelle.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.ScheduleKind
import com.payas.quelle.data.Task
import com.payas.quelle.data.formatMinutes
import com.payas.quelle.data.goalFor
import com.payas.quelle.data.moodFor
import com.payas.quelle.data.songFor
import com.payas.quelle.data.quoteFor
import com.payas.quelle.greetingFor
import com.payas.quelle.ui.theme.BabyBlue
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.Water
import java.time.format.DateTimeFormatter
import java.util.Locale

private val turkish = Locale.forLanguageTag("tr")
private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", turkish)

@Composable
fun TodayScreen() {
    val store = rememberStore()
    val data by store.data.collectAsState()
    val now = rememberNow()
    val today = now.toLocalDate()
    val todayKey = today.toString()
    val minutesNow = now.hour * 60 + now.minute

    var editing by remember { mutableStateOf<ScheduleItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var habitDialog by remember { mutableStateOf<HabitDialogState?>(null) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    now.format(dateFormat).replaceFirstChar { it.titlecase(turkish) },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("${greetingFor(now.hour)}, Payas", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            MoodPicker(
                selected = data.moodFor(todayKey),
                onSelect = { store.setMood(todayKey, it) },
            )
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val quote = quoteFor(today.dayOfYear)
                QuelleCard("Günün sözü", Modifier.fillMaxWidth(), container = BabyBlue) {
                    Text("“${quote.text}”", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                    Text(quote.author, fontWeight = FontWeight.Bold)
                }
                GoalCard(
                    goal = data.goalFor(todayKey),
                    onSave = { store.setGoal(todayKey, it) },
                    date = todayKey,
                )
                HabitsCard(
                    data = data,
                    today = today,
                    onCount = { habit, count -> store.setHabitCount(habit.id, todayKey, count) },
                    onAdd = { habitDialog = HabitDialogState(null) },
                    onEdit = { habitDialog = HabitDialogState(it) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            ProgramCard(
                items = data.scheduleFor(today.dayOfWeek.value),
                minutesNow = minutesNow,
                onAdd = { creating = true },
                onEdit = { editing = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SongCard(songFor(today.dayOfYear))
                TasksCard(
                    tasks = data.tasksFor(todayKey),
                    onAdd = { store.addTask(it, todayKey) },
                    onToggle = { store.toggleTask(it.id) },
                    onDelete = { store.deleteTask(it.id) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }

    if (creating) {
        ScheduleDialog(
            initial = null,
            defaultDay = today.dayOfWeek.value,
            onSave = { items -> items.forEach(store::saveSchedule); creating = false },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
    habitDialog?.let { state ->
        HabitDialog(
            initial = state.habit,
            onSave = { store.saveHabit(it); habitDialog = null },
            onDelete = state.habit?.let { h -> { store.deleteHabit(h.id); habitDialog = null } },
            onDismiss = { habitDialog = null },
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
private fun ProgramCard(
    items: List<ScheduleItem>,
    minutesNow: Int,
    onAdd: () -> Unit,
    onEdit: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    QuelleCard("Saat saat program", modifier) {
        if (items.isEmpty()) {
            Text(
                "Bugün için programında bir şey yok. Derslerini bir kez eklersen her hafta kendiliğinden tekrarlanır.",
                color = InkMuted,
            )
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                ProgramRow(item, isNow = item.isNow(minutesNow), onClick = { onEdit(item) })
            }
        }
        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Programa ekle", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ProgramRow(item: ScheduleItem, isNow: Boolean, onClick: () -> Unit) {
    val (bg, fg) = when {
        isNow -> Water to Color.White
        item.kind == ScheduleKind.Ders -> BabyBlueLight to MaterialTheme.colorScheme.onSurface
        item.kind == ScheduleKind.Calisma || item.kind == ScheduleKind.Spor -> GreenLight to GreenDeep
        else -> Color(0xFFF3F6F7) to MaterialTheme.colorScheme.onSurface
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            formatMinutes(item.startMinutes),
            modifier = Modifier.width(48.dp).padding(top = 12.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = InkMuted,
        )
        Column(
            Modifier
                .weight(1f)
                .background(bg, RoundedCornerShape(16.dp))
                .clickable(onClickLabel = "Düzenle", role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(if (isNow) "${item.title} · şimdi" else item.title, fontWeight = FontWeight.ExtraBold, color = fg)
            Text(
                "${item.kind.label} · ${formatMinutes(item.startMinutes)}–${formatMinutes(item.endMinutes)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isNow) fg else InkMuted,
            )
        }
    }
}

@Composable
private fun TasksCard(
    tasks: List<Task>,
    onAdd: (String) -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }

    QuelleCard("Görevler", modifier) {
        if (tasks.isNotEmpty()) {
            Text(
                "${tasks.count { it.done }} / ${tasks.size} tamamlandı",
                fontWeight = FontWeight.ExtraBold,
                color = GreenDeep,
            )
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(task, onToggle = { onToggle(task) }, onDelete = { onDelete(task) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Yeni görev") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = submit, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Görev ekle")
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(if (task.done) Color(0xFF3E7D52) else Color.White, RoundedCornerShape(9.dp))
                .border(2.dp, if (task.done) Color(0xFF3E7D52) else Color(0xFF9BB8BF), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            task.title,
            modifier = Modifier.weight(1f),
            fontWeight = if (task.done) FontWeight.SemiBold else FontWeight.Bold,
            color = if (task.done) InkMuted else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Görevi sil", tint = InkMuted)
        }
    }
}
