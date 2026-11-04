package com.payas.quelle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.AppData
import com.payas.quelle.data.Habit
import com.payas.quelle.data.Mood
import com.payas.quelle.data.QuelleStore
import com.payas.quelle.data.habitCount
import com.payas.quelle.data.streak
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.Water
import java.time.LocalDate

/** "Bugün nasılsın?" seçimi. Seçili olana tekrar basınca seçim kalkar. */
@Composable
fun MoodPicker(selected: Mood?, onSelect: (Mood?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bugün nasılsın?", fontWeight = FontWeight.Bold, color = InkMuted)
        Mood.entries.forEach { mood ->
            val isSelected = mood == selected
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) BabyBlueLight else MaterialTheme.colorScheme.surface, RoundedCornerShape(99.dp))
                    .clickable(role = Role.RadioButton) { onSelect(if (isSelected) null else mood) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(14.dp).background(Color(mood.color), CircleShape))
                Text(mood.label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold)
            }
        }
    }
}

data class HabitDialogState(val habit: Habit?)

@Composable
fun HabitsCard(
    data: AppData,
    today: LocalDate,
    onCount: (Habit, Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Habit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayKey = today.toString()
    QuelleCard("Alışkanlıklar", modifier) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.habits.isEmpty()) {
                Text("Su içmek, kitap okumak, erken yatmak gibi alışkanlıklar ekle.", color = InkMuted)
            }
            data.habits.forEach { habit ->
                val count = data.habitCount(habit.id, todayKey)
                val streak = data.streak(habit, today)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(onClickLabel = "Düzenle", role = Role.Button) { onEdit(habit) }
                    ) {
                        Text(habit.name, fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (habit.target > 1) {
                                Text("$count / ${habit.target}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                            }
                            if (streak > 0) {
                                Text(
                                    "$streak gün seri",
                                    modifier = Modifier.background(GreenLight, RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenDeep,
                                )
                            }
                        }
                    }
                    if (habit.target > 1) {
                        IconButton(onClick = { onCount(habit, count - 1) }, enabled = count > 0) {
                            Icon(Icons.Filled.Remove, contentDescription = "${habit.name} azalt")
                        }
                        FilledTonalIconButton(onClick = { onCount(habit, count + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "${habit.name} artır")
                        }
                    } else {
                        val done = count >= 1
                        FilledTonalIconButton(onClick = { onCount(habit, if (done) 0 else 1) }) {
                            if (done) {
                                Icon(Icons.Filled.Check, contentDescription = "${habit.name} yapıldı, geri al", tint = Water)
                            } else {
                                Icon(Icons.Filled.Add, contentDescription = "${habit.name} yapıldı olarak işaretle")
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Alışkanlık ekle", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun HabitDialog(initial: Habit?, onSave: (Habit) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableIntStateOf(initial?.target ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Alışkanlık ekle" else "Alışkanlığı düzenle", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ad (örneğin 8 bardak su)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Günde kaç kez?", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { target-- }, enabled = target > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = "Azalt")
                    }
                    Text("$target", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { target++ }, enabled = target < 20) {
                        Icon(Icons.Filled.Add, contentDescription = "Artır")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Habit(
                            id = initial?.id ?: QuelleStore.newId(),
                            name = name.trim(),
                            target = target,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
            ) { Text("Kaydet", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        },
    )
}
