package com.payas.quelle.data

import java.util.Locale
import kotlin.math.abs

/**
 * PDF'ten okunan tek bir kelime ve sayfadaki konumu.
 * Koordinatlar PdfBox'taki gibi: x soldan sağa, y yukarıdan aşağı büyür.
 */
data class PdfWord(
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String,
) {
    val centerX get() = x + width / 2
}

/** Plandan çıkarılan bir öğe. */
data class PlanEntry(
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val title: String,
    val kind: ScheduleKind,
    /** PDF'teki tür etiketi (ör. "Almanca", "Tekrar"); yoksa türün adı. */
    val category: String = kind.label,
    /** Satırın altındaki küçük açıklama (ör. "Pasif, 21 saate sayılmaz"). */
    val note: String = "",
    /** Bitiş saati PDF'te yazmıyorsa tahmin edildi (sonraki öğe ya da 1 saat). */
    val estimatedEnd: Boolean = false,
) {
    val duration get() = endMinutes - startMinutes
}

enum class PlanLayout(val label: String) {
    DayTables("Gün sayfaları (Saat / Ne yapılacak / Tür / Süre)"),
    Columns("Tablo (günler sütun)"),
    Lines("Liste (gün başlıkları altında)"),
    None("Tanınmadı"),
}

data class ParsedPlan(
    val entries: List<PlanEntry>,
    val layout: PlanLayout,
    /** Saat içeren ama bir güne bağlanamayan satırlar; kullanıcıya gösterilir. */
    val unmatched: List<String>,
    /** PDF'teki düz metin; bir şey okunamazsa neden okunamadığını görmek için. */
    val rawText: String,
    /** PDF'in kendi yazdığı toplamlar: gün (0 = haftalık) -> tür -> dakika. Tutarlılık kontrolü için. */
    val statedTotals: Map<Int, Map<String, Int>> = emptyMap(),
)

private val trLocale = Locale.forLanguageTag("tr")

/** Karşılaştırma için: küçük harf, Türkçe karakterler sadeleşmiş, noktalama yok. */
internal fun normalizeToken(s: String): String =
    s.lowercase(trLocale)
        .replace('ı', 'i').replace('ç', 'c').replace('ş', 's')
        .replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o')
        .replace(Regex("[^a-z0-9]"), "")

private val DAY_TOKENS: Map<String, Int> = buildMap {
    listOf("pazartesi", "pzt", "pts", "pztesi", "monday", "mon").forEach { put(it, 1) }
    listOf("sali", "sal", "tuesday", "tue", "tues").forEach { put(it, 2) }
    listOf("carsamba", "car", "crs", "crsb", "wednesday", "wed").forEach { put(it, 3) }
    listOf("persembe", "per", "prs", "prsb", "thursday", "thu", "thur", "thurs").forEach { put(it, 4) }
    listOf("cuma", "cum", "friday", "fri").forEach { put(it, 5) }
    listOf("cumartesi", "cmt", "cts", "cmrt", "saturday", "sat").forEach { put(it, 6) }
    listOf("pazar", "paz", "pzr", "sunday", "sun").forEach { put(it, 7) }
}

/** Kelime bir gün adıysa 1..7, değilse null. */
fun dayOf(token: String): Int? = DAY_TOKENS[normalizeToken(token)]

// 09:00-10:30, 9.00 – 10.30, 09:00 / 10:30 ... Tarihlerin (12.11.2026) saat sanılmaması için
// sayıdan ya da noktadan sonra/önce gelen eşleşmeler alınmaz.
private const val T = "(?<![\\d.:])([01]?\\d|2[0-4])[:.]([0-5]\\d)(?![\\d]|[.:]\\d)"
private val RANGE = Regex("$T\\s*(?:-|–|—|/|ile|to)\\s*$T")
private val SINGLE = Regex(T)

data class TimeHit(val start: Int, val end: Int?, val range: IntRange)

/** Metindeki ilk saat aralığını, yoksa ilk tek saati bulur. */
fun findTime(text: String): TimeHit? {
    RANGE.find(text)?.let { m ->
        val (h1, m1, h2, m2) = m.destructured
        val start = h1.toInt() * 60 + m1.toInt()
        var end = h2.toInt() * 60 + m2.toInt()
        if (end == 0) end = 24 * 60 // "23:15–00:00": gece yarısı
        else if (end <= start && end + 12 * 60 > start) end += 12 * 60 // "11:00-1:00" gibi öğleden sonra yazımı
        if (end <= start || end > 24 * 60) return null
        return TimeHit(start, end, m.range)
    }
    SINGLE.find(text)?.let { m ->
        val start = m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
        if (start >= 24 * 60) return null
        return TimeHit(start, null, m.range)
    }
    return null
}

private val KIND_KEYWORDS: List<Pair<ScheduleKind, List<String>>> = listOf(
    ScheduleKind.Spor to listOf("spor", "antrenman", "idman", "gym", "fitness", "kosu", "yuzme", "futbol", "basketbol", "voleybol", "tenis", "yoga", "pilates", "yuruyus", "workout"),
    ScheduleKind.Calisma to listOf("calisma", "calis", "etut", "tekrar", "odev", "soru", "deneme", "test", "kitap", "okuma", "proje", "study", "review"),
    ScheduleKind.Ders to listOf("ders", "kurs", "lab", "seminer", "matematik", "mat", "fizik", "kimya", "biyoloji", "tarih", "cografya", "edebiyat", "turkce", "ingilizce", "almanca", "lecture", "class"),
    ScheduleKind.Etkinlik to listOf("toplanti", "etkinlik", "bulusma", "randevu", "doktor", "konser", "meeting", "aile", "arkadas", "dogum"),
)

/** Başlıktaki anahtar kelimelere göre türü tahmin eder. */
fun guessKind(title: String): ScheduleKind {
    val words = title.split(Regex("\\s+")).map(::normalizeToken).filter { it.isNotEmpty() }
    for ((kind, keys) in KIND_KEYWORDS) {
        if (words.any { w -> keys.any { k -> w == k || (k.length >= 4 && w.startsWith(k)) } }) return kind
    }
    return ScheduleKind.Diger
}

private fun cleanTitle(s: String): String =
    s.replace(Regex("\\s+"), " ")
        .trim(' ', '-', '–', '—', ':', '|', '/', ',', ';', '•', '·', '*')
        .trim()

/**
 * Kelimeleri sayfa ve taban çizgisine (alt kenar) göre satırlara toplar; her satır soldan sağa sıralı.
 * Aynı satırdaki farklı boyuttaki yazıların üst kenarı farklı, taban çizgisi aynıdır.
 */
internal fun groupLines(words: List<PdfWord>): List<List<PdfWord>> {
    val result = mutableListOf<List<PdfWord>>()
    words.groupBy { it.page }.toSortedMap().forEach { (_, pageWords) ->
        val sorted = pageWords.sortedBy { it.y + it.height }
        var current = mutableListOf<PdfWord>()
        var lineY = Float.NaN
        for (w in sorted) {
            val base = w.y + w.height
            val tol = (w.height.takeIf { it > 0 } ?: 8f) * 0.4f
            if (current.isEmpty() || abs(base - lineY) <= tol) {
                if (current.isEmpty()) lineY = base
                current += w
            } else {
                result += current.sortedBy { it.x }
                current = mutableListOf(w)
                lineY = base
            }
        }
        if (current.isNotEmpty()) result += current.sortedBy { it.x }
    }
    return result
}

private fun List<PdfWord>.text() = joinToString(" ") { it.text }

/**
 * Haftalık planı ayrıştırır. Önce günlerin sütun olduğu tablo düzenini dener
 * (aynı satırda en az üç gün adı); bulamazsa gün başlıklı liste düzenine geçer.
 */
fun parsePlan(words: List<PdfWord>): ParsedPlan {
    val lines = groupLines(words.filter { it.text.isNotBlank() })
    val raw = lines.joinToString("\n") { it.text() }
    parseDayTables(lines)?.let { (entries, unmatched) ->
        if (entries.isNotEmpty()) {
            val categories = entries.map { it.category }.toSet()
            return ParsedPlan(entries, PlanLayout.DayTables, unmatched, raw, statedTotals(lines, categories))
        }
    }
    parseColumns(lines)?.let { (entries, unmatched) ->
        if (entries.isNotEmpty()) return ParsedPlan(finish(entries), PlanLayout.Columns, unmatched, raw)
    }
    val (entries, unmatched) = parseLines(lines)
    return ParsedPlan(finish(entries), if (entries.isEmpty()) PlanLayout.None else PlanLayout.Lines, unmatched, raw)
}

/** Tekrarları atar, eksik bitişleri sonraki öğeye göre tahmin eder, sıralar. */
private fun finish(entries: List<RawEntry>): List<PlanEntry> =
    entries.groupBy { it.day }.flatMap { (day, list) ->
        val sorted = list.sortedBy { it.start }
        sorted.mapIndexed { i, e ->
            val next = sorted.drop(i + 1).firstOrNull { it.start > e.start }?.start
            val end = e.end ?: minOf(next ?: (e.start + 60), e.start + 60, 24 * 60)
            val kind = guessKind(e.title)
            PlanEntry(day, e.start, end, e.title, kind, kind.label, estimatedEnd = e.end == null)
        }
    }
        .filter { it.title.isNotEmpty() && it.endMinutes > it.startMinutes }
        .distinctBy { listOf(it.dayOfWeek, it.startMinutes, it.endMinutes, normalizeToken(it.title)) }
        .sortedWith(compareBy({ it.dayOfWeek }, { it.startMinutes }))

private data class RawEntry(val day: Int, val start: Int, val end: Int?, val title: String)

// ---------------- Gün sayfaları (Haftalık Rutin biçimi) ----------------

/** PDF'teki tür etiketini uygulamadaki türe çevirir. Bilinmeyenler başlığa göre tahmin edilir. */
fun kindForCategory(category: String, title: String): ScheduleKind = when (normalizeToken(category)) {
    "ders", "kurs", "lab" -> ScheduleKind.Ders
    "tekrar", "almanca", "ingilizce", "odev", "planlama", "calisma", "etut", "proje", "okuma" -> ScheduleKind.Calisma
    "spor", "antrenman" -> ScheduleKind.Spor
    "mesai", "is", "toplanti", "etkinlik" -> ScheduleKind.Etkinlik
    "kisisel", "mola", "serbest", "yol", "oyun", "kitap", "gitar", "uyku", "yemek" -> ScheduleKind.Diger
    else -> guessKind("$category $title")
}

private val TIME_TOKEN = Regex("^[–—-]?([01]?\\d|2[0-4])[:.][0-5]\\d[–—-]?$|^[–—-]$")
private val DURATION_TOKEN = Regex("^(\\d+([,.]\\d+)?|sa|saat|dk|dakika)$", RegexOption.IGNORE_CASE)

/**
 * Her günün kendi sayfası/bölümü olduğu, "Saat | Ne yapılacak | Tür | Süre" başlıklı tablo biçimi.
 * Sütunlar başlık kelimelerinin konumundan bulunur. Satırın altındaki aynı boyutta yazılmış satır
 * başlığın devamıdır, daha küçük yazılmış satır not sayılır.
 */
private fun parseDayTables(lines: List<List<PdfWord>>): Pair<List<PlanEntry>, List<String>>? {
    fun isHeader(line: List<PdfWord>): Boolean {
        val t = line.map { normalizeToken(it.text) }
        return "saat" in t && ("tur" in t || "tip" in t || "kategori" in t)
    }
    if (lines.none(::isHeader)) return null

    val entries = mutableListOf<PlanEntry>()
    val unmatched = mutableListOf<String>()
    var day: Int? = null
    var typeX = Float.NaN   // "Tür" sütununun sol kenarı
    var durX = Float.NaN    // "Süre" sütununun sol kenarı (yoksa sağ uç)
    var inTable = false
    var current: PlanEntry? = null
    var titleHeight = 0f
    var tablePage = -1

    fun flush() {
        current?.let { entries += it }
        current = null
    }

    for (line in lines) {
        val firstDay = dayOf(line.first().text)
        // Gün başlığı: satır gün adıyla başlar, saat aralığı içermez ("Pazartesi  Almanca: 2 sa ...") ve
        // tablonun içindeki bir not değildir ("Pazartesi gecesi 8 saat uyku"): ya tablo dışındayız,
        // ya yeni sayfadayız ya da satırlardan daha büyük yazılmış.
        val looksLikeHeading = !inTable || line.first().page != tablePage || line.first().height > titleHeight * 1.1f
        if (firstDay != null && looksLikeHeading && splitByTimes(line.text()).isEmpty()) {
            flush()
            day = firstDay
            inTable = false
            continue
        }
        if (isHeader(line)) {
            flush()
            val type = line.first { normalizeToken(it.text) in setOf("tur", "tip", "kategori") }
            typeX = type.x - 15
            durX = line.firstOrNull { normalizeToken(it.text) in setOf("sure", "dakika") }?.x?.minus(20) ?: Float.MAX_VALUE
            inTable = true
            tablePage = line.first().page
            continue
        }
        if (!inTable) continue
        val d = day ?: continue

        val first = line.first()
        val hit = findTime(line.takeWhile { TIME_TOKEN.matches(it.text) }.joinToString(" ") { it.text })
        val titleWords = line.dropWhile { TIME_TOKEN.matches(it.text) }.filter { it.x < typeX }
        val typeWords = line.filter { it.x >= typeX && it.x < durX && !DURATION_TOKEN.matches(it.text) }

        if (hit != null && hit.end != null && TIME_TOKEN.matches(first.text)) {
            flush()
            tablePage = first.page
            val title = cleanTitle(titleWords.text())
            val category = cleanTitle(typeWords.text())
            val kind = kindForCategory(category, title)
            current = PlanEntry(d, hit.start, hit.end, title, kind, category.ifEmpty { kind.label })
            titleHeight = titleWords.maxOfOrNull { it.height } ?: 0f
            continue
        }
        val c = current
        if (c == null) {
            if (titleWords.isNotEmpty() && splitByTimes(line.text()).isNotEmpty()) unmatched += line.text()
            continue
        }
        // Saatsiz satır: notun kendisi saatle başlayabilir ("22:45 yatak"), o yüzden tümünü al.
        val noteWords = line.filter { it.x < typeX }
        val text = cleanTitle(noteWords.text())
        if (text.isEmpty()) continue
        val h = noteWords.maxOf { it.height }
        current = if (titleHeight > 0 && h >= titleHeight * 0.93f && typeWords.isEmpty() && c.note.isEmpty()) {
            c.copy(title = "${c.title} $text")
        } else {
            c.copy(note = listOf(c.note, text).filter { it.isNotEmpty() }.joinToString(" · "))
        }
    }
    flush()
    return entries.filter { it.title.isNotEmpty() && it.endMinutes > it.startMinutes }
        .distinctBy { listOf(it.dayOfWeek, it.startMinutes, it.endMinutes, normalizeToken(it.title)) }
        .sortedWith(compareBy({ it.dayOfWeek }, { it.startMinutes })) to unmatched
}

private val STATED = Regex("([\\p{L}]+):?\\s+(?:(\\d+)\\s*sa\\b(?:\\s+(\\d+)\\s*dk\\b)?|(\\d+)\\s*dk\\b)")

/**
 * PDF'in kendi yazdığı toplamları okur: gün başlığındaki "Almanca: 2 sa 30 dk · Ders: 4 sa 50 dk"
 * (o gün) ve tablo dışındaki "Almanca 21 sa 5 dk" satırları (haftalık, anahtar 0).
 */
private fun statedTotals(lines: List<List<PdfWord>>, categories: Set<String>): Map<Int, Map<String, Int>> {
    val known = categories.associateBy { normalizeToken(it) }
    val out = mutableMapOf<Int, MutableMap<String, Int>>()
    var seenTable = false
    for (line in lines) {
        val tokens = line.map { normalizeToken(it.text) }
        if ("saat" in tokens && "tur" in tokens) seenTable = true
        val text = line.text()
        val dayHeader = dayOf(line.first().text)?.takeIf { splitByTimes(text).isEmpty() }
        // Haftalık toplamlar yalnızca ilk tablodan önceki özet kısmında aranır.
        val key = dayHeader ?: if (!seenTable) 0 else continue
        for (m in STATED.findAll(text)) {
            val cat = known[normalizeToken(m.groupValues[1])] ?: continue
            val minutes = if (m.groupValues[4].isNotEmpty()) m.groupValues[4].toInt()
            else m.groupValues[2].toInt() * 60 + (m.groupValues[3].toIntOrNull() ?: 0)
            out.getOrPut(key) { mutableMapOf() }.putIfAbsent(cat, minutes)
        }
    }
    return out
}

// ---------------- Liste düzeni ----------------

private fun parseLines(lines: List<List<PdfWord>>): Pair<List<RawEntry>, List<String>> {
    val entries = mutableListOf<RawEntry>()
    val unmatched = mutableListOf<String>()
    var day: Int? = null
    var pending: RawEntry? = null // saati olan ama başlığı bir sonraki satırda olan öğe

    for (line in lines) {
        var tokens = line.map { it.text }
        // Satır bir gün adıyla başlıyorsa (ör. "Pazartesi", "Pzt 18.11", "SALI:") günü değiştir.
        val first = tokens.firstOrNull()?.let(::dayOf)
        if (first != null) {
            pending = null
            day = first
            tokens = tokens.drop(1)
        }
        val text = tokens.joinToString(" ")
        val timed = splitByTimes(text)
        if (timed.isEmpty()) {
            val title = cleanTitle(text)
            if (pending != null && title.isNotEmpty()) {
                entries += pending.copy(title = title)
                pending = null
            }
            continue
        }
        val d = day
        if (d == null) {
            unmatched += line.text()
            continue
        }
        // "09:00-10:00 Mat 10:15-11:00 Fizik": bir satırda birden fazla öğe olabilir.
        for ((hit, title) in timed) {
            val e = RawEntry(d, hit.start, hit.end, title)
            if (title.isEmpty()) pending = e else entries += e
        }
    }
    return entries to unmatched
}

// ---------------- Tablo düzeni ----------------

private data class Column(val day: Int, val left: Float, val right: Float)

private fun headerColumns(line: List<PdfWord>): List<Pair<Int, Float>>? {
    val days = line.mapNotNull { w -> dayOf(w.text)?.let { it to w.centerX } }.distinctBy { it.first }
    return days.takeIf { it.size >= 3 }?.sortedBy { it.second }
}

private fun parseColumns(lines: List<List<PdfWord>>): Pair<List<RawEntry>, List<String>>? {
    val headerIndex = lines.indexOfFirst { headerColumns(it) != null }
    if (headerIndex < 0) return null
    val entries = mutableListOf<RawEntry>()
    val unmatched = mutableListOf<String>()

    // Her sayfanın kendi başlık satırı olabilir; yoksa bir öncekinin sütunları kullanılır.
    var columns: List<Column> = emptyList()
    var headerY = 0f
    val byPage = mutableMapOf<Int, MutableList<PdfWord>>()
    val headerOfPage = mutableMapOf<Int, Pair<List<Column>, Float>>()
    for ((i, line) in lines.withIndex()) {
        if (i < headerIndex && line.first().page == lines[headerIndex].first().page) continue
        val header = headerColumns(line)
        if (header != null) {
            columns = header.mapIndexed { j, (day, cx) ->
                val l = if (j == 0) cx - (header[1].second - cx) / 2 else (header[j - 1].second + cx) / 2
                val r = if (j == header.lastIndex) cx + (cx - header[j - 1].second) / 2 else (cx + header[j + 1].second) / 2
                Column(day, l, r)
            }
            headerY = line.maxOf { it.y }
            headerOfPage[line.first().page] = columns to headerY
            continue
        }
        if (columns.isEmpty()) continue
        val page = line.first().page
        if (page !in headerOfPage) headerOfPage[page] = columns to -1f
        val (_, hy) = headerOfPage.getValue(page)
        line.filter { it.y > hy }.forEach { byPage.getOrPut(page) { mutableListOf() } += it }
    }

    for ((page, words) in byPage) {
        val cols = headerOfPage.getValue(page).first
        val firstLeft = cols.first().left
        // Sol kenardaki saat sütunu (satır başlıkları).
        val labels = groupLines(words.filter { it.centerX < firstLeft })
            .mapNotNull { l -> findTime(l.text())?.let { Triple(l.minOf { it.y }, l.maxOf { it.y + it.height }, it) } }
            .sortedBy { it.first }

        for (col in cols) {
            val cellWords = words.filter { it.centerX >= col.left && it.centerX < col.right }
            for (block in blocks(groupLines(cellWords))) {
                val text = block.joinToString(" ") { it.text() }
                val top = block.first().minOf { it.y }
                val bottom = block.last().maxOf { it.y + it.height }
                val timed = splitByTimes(text)
                if (timed.isNotEmpty()) {
                    timed.forEach { (hit, title) -> if (title.isNotEmpty()) entries += RawEntry(col.day, hit.start, hit.end, title) }
                    continue
                }
                // Hücrede saat yok: satır başlığından al.
                val title = cleanTitle(text)
                if (title.isEmpty()) continue
                val startLabel = labels.lastOrNull { it.first <= top + 2 } ?: labels.firstOrNull { it.first in top..bottom }
                if (startLabel == null) {
                    unmatched += title
                    continue
                }
                val start = startLabel.third.start
                val endLabel = labels.lastOrNull { it.first < bottom - 1 } ?: startLabel
                val end = endLabel.third.end
                    ?: labels.firstOrNull { it.first > endLabel.first }?.third?.start
                    ?: startLabel.third.end
                entries += RawEntry(col.day, start, end?.takeIf { it > start }, title)
            }
        }
    }
    return entries to unmatched
}

/** Bir sütundaki satırları, aralarında belirgin boşluk olmayan bloklara ayırır (bir hücre = bir blok). */
private fun blocks(lines: List<List<PdfWord>>): List<List<List<PdfWord>>> {
    val out = mutableListOf<MutableList<List<PdfWord>>>()
    var lastBottom = Float.NaN
    for (l in lines) {
        val top = l.minOf { it.y }
        val h = l.maxOf { it.height }.takeIf { it > 0 } ?: 8f
        if (out.isEmpty() || top - lastBottom > h * 0.9f) out += mutableListOf(l) else out.last() += l
        lastBottom = l.maxOf { it.y + it.height }
    }
    return out
}

/**
 * Metni saatlere göre böler. Tek saat varsa başlık saatin sonrasındaki (yoksa öncesindeki) metindir;
 * birden fazla saat varsa her saatin başlığı, bir sonraki saate kadar olan metindir.
 */
internal fun splitByTimes(text: String): List<Pair<TimeHit, String>> {
    val hits = mutableListOf<TimeHit>()
    var offset = 0
    while (offset < text.length) {
        val h = findTime(text.substring(offset)) ?: break
        val shifted = h.copy(range = (h.range.first + offset)..(h.range.last + offset))
        hits += shifted
        offset = shifted.range.last + 1
    }
    if (hits.isEmpty()) return emptyList()
    if (hits.size == 1) {
        val h = hits[0]
        val after = cleanTitle(text.substring(h.range.last + 1))
        return listOf(h to after.ifEmpty { cleanTitle(text.substring(0, h.range.first)) })
    }
    val leading = cleanTitle(text.substring(0, hits[0].range.first))
    val afters = hits.mapIndexed { i, h ->
        cleanTitle(text.substring(h.range.last + 1, hits.getOrNull(i + 1)?.range?.first ?: text.length))
    }
    // "Mat 09:00-10:00 Fizik 10:15-11:00" biçimi: başlık önde, son parça boş.
    if (leading.isNotEmpty() && afters.last().isEmpty()) {
        val befores = listOf(leading) + afters.dropLast(1)
        return hits.zip(befores)
    }
    return hits.zip(afters)
}
