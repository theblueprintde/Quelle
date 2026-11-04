package com.payas.quelle

import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.reminders.nextReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ReminderTest {
    // 4 Kasım 2026 bir çarşamba (3).
    private val wednesday9 = LocalDateTime.of(2026, 11, 4, 9, 0)
    private val items = listOf(
        ScheduleItem("mat", "Matematik", 3, 510, 590),   // Çar 08:30
        ScheduleItem("fiz", "Fizik", 3, 600, 680),       // Çar 10:00
        ScheduleItem("tar", "Tarih", 2, 510, 590),       // Sal 08:30
    )

    @Test
    fun picksNextLessonToday() {
        val r = nextReminder(items, wednesday9)!!
        assertEquals("fiz", r.item.id)
        assertEquals(LocalDateTime.of(2026, 11, 4, 9, 45), r.triggerAt)
    }

    @Test
    fun skipsLessonWhoseReminderTimePassed() {
        val r = nextReminder(items, LocalDateTime.of(2026, 11, 4, 9, 50))!!
        // Fizik'in hatırlatması 09:45'te geçti; sıradaki salı Tarih.
        assertEquals("tar", r.item.id)
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 15), r.triggerAt)
    }

    @Test
    fun wrapsToSameDayNextWeek() {
        val only = listOf(ScheduleItem("mat", "Matematik", 3, 510, 590))
        val r = nextReminder(only, wednesday9)!!
        assertEquals(LocalDateTime.of(2026, 11, 11, 8, 15), r.triggerAt)
    }

    @Test
    fun noScheduleMeansNoReminder() {
        assertNull(nextReminder(emptyList(), wednesday9))
    }
}
