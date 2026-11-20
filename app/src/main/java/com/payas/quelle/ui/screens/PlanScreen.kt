package com.payas.quelle.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.Finding
import com.payas.quelle.data.ParsedPlan
import com.payas.quelle.data.PlanEntry
import com.payas.quelle.data.PlanLayout
import com.payas.quelle.data.PlanReport
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.Severity
import com.payas.quelle.data.WEEKDAY_NAMES
import com.payas.quelle.data.analyzePlan
import com.payas.quelle.data.formatDuration
import com.payas.quelle.data.formatMinutes
import com.payas.quelle.data.parsePlan
import com.payas.quelle.pdf.PdfReadException
import com.payas.quelle.pdf.PdfWords
import com.payas.quelle.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class PlanResult(val plan: ParsedPlan, val report: PlanReport)

/** Ekranlar arasında geçince analiz kaybolmasın diye sonuç uygulama açık kaldıkça burada durur. */
private object PlanState {
    var result by mutableStateOf<PlanResult?>(null)
    var error by mutableStateOf<String?>(null)
    var excluded by mutableStateOf(setOf("Mola"))
    var placedCount by mutableStateOf<Int?>(null)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen() {
    val context = LocalContext.current
    val store = rememberStore()
    val data by store.data.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    fun load(uri: Uri) {
        loading = true
        PlanState.error = null
        PlanState.placedCount = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val words = context.contentResolver.openInputStream(uri)?.use { PdfWords.read(it) }
                        ?: throw PdfReadException("Dosya açılamadı.")
                    if (words.isEmpty()) throw PdfReadException(
                        "PDF'te seçilebilir metin yok; büyük ihtimalle taranmış bir görüntü. Planı bir uygulamadan doğrudan PDF olarak dışa aktar."
                    )
                    val plan = parsePlan(words)
                    PlanResult(plan, analyzePlan(plan.entries, plan.statedTotals))
                }
                PlanState.result = result
                if (result.plan.entries.isEmpty()) PlanState.error =
                    "Metin okundu ama gün + saat aralığı (ör. Pazartesi, 09:00–10:30) eşleşmesi bulunamadı. Okunan metin aşağıda."
            } catch (e: PdfReadException) {
                PlanState.error = e.message
            } catch (e: Throwable) {
                // Error dahil her şeyi yakala: bozuk/beklenmedik bir PDF uygulamayı kapatmasın.
                if (e is kotlinx.coroutines.CancellationException) throw e
                PlanState.error = "PDF okunamadı (${e.javaClass.simpleName}): ${e.message.orEmpty().take(200)}"
            } finally {
                loading = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::load) }
    val result = PlanState.result
    val selected = result?.plan?.entries?.filter { it.category !in PlanState.excluded }.orEmpty()
    val previousPdf = data.schedule.count { it.source == ScheduleItem.SOURCE_PDF }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    result?.let { "${it.plan.entries.size} öğe · ${formatDuration(it.report.totalMinutes)} planlı · ${it.plan.layout.label}" }
                        ?: "PDF tablette okunur, hiçbir yere gönderilmez.",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkMuted,
                )
                Text("Haftalık plan", fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = Display)
            }
            if (loading) CircularProgressIndicator(Modifier.padding(end = 8.dp))
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = !loading, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Text("  PDF yükle", fontWeight = FontWeight.ExtraBold)
            }
            Button(onClick = { confirm = true }, enabled = selected.isNotEmpty() && !loading, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = null)
                Text("  Programa yerleştir (${selected.size})", fontWeight = FontWeight.ExtraBold)
            }
        }

        PlanState.error?.let { Banner(it, DangerSoft, Danger) }
        PlanState.placedCount?.let {
            Banner("$it öğe programa yerleştirildi. Önceki PDF'ten gelenler kaldırıldı, elle eklediklerin duruyor.", Sage, Emerald)
        }

        if (result == null) {
            QuelleCard("Nasıl çalışır") {
                Text("1. Haftalık rutin PDF'ini yükle. Her gün için \"Saat · Ne yapılacak · Tür · Süre\" tablosu olan biçim doğrudan tanınır; gün başlıklı listeler ve günlerin sütun olduğu tablolar da okunur.")
                Text("2. Uygulama yükü, çakışmaları, uyku aralığını, arasız uzun blokları ve PDF'teki toplamların satırlarla tutup tutmadığını kontrol eder.")
                Text("3. \"Programa yerleştir\" planı Takvim ve Bugün ekranına koyar. Her hafta yeni PDF yüklediğinde öncekinin yerine geçer; elle eklediğin dersler silinmez.")
                Text("Not: PDF taranmış bir fotoğrafsa içinde metin yoktur ve okunamaz.", color = InkMuted)
            }
        } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            QuelleCard("Hafta", Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayBars(result.report)
                    Text("TÜRLER · programa eklenecekleri seç", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = InkMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.report.minutesByCategory.forEach { (cat, min) ->
                            val on = cat !in PlanState.excluded
                            FilterChip(
                                selected = on,
                                onClick = { PlanState.excluded = if (on) PlanState.excluded + cat else PlanState.excluded - cat },
                                label = { Text("$cat · ${formatDuration(min)}", fontWeight = FontWeight.Bold) },
                            )
                        }
                    }
                }
            }
            QuelleCard("Bulgular", Modifier.weight(1f).fillMaxHeight(), container = Champagne) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (result.report.findings.isEmpty()) Text("Belirgin bir sorun bulunmadı.", color = InkMuted)
                    result.report.findings.take(40).forEach { FindingRow(it) }
                    if (result.report.freeSlots.isNotEmpty()) {
                        Text("PLANLANMAMIŞ BOŞLUKLAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = InkMuted)
                        result.report.freeSlots.forEach {
                            Text("${WEEKDAY_NAMES[it.dayOfWeek - 1]} ${formatMinutes(it.start)}–${formatMinutes(it.end)}")
                        }
                    }
                }
            }
            QuelleCard("Okunan plan", Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (result.plan.layout == PlanLayout.None) {
                        Text(result.plan.rawText.take(4000), fontSize = 13.sp, color = InkMuted)
                    }
                    // Kaydırılan sütunun yüksekliği sınırlı (Compose ~260 bin px üstünü çizemez); liste makul tutulur.
                    result.plan.entries.take(300).groupBy { it.dayOfWeek }.forEach { (day, list) ->
                        Text(WEEKDAY_NAMES[day - 1], fontSize = 20.sp, fontFamily = Display, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                        list.forEach { EntryRow(it, it.category !in PlanState.excluded) }
                    }
                    if (result.plan.unmatched.isNotEmpty()) {
                        Text("GÜNE BAĞLANAMAYAN SATIRLAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = InkMuted)
                        result.plan.unmatched.take(30).forEach { Text(it, fontSize = 13.sp, color = InkMuted) }
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Programa yerleştir") },
            text = {
                Text(
                    buildString {
                        append("${selected.size} öğe programa eklenecek")
                        if (previousPdf > 0) append(", önceki PDF'ten gelen $previousPdf öğe kaldırılacak")
                        append(". Elle eklediğin dersler olduğu gibi kalır.")
                        val left = (result?.plan?.entries?.size ?: 0) - selected.size
                        if (left > 0) append(" Seçili olmayan türlerdeki $left öğe eklenmeyecek.")
                        append(" Her öğe için başlamadan 15 dakika önce hatırlatma gelir.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.replacePdfSchedule(selected)
                    PlanState.placedCount = selected.size
                    confirm = false
                }) { Text("Yerleştir", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Banner(text: String, background: Color, color: Color) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().background(background, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@Composable
private fun DayBars(report: PlanReport) {
    val max = (report.minutesByDay.maxOrNull() ?: 0).coerceAtLeast(60)
    Text("Koyu: yük (ders, çalışma, iş, yol) · açık: toplam planlı", fontSize = 12.sp, color = InkMuted)
    WEEKDAY_NAMES.forEachIndexed { i, name ->
        val total = report.minutesByDay[i]
        val load = report.loadByDay[i]
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row {
                Text(name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("${formatDuration(load)} / ${formatDuration(total)}", fontSize = 13.sp, color = InkMuted)
            }
            Box(Modifier.fillMaxWidth().height(12.dp).background(Mist, RoundedCornerShape(6.dp))) {
                Box(Modifier.fillMaxWidth(total.toFloat() / max).height(12.dp).background(GoldSoft, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(load.toFloat() / max).height(12.dp).background(Midnight, RoundedCornerShape(6.dp)))
            }
            report.nightGaps[i]?.let {
                Text("Gece arası ${formatDuration(it)}", fontSize = 12.sp, color = if (it < 7 * 60) Danger else InkMuted)
            }
        }
    }
}

@Composable
private fun FindingRow(f: Finding) {
    val (bg, fg) = when (f.severity) {
        Severity.Uyari -> DangerSoft to Danger
        Severity.Oneri -> WarnSoft to Warn
        Severity.Bilgi -> Mist to InkMuted
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            f.severity.label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            modifier = Modifier.width(64.dp).background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
        )
        Text(f.text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EntryRow(e: PlanEntry, included: Boolean) {
    val alpha = if (included) 1f else 0.45f
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "${formatMinutes(e.startMinutes)}–${formatMinutes(e.endMinutes)}${if (e.estimatedEnd) "?" else ""}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkMuted.copy(alpha = alpha),
            modifier = Modifier.width(96.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(e.title, color = Ink.copy(alpha = alpha), fontWeight = FontWeight.SemiBold)
            val sub = listOf(e.category, e.note).filter { it.isNotEmpty() }.joinToString(" · ")
            Text(sub, fontSize = 12.sp, color = InkMuted.copy(alpha = alpha))
        }
    }
}
