package com.payas.quelle

import com.payas.quelle.data.AppData
import com.payas.quelle.data.PdfWord
import com.payas.quelle.data.PlanEntry
import com.payas.quelle.data.PlanLayout
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.ScheduleKind
import com.payas.quelle.data.Severity
import com.payas.quelle.data.analyzePlan
import com.payas.quelle.data.dayOf
import com.payas.quelle.data.findTime
import com.payas.quelle.data.parsePlan
import com.payas.quelle.data.replacePdfSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanImportTest {
    /** Bir satırı kelimelere böler; her parça (metin, x) olarak verilir, kelimeler 6 pt aralıklı dizilir. */
    private class Page(val page: Int) {
        val words = mutableListOf<PdfWord>()
        fun line(y: Float, vararg cells: Pair<String, Float>, height: Float = 15f) {
            for ((text, x0) in cells) {
                var x = x0
                text.split(" ").filter { it.isNotEmpty() }.forEach { w ->
                    val width = w.length * 6f
                    words += PdfWord(page, x, y, width, height, w)
                    x += width + 3f
                }
            }
        }
    }

    /** "Saat | Ne yapılacak | Tür | Süre" biçiminde bir gün sayfası. */
    private fun dayPage(page: Int, day: String, summary: String, rows: List<List<String>>, notes: Map<Int, String> = emptyMap()): List<PdfWord> {
        val p = Page(page)
        p.line(32f, day to 36f, summary to 318f, height = 18f)
        p.line(59f, "Saat" to 41f, "Ne yapılacak" to 113f, "Tür" to 420f, "Süre" to 537f, height = 11f)
        var y = 81f
        rows.forEachIndexed { i, (start, end, title, type, dur) ->
            p.line(y, start to 41f, "–$end" to 74f, title to 122f, type to 425f, dur to 520f)
            y += 15f
            notes[i]?.let { p.line(y, it to 122f, height = 12.9f); y += 14f }
            y += 14f
        }
        return p.words
    }

    @Test
    fun findsTimeRangesAndIgnoresDates() {
        assertEquals(9 * 60, findTime("09:00 – 10:30 Mat")!!.start)
        assertEquals(10 * 60 + 30, findTime("9.00-10.30")!!.end)
        assertEquals(24 * 60, findTime("23:15 –00:00")!!.end)
        assertNull(findTime("Pazartesi 18.11.2026"))
        assertEquals(1, dayOf("PAZARTESİ"))
        assertEquals(3, dayOf("Çarşamba:"))
        assertEquals(6, dayOf("Cmt"))
        assertNull(dayOf("Saat"))
    }

    @Test
    fun parsesDayTablePagesWithNotesAndWrappedTitles() {
        val summary = Page(1).apply {
            line(40f, "Haftalık toplamlar" to 36f)
            line(70f, "Almanca 2 sa 30 dk" to 36f)
            line(90f, "Ders 1 sa" to 36f)
        }.words
        val monday = dayPage(
            2, "Pazartesi", "Almanca: 1 sa 30 dk · Ders: 1 sa",
            listOf(
                listOf("07:30", "09:00", "Almanca: ağır blok (gramer)", "Almanca", "1 sa 30 dk"),
                listOf("09:00", "09:15", "Mola", "Mola", "15 dk"),
                listOf("10:00", "11:00", "Ders: Calculus 1", "Ders", "1 sa"),
            ),
            notes = mapOf(0 to "Pazartesi gecesi erken yat", 1 to "22:45 telefon yok"),
        )
        val tuesday = dayPage(
            3, "Salı", "Almanca: 1 sa",
            listOf(
                listOf("13:30", "23:00", "Mesai (dükkan)", "Mesai", "9 sa 30 dk"),
                listOf("23:15", "00:00", "Duş, hafif yemek", "Kişisel", "45 dk"),
            ),
        )
        val plan = parsePlan(summary + monday + tuesday)
        assertEquals(PlanLayout.DayTables, plan.layout)
        assertEquals(5, plan.entries.size)

        val first = plan.entries[0]
        assertEquals(1, first.dayOfWeek)
        assertEquals("Almanca: ağır blok (gramer)", first.title)
        assertEquals("Almanca", first.category)
        assertEquals(ScheduleKind.Calisma, first.kind)
        // Notun "Pazartesi" ile başlaması yeni gün başlığı sanılmamalı.
        assertEquals("Pazartesi gecesi erken yat", first.note)
        assertEquals("22:45 telefon yok", plan.entries[1].note)
        assertEquals(ScheduleKind.Ders, plan.entries[2].kind)

        val late = plan.entries.last()
        assertEquals(2, late.dayOfWeek)
        assertEquals(24 * 60, late.endMinutes)
        assertEquals(ScheduleKind.Etkinlik, plan.entries[3].kind)

        assertEquals(mapOf("Almanca" to 150, "Ders" to 60), plan.statedTotals[0])
        assertEquals(mapOf("Almanca" to 90, "Ders" to 60), plan.statedTotals[1])

        // Salı başlığı 1 sa Almanca diyor ama satırlarda yok: tutarsızlık raporlanmalı.
        val report = analyzePlan(plan.entries, plan.statedTotals)
        assertTrue(report.findings.any { it.severity == Severity.Uyari && it.text.startsWith("Salı: PDF'te Almanca") })
        assertTrue(report.findings.any { it.text.startsWith("Haftalık özet: PDF'te Almanca 2 sa 30 dk") })
        assertTrue(report.findings.none { it.text.startsWith("Pazartesi: PDF'te") })
    }

    @Test
    fun parsesListLayoutUnderDayHeadings() {
        val p = Page(1)
        p.line(10f, "Pazartesi 18.11.2026" to 20f)
        p.line(30f, "09:00 - 10:30 Matematik dersi" to 20f)
        p.line(50f, "11:00-12:00 Etüt 13:00-14:00 Spor salonu" to 20f)
        p.line(70f, "Salı" to 20f)
        p.line(90f, "18:00 Futbol" to 20f)
        p.line(110f, "19:30 Kitap okuma" to 20f)
        val plan = parsePlan(p.words)
        assertEquals(PlanLayout.Lines, plan.layout)
        assertEquals(listOf("Matematik dersi", "Etüt", "Spor salonu", "Futbol", "Kitap okuma"), plan.entries.map { it.title })
        assertEquals(ScheduleKind.Ders, plan.entries[0].kind)
        assertEquals(ScheduleKind.Spor, plan.entries[3].kind)
        // Bitişi yazmayan öğe sonraki öğeye kadar sürer, en çok 1 saat.
        assertEquals(19 * 60, plan.entries[3].endMinutes)
        assertTrue(plan.entries[3].estimatedEnd)
    }

    @Test
    fun parsesGridWithDaysAsColumns() {
        val p = Page(1)
        p.line(10f, "Saat" to 10f, "Pazartesi" to 100f, "Salı" to 200f, "Çarşamba" to 300f)
        p.line(30f, "09:00-10:00" to 10f, "Fizik" to 100f, "Kimya" to 300f)
        p.line(50f, "10:00-11:00" to 10f, "Tarih" to 200f)
        val plan = parsePlan(p.words)
        assertEquals(PlanLayout.Columns, plan.layout)
        val got = plan.entries.map { Triple(it.dayOfWeek, it.startMinutes, it.title) }
        assertEquals(listOf(Triple(1, 540, "Fizik"), Triple(2, 600, "Tarih"), Triple(3, 540, "Kimya")), got)
    }

    private fun e(day: Int, s: Int, end: Int, title: String, cat: String, kind: ScheduleKind) =
        PlanEntry(day, s, end, title, kind, cat)

    @Test
    fun analysisFlagsConflictsShortNightsAndLongBlocks() {
        val entries = listOf(
            e(1, 9 * 60, 13 * 60 + 30, "Ders", "Ders", ScheduleKind.Ders),
            e(1, 13 * 60, 14 * 60, "Toplantı", "Mesai", ScheduleKind.Etkinlik),
            e(1, 22 * 60, 24 * 60, "Ödev", "Ödev", ScheduleKind.Calisma),
            e(2, 5 * 60, 6 * 60, "Koşu", "Spor", ScheduleKind.Spor),
        )
        val r = analyzePlan(entries)
        assertTrue(r.findings.any { it.text.contains("çakışıyor") })
        assertTrue(r.findings.any { it.text.startsWith("Pazartesi gecesi") && it.text.contains("5 sa") })
        assertTrue(r.findings.any { it.text.contains("09:00–14:00 arası 5 sa arasız") })
        assertEquals(5 * 60 + 2 * 60, r.loadByDay[0])
    }

    @Test
    fun weeklyImportReplacesPreviousPdfButKeepsManualItems() {
        val manual = ScheduleItem("m", "Fizik", 1, 600, 680)
        val oldPdf = ScheduleItem("p", "Eski blok", 2, 600, 660, source = ScheduleItem.SOURCE_PDF)
        val data = AppData(schedule = listOf(manual, oldPdf))
        var n = 0
        val next = data.replacePdfSchedule(
            listOf(
                e(1, 600, 680, "fizik", "Ders", ScheduleKind.Ders), // elle eklenenin aynısı, tekrar eklenmez
                e(3, 540, 600, "Yeni blok", "Tekrar", ScheduleKind.Calisma),
            ),
        ) { "id${n++}" }
        assertEquals(listOf("m", "id0"), next.schedule.map { it.id })
        assertEquals(ScheduleItem.SOURCE_PDF, next.schedule[1].source)
        assertNull(next.schedule[0].source)
    }
}
