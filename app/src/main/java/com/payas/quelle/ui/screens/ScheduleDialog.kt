package com.payas.quelle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payas.quelle.data.QuelleStore
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.ScheduleKind
import com.payas.quelle.data.formatMinutes

private val DAY_NAMES = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

/**
 * Program öğesi ekleme / düzenleme penceresi.
 * Yeni eklerken birden fazla gün seçilebilir (aynı ders haftada iki gün varsa).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleDialog(
    initial: ScheduleItem?,
    defaultDay: Int,
    onSave: (List<ScheduleItem>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: ScheduleKind.Ders) }
    var days by remember { mutableStateOf(setOf(initial?.dayOfWeek ?: defaultDay)) }
    var start by remember { mutableStateOf(initial?.startMinutes ?: (8 * 60 + 30)) }
    var end by remember { mutableStateOf(initial?.endMinutes ?: (9 * 60 + 50)) }
    var picking by remember { mutableStateOf<String?>(null) }

    val valid = title.isNotBlank() && days.isNotEmpty() && end > start

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Programa ekle" else "Programı düzenle", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ad (örneğin Matematik)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tür", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleKind.entries.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                    }
                }
                Text(if (initial == null) "Günler" else "Gün", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_NAMES.forEachIndexed { index, name ->
                        val day = index + 1
                        FilterChip(
                            selected = day in days,
                            onClick = {
                                days = if (initial != null) setOf(day)
                                else if (day in days) days - day else days + day
                            },
                            label = { Text(name) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { picking = "start" }) { Text("Başlangıç: ${formatMinutes(start)}") }
                    OutlinedButton(onClick = { picking = "end" }) { Text("Bitiş: ${formatMinutes(end)}") }
                }
                if (end <= start) {
                    Text("Bitiş saati başlangıçtan sonra olmalı.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val items = days.sorted().mapIndexed { i, day ->
                        ScheduleItem(
                            id = if (initial != null && i == 0) initial.id else QuelleStore.newId(),
                            title = title.trim(),
                            dayOfWeek = day,
                            startMinutes = start,
                            endMinutes = end,
                            kind = kind,
                        )
                    }
                    onSave(items)
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

    picking?.let { which ->
        TimePickerDialog(
            initialMinutes = if (which == "start") start else end,
            onConfirm = { m ->
                if (which == "start") {
                    val length = end - start
                    start = m
                    if (end <= start) end = minOf(start + maxOf(length, 30), 23 * 60 + 59)
                } else {
                    end = m
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Tamam", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
