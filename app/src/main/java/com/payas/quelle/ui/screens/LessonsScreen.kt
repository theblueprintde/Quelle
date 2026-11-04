package com.payas.quelle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.Assignment
import com.payas.quelle.data.AssignmentKind
import com.payas.quelle.data.Note
import com.payas.quelle.data.NoteColor
import com.payas.quelle.data.QuelleStore
import com.payas.quelle.data.daysUntil
import com.payas.quelle.data.sortedAssignments
import com.payas.quelle.ui.theme.BabyBlueLight
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted
import com.payas.quelle.ui.theme.WaterDeep
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val trLocale = Locale.forLanguageTag("tr")
private val dueFormat = DateTimeFormatter.ofPattern("d MMMM EEEE", trLocale)

private data class AssignmentDialogState(val item: Assignment?, val kind: AssignmentKind)
private data class NoteDialogState(val note: Note?)

@Composable
fun LessonsScreen() {
    val store = rememberStore()
    val data by store.data.collectAsState()
    val today = rememberNow().toLocalDate()
    var assignmentDialog by remember { mutableStateOf<AssignmentDialogState?>(null) }
    var noteDialog by remember { mutableStateOf<NoteDialogState?>(null) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        QuelleCard("Ödev ve sınavlar", Modifier.weight(1f).fillMaxHeight()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AssignmentKind.entries.forEach { kind ->
                    val list = data.sortedAssignments(kind)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (kind == AssignmentKind.Sinav) "Sınavlar" else "Ödevler",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { assignmentDialog = AssignmentDialogState(null, kind) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(if (kind == AssignmentKind.Sinav) "Sınav ekle" else "Ödev ekle", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (list.isEmpty()) {
                        Text(
                            if (kind == AssignmentKind.Sinav) "Yaklaşan sınav yok." else "Bekleyen ödev yok.",
                            color = InkMuted,
                        )
                    }
                    list.forEach { a ->
                        AssignmentRow(
                            a,
                            today,
                            onToggle = { store.saveAssignment(a.copy(done = !a.done)) },
                            onClick = { assignmentDialog = AssignmentDialogState(a, a.kind) },
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }
        }

        Column(Modifier.width(420.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notlar", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                FilledIconButton(onClick = { noteDialog = NoteDialogState(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Not ekle")
                }
            }
            if (data.notes.isEmpty()) {
                Text("Ders notlarını, kelimeleri, fikirlerini buraya yazabilirsin.", color = InkMuted)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(data.notes.sortedByDescending { it.updatedAt }, key = { it.id }) { note ->
                    Column(
                        Modifier
                            .heightIn(min = 140.dp)
                            .background(Color(note.color.argb), RoundedCornerShape(20.dp))
                            .clickable(onClickLabel = "Notu aç", role = Role.Button) { noteDialog = NoteDialogState(note) }
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(note.title.ifBlank { "Başlıksız" }, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(note.body, fontSize = 14.sp, maxLines = 6, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    }
                }
            }
        }
    }

    assignmentDialog?.let { state ->
        AssignmentDialog(
            initial = state.item,
            kind = state.kind,
            today = today,
            onSave = { store.saveAssignment(it); assignmentDialog = null },
            onDelete = state.item?.let { a -> { store.deleteAssignment(a.id); assignmentDialog = null } },
            onDismiss = { assignmentDialog = null },
        )
    }
    noteDialog?.let { state ->
        NoteDialog(
            initial = state.note,
            onSave = { store.saveNote(it); noteDialog = null },
            onDelete = state.note?.let { n -> { store.deleteNote(n.id); noteDialog = null } },
            onDismiss = { noteDialog = null },
        )
    }
}

@Composable
private fun AssignmentRow(a: Assignment, today: LocalDate, onToggle: () -> Unit, onClick: () -> Unit) {
    val days = daysUntil(a.dueDate, today)
    val (badgeBg, badgeFg) = when {
        a.done -> Color(0xFF3E7D52) to Color.White
        days < 0 -> Color(0xFFF6D3D3) to Color(0xFF7A1F1F)
        days <= 3 && a.kind == AssignmentKind.Sinav -> Color(0xFFF6D3D3) to Color(0xFF7A1F1F)
        days <= 3 -> Color(0xFFFCE9C8) to Color(0xFF7A4E0E)
        else -> BabyBlueLight to WaterDeep
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (a.done) Color.White else Color(0xFFF5FBFD), RoundedCornerShape(16.dp))
            .border(if (a.done) 2.dp else 0.dp, Color(0xFFE4EEF0), RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Düzenle", role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(badgeBg, RoundedCornerShape(14.dp))
                .clickable(role = Role.Checkbox, onClickLabel = if (a.done) "Bitmedi olarak işaretle" else "Bitti olarak işaretle", onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (a.done) {
                Icon(Icons.Filled.Check, contentDescription = "Bitti", tint = badgeFg)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            days < 0 -> "!"
                            days == 0L -> "BUGÜN"
                            else -> "$days"
                        },
                        fontSize = if (days == 0L) 13.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeFg,
                    )
                    if (days > 0) Text("GÜN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = badgeFg)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                a.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (a.done) InkMuted else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (a.done) TextDecoration.LineThrough else null,
            )
            val dateText = LocalDate.parse(a.dueDate).format(dueFormat)
            Text(
                if (a.detail.isBlank()) dateText else "$dateText · ${a.detail}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkMuted,
            )
        }
        if (!a.done && a.kind == AssignmentKind.Sinav) {
            Text(
                "Hazırlık %${a.progress}",
                modifier = Modifier
                    .background(if (a.progress >= 50) GreenLight else Color(0xFFEEF2F3), RoundedCornerShape(99.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (a.progress >= 50) GreenDeep else InkMuted,
            )
        } else if (days < 0 && !a.done) {
            Text("Gecikti", color = Color(0xFF7A1F1F), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentDialog(
    initial: Assignment?,
    kind: AssignmentKind,
    today: LocalDate,
    onSave: (Assignment) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var detail by remember { mutableStateOf(initial?.detail ?: "") }
    var due by remember { mutableStateOf(initial?.dueDate?.let { LocalDate.parse(it) } ?: today.plusDays(7)) }
    var progress by remember { mutableFloatStateOf((initial?.progress ?: 0).toFloat()) }
    var pickingDate by remember { mutableStateOf(false) }
    val isExam = kind == AssignmentKind.Sinav

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "Yeni ${kind.label.lowercase(trLocale)}" else kind.label,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isExam) "Sınav (örneğin Matematik sınavı)" else "Ödev (örneğin Almanca kompozisyon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text(if (isExam) "Konular" else "Açıklama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { pickingDate = true }) {
                    Text("Tarih: ${due.format(dueFormat)}")
                }
                if (isExam) {
                    Text("Hazırlık: %${progress.toInt()}", fontWeight = FontWeight.Bold)
                    Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f, steps = 9)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        Assignment(
                            id = initial?.id ?: QuelleStore.newId(),
                            title = title.trim(),
                            kind = kind,
                            dueDate = due.toString(),
                            detail = detail.trim(),
                            progress = progress.toInt(),
                            done = initial?.done ?: false,
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

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = due.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("Tamam", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Vazgeç") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteDialog(initial: Note?, onSave: (Note) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: NoteColor.Mavi) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yeni not" else "Not", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Not") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteColor.entries.forEach { c ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(c.argb), CircleShape)
                                .border(if (c == color) 3.dp else 1.dp, if (c == color) WaterDeep else Color(0xFFD3E3E6), CircleShape)
                                .clickable(role = Role.RadioButton, onClickLabel = "Renk seç") { color = c },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() || body.isNotBlank(),
                onClick = {
                    onSave(
                        Note(
                            id = initial?.id ?: QuelleStore.newId(),
                            title = title.trim(),
                            body = body.trim(),
                            color = color,
                            updatedAt = System.currentTimeMillis(),
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
