package com.payas.quelle

import com.payas.quelle.data.AppData
import com.payas.quelle.data.Habit
import com.payas.quelle.data.Mood
import com.payas.quelle.data.TimerMode
import com.payas.quelle.data.completeTimerIfDone
import com.payas.quelle.data.habitCount
import com.payas.quelle.data.moodFor
import com.payas.quelle.data.pauseTimer
import com.payas.quelle.data.resumeTimer
import com.payas.quelle.data.setHabitCount
import com.payas.quelle.data.setMood
import com.payas.quelle.data.startTimer
import com.payas.quelle.data.streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WellbeingTest {
    private val water = Habit("su", "8 bardak su", target = 8)
    private val today = LocalDate.of(2026, 11, 4)

    @Test
    fun habitCountCanBeSetAndCleared() {
        var d = AppData(habits = listOf(water)).setHabitCount("su", "2026-11-04", 3)
        assertEquals(3, d.habitCount("su", "2026-11-04"))
        d = d.setHabitCount("su", "2026-11-04", 5)
        assertEquals(5, d.habitCount("su", "2026-11-04"))
        assertEquals(1, d.habitEntries.size)
        d = d.setHabitCount("su", "2026-11-04", 0)
        assertEquals(0, d.habitCount("su", "2026-11-04"))
        assertTrue(d.habitEntries.isEmpty())
    }

    @Test
    fun streakCountsConsecutiveDaysThatHitTarget() {
        var d = AppData(habits = listOf(water))
        d = d.setHabitCount("su", "2026-11-01", 8)
        d = d.setHabitCount("su", "2026-11-02", 8)
        d = d.setHabitCount("su", "2026-11-03", 8)
        // Bugün henüz 3 bardak: seri dünden geriye sayılır.
        d = d.setHabitCount("su", "2026-11-04", 3)
        assertEquals(3, d.streak(water, today))
        d = d.setHabitCount("su", "2026-11-04", 8)
        assertEquals(4, d.streak(water, today))
    }

    @Test
    fun streakBreaksOnMissedDay() {
        var d = AppData(habits = listOf(water))
        d = d.setHabitCount("su", "2026-11-01", 8)
        d = d.setHabitCount("su", "2026-11-03", 8)
        assertEquals(1, d.streak(water, today))
    }

    @Test
    fun moodIsReplacedNotDuplicated() {
        var d = AppData().setMood("2026-11-04", Mood.Iyi)
        d = d.setMood("2026-11-04", Mood.Harika)
        assertEquals(Mood.Harika, d.moodFor("2026-11-04"))
        assertEquals(1, d.moods.size)
        assertNull(d.setMood("2026-11-04", null).moodFor("2026-11-04"))
    }

    @Test
    fun focusTimerRecordsSessionWhenDone() {
        val start = 1_000_000L
        var d = AppData().startTimer(TimerMode.Odak, "Matematik", start)
        // Henüz bitmedi.
        assertEquals(d, d.completeTimerIfDone(start + 60_000, "2026-11-04"))
        d = d.completeTimerIfDone(start + 25 * 60_000, "2026-11-04")
        assertNull(d.timer)
        assertEquals(1, d.focusSessions.size)
        assertEquals("Matematik", d.focusSessions[0].label)
    }

    @Test
    fun breaksAreNotRecordedAsFocus() {
        val d = AppData().startTimer(TimerMode.KisaMola, "", 0).completeTimerIfDone(5 * 60_000, "2026-11-04")
        assertNull(d.timer)
        assertTrue(d.focusSessions.isEmpty())
    }

    @Test
    fun pauseKeepsRemainingTime() {
        var d = AppData().startTimer(TimerMode.Odak, "", 0)
        d = d.pauseTimer(10 * 60_000)                 // 10 dk sonra duraklat: 15 dk kaldı
        assertEquals(15 * 60_000L, d.timer!!.remaining(99 * 60_000))
        assertEquals(d, d.completeTimerIfDone(99 * 60_000, "2026-11-04"))
        d = d.resumeTimer(100 * 60_000)               // devam: 115. dakikada biter
        assertEquals(115 * 60_000L, d.timer!!.endAt)
    }
}
