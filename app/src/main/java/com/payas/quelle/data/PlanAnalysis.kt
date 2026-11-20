package com.payas.quelle.data

enum class Severity(val label: String) { Uyari("Uyarı"), Oneri("Öneri"), Bilgi("Bilgi") }

data class Finding(val severity: Severity, val text: String)

data class FreeSlot(val dayOfWeek: Int, val start: Int, val end: Int)

data class PlanReport(
    /** Pazartesi..Pazar, planlanmış toplam dakika (çakışan saatler bir kez sayılır). */
    val minutesByDay: List<Int>,
    /** Pazartesi..Pazar, yük sayılan dakika (ders, çalışma, iş, yol). */
    val loadByDay: List<Int>,
    /** PDF'teki tür etiketine göre haftalık dakika, büyükten küçüğe. */
    val minutesByCategory: Map<String, Int>,
    val totalMinutes: Int,
    val findings: List<Finding>,
    val freeSlots: List<FreeSlot>,
    /** Gece n -> n+1 arasındaki boşluk (son öğenin bitişinden ertesi günün ilk öğesine), dakika. */
    val nightGaps: List<Int?>,
)

val WEEKDAY_NAMES = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

/** Analizde kullanılan eşikler; tek yerde dursun ki ayarlamak kolay olsun. */
object PlanRules {
    const val HEAVY_DAY = 10 * 60         // günde 10 saatten fazla yük ağır
    const val NO_BREAK = 3 * 60           // 3 saatten uzun molasız yük bloğu
    const val BREAK_GAP = 10              // 10 dakikadan kısa ara mola sayılmaz
    const val MIN_NIGHT = 7 * 60          // son öğe ile ertesi sabahın ilk öğesi arası en az 7 saat
    const val LIGHT_DAY = 3 * 60          // 3 saatten az yük = hafif gün
    const val DAY_START = 8 * 60          // boşluk ararken gün 08:00–22:00
    const val DAY_END = 22 * 60
    const val MIN_FREE = 90               // en az 1,5 saatlik boşluklar listelenir
    const val TOTAL_TOLERANCE = 5         // PDF toplamı ile hesap arasında kabul edilen fark (dk)
}

/** Dinlenme değil, iş yükü sayılan öğe mi? */
fun PlanEntry.isLoad(): Boolean =
    kind == ScheduleKind.Ders || kind == ScheduleKind.Calisma ||
        normalizeToken(category) in setOf("mesai", "is", "yol", "toplanti")

fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "$m dk"
        m == 0 -> "$h sa"
        else -> "$h sa $m dk"
    }
}

/** Aynı gündeki aralıkları birleştirir (gap dakikadan kısa aralar da birleşir). */
private fun merge(intervals: List<Pair<Int, Int>>, gap: Int = 0): List<Pair<Int, Int>> {
    val out = mutableListOf<Pair<Int, Int>>()
    for ((s, e) in intervals.sortedBy { it.first }) {
        val last = out.lastOrNull()
        if (last != null && s <= last.second + gap) out[out.lastIndex] = last.first to maxOf(last.second, e)
        else out += s to e
    }
    return out
}

private fun span(list: List<PlanEntry>) = merge(list.map { it.startMinutes to it.endMinutes }).sumOf { it.second - it.first }

fun analyzePlan(entries: List<PlanEntry>, stated: Map<Int, Map<String, Int>> = emptyMap()): PlanReport {
    val findings = mutableListOf<Finding>()
    val byDay = (1..7).associateWith { d -> entries.filter { it.dayOfWeek == d }.sortedBy { it.startMinutes } }
    val minutesByDay = (1..7).map { span(byDay.getValue(it)) }
    val loadByDay = (1..7).map { d -> span(byDay.getValue(d).filter { it.isLoad() }) }
    val byCategory = entries.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.duration } }
        .toList().sortedByDescending { it.second }.toMap()
    val nightGaps = (1..7).map { d ->
        val last = byDay.getValue(d).maxOfOrNull { it.endMinutes } ?: return@map null
        val next = byDay.getValue(d % 7 + 1).minOfOrNull { it.startMinutes } ?: return@map null
        24 * 60 - last + next
    }

    if (entries.isEmpty()) {
        return PlanReport(minutesByDay, loadByDay, byCategory, 0, listOf(Finding(Severity.Uyari, "Planda okunabilen öğe yok.")), emptyList(), nightGaps)
    }

    // Çakışmalar
    for ((d, list) in byDay) {
        for (i in list.indices) for (j in i + 1 until list.size) {
            val a = list[i]
            val b = list[j]
            if (b.startMinutes < a.endMinutes && a.startMinutes < b.endMinutes) {
                findings += Finding(
                    Severity.Uyari,
                    "${WEEKDAY_NAMES[d - 1]}: \"${a.title}\" (${formatMinutes(a.startMinutes)}–${formatMinutes(a.endMinutes)}) ile " +
                        "\"${b.title}\" (${formatMinutes(b.startMinutes)}–${formatMinutes(b.endMinutes)}) çakışıyor.",
                )
            }
        }
    }

    // PDF'in kendi toplamlarıyla tutarlılık
    for ((key, totals) in stated.toSortedMap()) {
        for ((cat, said) in totals) {
            val source = if (key == 0) entries else byDay.getValue(key)
            val actual = source.filter { normalizeToken(it.category) == normalizeToken(cat) }.sumOf { it.duration }
            if (kotlin.math.abs(actual - said) > PlanRules.TOTAL_TOLERANCE) {
                val where = if (key == 0) "Haftalık özet" else WEEKDAY_NAMES[key - 1]
                findings += Finding(Severity.Uyari, "$where: PDF'te $cat ${formatDuration(said)} yazıyor, satırların toplamı ${formatDuration(actual)}.")
            }
        }
    }

    // Ağır günler, molasız bloklar, uyku penceresi
    for (d in 1..7) {
        val name = WEEKDAY_NAMES[d - 1]
        val load = loadByDay[d - 1]
        if (load > PlanRules.HEAVY_DAY) findings += Finding(Severity.Uyari, "$name ${formatDuration(load)} yük (ders, çalışma, iş, yol). 10 saatin üstü birkaç hafta sürdürülemez; aksayan ilk gün bu olur.")
        merge(byDay.getValue(d).filter { it.isLoad() }.map { it.startMinutes to it.endMinutes }, PlanRules.BREAK_GAP - 1)
            .filter { it.second - it.first > PlanRules.NO_BREAK }
            .forEach { (s, e) ->
                findings += Finding(Severity.Oneri, "$name ${formatMinutes(s)}–${formatMinutes(e)} arası ${formatDuration(e - s)} arasız yük. Dışarıdan belirlenmiş değilse (ders, mesai) araya 10–15 dakika koy.")
            }
        nightGaps[d - 1]?.takeIf { it < PlanRules.MIN_NIGHT }?.let {
            findings += Finding(Severity.Uyari, "$name gecesi son öğe ile ertesi sabahın ilk öğesi arasında ${formatDuration(it)} var. Uykuya dalma süresiyle 7 saatin altına düşer.")
        }
    }

    // Dağılım dengesi
    val busy = (1..7).filter { loadByDay[it - 1] > 0 }
    if (busy.size >= 2) {
        val max = busy.maxBy { loadByDay[it - 1] }
        val min = busy.minBy { loadByDay[it - 1] }
        val hi = loadByDay[max - 1]
        val lo = loadByDay[min - 1]
        if (hi >= 2 * lo && hi - lo >= 4 * 60) {
            findings += Finding(Severity.Bilgi, "Yük dengesiz: ${WEEKDAY_NAMES[max - 1]} ${formatDuration(hi)}, ${WEEKDAY_NAMES[min - 1]} ${formatDuration(lo)}.")
        }
    }
    val light = (1..7).filter { loadByDay[it - 1] < PlanRules.LIGHT_DAY }
    if (light.isEmpty()) findings += Finding(Severity.Oneri, "Haftada yükü 3 saatin altında olan hiç gün yok. Toparlanma günü olmayan plan genellikle 2–3 hafta içinde kayar.")

    // Ders / bireysel çalışma oranı
    val lesson = entries.filter { it.kind == ScheduleKind.Ders }.sumOf { it.duration }
    val review = entries.filter { normalizeToken(it.category) in setOf("tekrar", "odev", "calisma", "etut") }.sumOf { it.duration }
    if (lesson >= 5 * 60) {
        val ratio = review.toDouble() / lesson
        val text = "Ders ${formatDuration(lesson)}, ders tekrarı/ödev ${formatDuration(review)} (oran ${"%.2f".format(ratio)})."
        findings += if (ratio < 0.5) Finding(Severity.Oneri, "$text Ders başına yarım saatten az tekrar düşüyor; sınav dönemi yükü sona yığılır.")
        else Finding(Severity.Bilgi, text)
    }
    if (entries.none { it.kind == ScheduleKind.Spor }) findings += Finding(Severity.Bilgi, "Planda spor/hareket yok.")

    val estimated = entries.count { it.estimatedEnd }
    if (estimated > 0) findings += Finding(Severity.Bilgi, "$estimated öğenin bitiş saati PDF'te yok; sonraki öğeye ya da 1 saate göre tahmin edildi.")

    // Planlanmamış boşluklar
    val free = (1..7).flatMap { d ->
        val busyIntervals = merge(byDay.getValue(d).map { it.startMinutes to it.endMinutes })
        if (busyIntervals.isEmpty()) return@flatMap emptyList()
        var cursor = PlanRules.DAY_START
        val slots = mutableListOf<FreeSlot>()
        for ((s, e) in busyIntervals + (PlanRules.DAY_END to PlanRules.DAY_END)) {
            val gapEnd = minOf(s, PlanRules.DAY_END)
            if (gapEnd - cursor >= PlanRules.MIN_FREE) slots += FreeSlot(d, cursor, gapEnd)
            cursor = maxOf(cursor, e)
            if (cursor >= PlanRules.DAY_END) break
        }
        slots
    }

    val order = listOf(Severity.Uyari, Severity.Oneri, Severity.Bilgi)
    return PlanReport(minutesByDay, loadByDay, byCategory, minutesByDay.sum(), findings.sortedBy { order.indexOf(it.severity) }, free, nightGaps)
}

/** Plan öğesini programa eklenecek forma çevirir. */
fun PlanEntry.toScheduleItem(id: String) = ScheduleItem(
    id = id,
    title = title,
    dayOfWeek = dayOfWeek,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    kind = kind,
    source = ScheduleItem.SOURCE_PDF,
)

/**
 * PDF'ten gelen haftalık planı programa yerleştirir: önceki PDF'ten gelen öğeler silinir,
 * elle eklenenler korunur. Elle eklenmiş aynı öğe (gün, saat, başlık) tekrar eklenmez.
 */
fun AppData.replacePdfSchedule(entries: List<PlanEntry>, newId: () -> String): AppData {
    val manual = schedule.filter { it.source != ScheduleItem.SOURCE_PDF }
    fun key(day: Int, s: Int, e: Int, t: String) = listOf(day, s, e, normalizeToken(t))
    val manualKeys = manual.map { key(it.dayOfWeek, it.startMinutes, it.endMinutes, it.title) }.toSet()
    val imported = entries
        .filter { key(it.dayOfWeek, it.startMinutes, it.endMinutes, it.title) !in manualKeys }
        .map { it.toScheduleItem(newId()) }
    return copy(schedule = manual + imported)
}
