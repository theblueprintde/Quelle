package com.payas.quelle.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.QuelleApplication
import com.payas.quelle.admin.AdminLock
import com.payas.quelle.data.AppData
import com.payas.quelle.data.Cleanup
import com.payas.quelle.data.QUOTES
import com.payas.quelle.data.Quote
import com.payas.quelle.data.SONGS
import com.payas.quelle.data.Song
import com.payas.quelle.data.cleanupCount
import com.payas.quelle.data.isValidPin
import com.payas.quelle.data.summary
import com.payas.quelle.data.totalRecords
import com.payas.quelle.sync.SyncStatus
import com.payas.quelle.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val adminTimeFormat = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag("tr"))

private fun formatMoment(ms: Long?): String =
    if (ms == null || ms <= 0) "—" else Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(adminTimeFormat)

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

/** Yönetim paneli: PIN kilidi, veri özeti, senkron, yedekleme, temizlik, söz ve şarkılar. */
@Composable
fun AdminScreen() {
    val context = LocalContext.current
    val lock = remember { AdminLock(context) }
    // Ekrandan çıkınca bu durum unutulur, yani panel her girişte yeniden kilitlenir.
    var unlocked by remember { mutableStateOf(!lock.hasPin) }

    if (unlocked) {
        AdminPanel(lock = lock, onLock = { unlocked = false })
    } else {
        PinGate(check = lock::check, onUnlock = { unlocked = true })
    }
}

@Composable
private fun PinGate(check: (String) -> Boolean, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    fun submit() {
        if (check(pin)) onUnlock() else {
            error = true
            pin = ""
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        QuelleCard("Yönetim paneli", Modifier.widthIn(max = 420.dp), container = Midnight, titleColor = GoldBright) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = GoldBright, modifier = Modifier.size(36.dp))
            Text("Kilitli", fontSize = 30.sp, fontFamily = Display, color = Surface)
            Text("Devam etmek için PIN'ini gir.", color = GoldSoft)
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> if (v.length <= 8 && v.all { it.isDigit() }) { pin = v; error = false } },
                placeholder = { Text("PIN") },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)),
            )
            if (error) Text("PIN yanlış, tekrar dene.", color = Color(0xFFF0B4AE), fontWeight = FontWeight.SemiBold)
            Button(
                onClick = ::submit,
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Midnight),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Kilidi aç", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun AdminPanel(lock: AdminLock, onLock: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as QuelleApplication
    val store = app.store
    val data by store.data.collectAsState()
    val user by app.sync.user.collectAsState()
    val status by app.sync.status.collectAsState()
    val lastSuccess by app.sync.lastSuccess.collectAsState()
    val today = rememberNow().toLocalDate()

    var hasPin by remember { mutableStateOf(lock.hasPin) }
    var pinDialog by remember { mutableStateOf(false) }
    var pendingCleanup by remember { mutableStateOf<Cleanup?>(null) }
    var resetDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<AppData?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(store.encode(store.data.value).toByteArray()) }
                ?: error("Dosya açılamadı")
        }.isSuccess
        toast(if (ok) "Yedek kaydedildi." else "Yedek kaydedilemedi.")
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val parsed = runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Dosya okunamadı")
            store.decode(text)
        }.getOrNull()
        if (parsed == null) toast("Bu dosya bir Quelle yedeği değil.") else pendingImport = parsed
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Verilerin, yedeklerin ve içeriğin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                Text("Yönetim paneli", fontSize = 38.sp, fontWeight = FontWeight.Medium, fontFamily = Display)
            }
            if (hasPin) {
                OutlinedButton(onClick = onLock) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kilitle", fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // 1. sütun: özet ve senkron
            AdminColumn {
                QuelleCard("Veri özeti", Modifier.fillMaxWidth(), container = Midnight, titleColor = GoldBright) {
                    Text("${data.totalRecords()}", fontSize = 44.sp, fontFamily = Display, color = Surface)
                    Text("kayıt · ${formatSize(store.fileSize())}", color = GoldSoft, fontWeight = FontWeight.SemiBold)
                    Text("Son değişiklik: ${formatMoment(data.updatedAt)}", color = GoldSoft, fontSize = 13.sp)
                }
                QuelleCard("Kayıtlar", Modifier.fillMaxWidth()) {
                    data.summary().forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = Line)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.label, Modifier.weight(1f))
                            Text("${item.count}", fontFamily = Display, fontSize = 18.sp, color = if (item.count > 0) Midnight else InkMuted)
                        }
                    }
                }
                QuelleCard("Senkron", Modifier.fillMaxWidth()) {
                    InfoRow("Hesap", user?.email ?: "Giriş yapılmadı")
                    InfoRow("Durum", status.label)
                    InfoRow("Son başarılı", formatMoment(lastSuccess))
                    ActionButton(
                        text = "Şimdi eşitle",
                        icon = Icons.Outlined.Sync,
                        enabled = user != null && status != SyncStatus.Syncing,
                        onClick = { if (!app.sync.syncNow()) toast("Önce Google ile giriş yap.") },
                    )
                }
            }

            // 2. sütun: yedekleme, temizlik, güvenlik
            AdminColumn {
                QuelleCard("Yedekleme", Modifier.fillMaxWidth()) {
                    Text("Tüm verilerini tek bir dosyaya kaydet ya da eski bir yedeği geri yükle.", color = InkMuted)
                    ActionButton("Yedeği dışa aktar", Icons.Outlined.FileDownload) {
                        exportLauncher.launch("quelle-yedek-$today.json")
                    }
                    ActionButton("Yedekten geri yükle", Icons.Outlined.FileUpload, outlined = true) {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }
                }
                QuelleCard("Temizlik", Modifier.fillMaxWidth()) {
                    Cleanup.entries.forEachIndexed { i, c ->
                        if (i > 0) HorizontalDivider(color = Line)
                        val count = data.cleanupCount(c, today)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.label, fontWeight = FontWeight.SemiBold)
                                Text("$count kayıt", fontSize = 13.sp, color = InkMuted)
                            }
                            TextButton(onClick = { pendingCleanup = c }, enabled = count > 0) {
                                Text("Temizle", color = if (count > 0) Danger else InkMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                QuelleCard("Güvenlik", Modifier.fillMaxWidth()) {
                    if (!hasPin) {
                        Text(
                            "PIN belirlenmedi, panel herkese açık.",
                            color = Warn,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().background(WarnSoft, RoundedCornerShape(10.dp)).padding(12.dp),
                        )
                    }
                    ActionButton(if (hasPin) "PIN'i değiştir" else "PIN belirle", Icons.Outlined.Lock) { pinDialog = true }
                    if (hasPin) {
                        TextButton(onClick = {
                            lock.clearPin()
                            hasPin = false
                            toast("PIN kaldırıldı.")
                        }) { Text("PIN'i kaldır", color = Danger, fontWeight = FontWeight.Bold) }
                    }
                }
                QuelleCard("Tehlikeli bölge", Modifier.fillMaxWidth(), container = DangerSoft, titleColor = Danger) {
                    Text("Tüm görevler, notlar, program ve geçmiş silinir. Senkron açıksa buluttaki kopya da boşalır.", color = Danger)
                    Button(
                        onClick = { resetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Tüm verileri sıfırla", fontWeight = FontWeight.ExtraBold) }
                }
            }

            // 3. sütun: içerik
            AdminColumn {
                ContentCard(
                    title = "Günün sözleri",
                    hint = "${QUOTES.size} hazır söz var. Eklediklerin sıraya katılır.",
                    firstLabel = "Söz",
                    secondLabel = "Kimden",
                    items = data.customQuotes.map { "“${it.text}”" to it.author },
                    onAdd = { a, b -> store.addQuote(Quote(a, b)) },
                    onRemove = store::removeQuote,
                )
                ContentCard(
                    title = "Günün şarkıları",
                    hint = "${SONGS.size} hazır şarkı var. Eklediklerin sıraya katılır.",
                    firstLabel = "Şarkı",
                    secondLabel = "Sanatçı",
                    items = data.customSongs.map { it.title to it.artist },
                    onAdd = { a, b -> store.addSong(Song(a, b)) },
                    onRemove = store::removeSong,
                )
            }
        }
    }

    pendingCleanup?.let { c ->
        ConfirmDialog(
            title = c.label,
            text = "${c.description} ${data.cleanupCount(c, today)} kayıt silinecek. Bu geri alınamaz.",
            confirmLabel = "Temizle",
            onDismiss = { pendingCleanup = null },
            onConfirm = {
                store.cleanup(c, today)
                pendingCleanup = null
                toast("Temizlendi.")
            },
        )
    }

    if (resetDialog) {
        ConfirmDialog(
            title = "Tüm veriler silinsin mi?",
            text = "Bu geri alınamaz. İstersen önce yedeği dışa aktar. Onaylamak için SIFIRLA yaz.",
            confirmLabel = "Sıfırla",
            requiredWord = "SIFIRLA",
            onDismiss = { resetDialog = false },
            onConfirm = {
                store.resetAll()
                resetDialog = false
                toast("Tüm veriler silindi.")
            },
        )
    }

    pendingImport?.let { imported ->
        ConfirmDialog(
            title = "Yedek geri yüklensin mi?",
            text = "Bu yedekte ${imported.totalRecords()} kayıt var (son değişiklik ${formatMoment(imported.updatedAt)}). " +
                "Şu anki verilerin yerini alacak.",
            confirmLabel = "Geri yükle",
            onDismiss = { pendingImport = null },
            onConfirm = {
                store.importData(imported)
                pendingImport = null
                toast("Yedek geri yüklendi.")
            },
        )
    }

    if (pinDialog) {
        PinDialog(
            onDismiss = { pinDialog = false },
            onSave = { pin ->
                lock.setPin(pin)
                hasPin = true
                pinDialog = false
                toast("PIN kaydedildi.")
            },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AdminColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, Modifier.width(110.dp), color = InkMuted)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    }
}

/** Söz ya da şarkı ekleme formu ve eklenenlerin listesi. */
@Composable
private fun ContentCard(
    title: String,
    hint: String,
    firstLabel: String,
    secondLabel: String,
    items: List<Pair<String, String>>,
    onAdd: (String, String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }

    QuelleCard(title, Modifier.fillMaxWidth()) {
        Text(hint, color = InkMuted, fontSize = 13.sp)
        OutlinedTextField(
            value = first,
            onValueChange = { first = it },
            label = { Text(firstLabel) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = second,
            onValueChange = { second = it },
            label = { Text(secondLabel) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onAdd(first, second)
                first = ""
                second = ""
            },
            enabled = first.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Ekle", fontWeight = FontWeight.Bold) }

        if (items.isEmpty()) {
            Text("Henüz eklediğin yok.", color = InkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        items.forEachIndexed { i, (main, sub) ->
            HorizontalDivider(color = Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(main, fontFamily = Display, fontSize = 16.sp)
                    if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = Gold, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { onRemove(i) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Sil", tint = InkMuted)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    requiredWord: String? = null,
) {
    var typed by remember { mutableStateOf("") }
    val canConfirm = requiredWord == null || typed.trim().equals(requiredWord, ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text)
                if (requiredWord != null) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        placeholder = { Text(requiredWord) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(confirmLabel, color = if (canConfirm) Danger else InkMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val error = when {
        pin.isNotEmpty() && !isValidPin(pin) -> "PIN 4 ile 8 rakam arasında olmalı."
        repeat.isNotEmpty() && repeat != pin -> "İki PIN aynı değil."
        else -> null
    }
    val pinField: @Composable (String, String, (String) -> Unit) -> Unit = { label, value, onChange ->
        OutlinedTextField(
            value = value,
            onValueChange = { v -> if (v.length <= 8 && v.all { it.isDigit() }) onChange(v) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yönetim PIN'i", fontFamily = Display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PIN yalnızca bu tablette saklanır ve paneli her açışında sorulur.", color = InkMuted)
                pinField("Yeni PIN", pin) { pin = it }
                pinField("PIN tekrar", repeat) { repeat = it }
                if (error != null) Text(error, color = Danger, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin) }, enabled = isValidPin(pin) && pin == repeat) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
