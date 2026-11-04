package com.payas.quelle

import com.payas.quelle.data.AppData
import com.payas.quelle.data.Assignment
import com.payas.quelle.data.AssignmentKind
import com.payas.quelle.data.DailyGoal
import com.payas.quelle.data.FocusSession
import com.payas.quelle.data.SONGS
import com.payas.quelle.data.Task
import com.payas.quelle.data.WeeklyReview
import com.payas.quelle.data.daysUntil
import com.payas.quelle.data.goalFor
import com.payas.quelle.data.mondayOf
import com.payas.quelle.data.reviewFor
import com.payas.quelle.data.saveReview
import com.payas.quelle.data.setGoal
import com.payas.quelle.data.songFor
import com.payas.quelle.data.sortedAssignments
import com.payas.quelle.data.weekStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StudyTest {
    private val today = LocalDate.of(2026, 11, 4) // çarşamba

    @Test
    fun countsDaysUntilDue() {
        assertEquals(2, daysUntil("2026-11-06", today))
        assertEquals(0, daysUntil("2026-11-04", today))
        assertEquals(-1, daysUntil("2026-11-03", today))
    }

    @Test
    fun mondayOfWeek() {
        assertEquals(LocalDate.of(2026, 11, 2), mondayOf(today))
        assertEquals(LocalDate.of(2026, 11, 2), mondayOf(LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun openAssignmentsComeFirstByDate() {
        val data = AppData(
            assignments = listOf(
                Assignment("a", "Bitti", AssignmentKind.Sinav, "2026-11-01", done = true),
                Assignment("b", "Fizik", AssignmentKind.Sinav, "2026-11-20"),
                Assignment("c", "Mat", AssignmentKind.Sinav, "2026-11-06"),
                Assignment("d", "Ödev", AssignmentKind.Odev, "2026-11-05"),
            )
        )
        assertEquals(listOf("Mat", "Fizik", "Bitti"), data.sortedAssignments(AssignmentKind.Sinav).map { it.title })
    }

    @Test
    fun goalIsSetReplacedAndCleared() {
        var d = AppData().setGoal(DailyGoal("2026-11-04", "2 konu bitir"), "2026-11-04")
        d = d.setGoal(DailyGoal("2026-11-04", "3 konu bitir", done = true), "2026-11-04")
        assertEquals("3 konu bitir", d.goalFor("2026-11-04")?.text)
        assertEquals(1, d.dailyGoals.size)
        assertNull(d.setGoal(null, "2026-11-04").goalFor("2026-11-04"))
    }

    @Test
    fun reviewIsSavedPerWeek() {
        var d = AppData().saveReview(WeeklyReview("2026-11-02", achieved = "Ödev bitti"))
        d = d.saveReview(WeeklyReview("2026-11-02", achieved = "Ödev ve kitap"))
        assertEquals("Ödev ve kitap", d.reviewFor("2026-11-02").achieved)
        assertEquals("", d.reviewFor("2026-10-26").achieved)
        assertEquals(1, d.weeklyReviews.size)
    }

    @Test
    fun weekStatsCountOnlyThisWeek() {
        val d = AppData(
            tasks = listOf(
                Task("1", "a", "2026-11-02", done = true),
                Task("2", "b", "2026-11-04", done = true),
                Task("3", "c", "2026-11-04"),
                Task("4", "eski", "2026-10-30", done = true),
            ),
            focusSessions = listOf(FocusSession("2026-11-03", 25), FocusSession("2026-10-29", 25)),
        )
        val s = d.weekStats(mondayOf(today), today)
        assertEquals(2, s.tasksDone)
        assertEquals(3, s.tasksTotal)
        assertEquals(1, s.focusSessions)
        assertEquals(listOf(1, 0, 1, 0, 0, 0, 0), s.doneByDay)
    }

    @Test
    fun songLinksToYoutubeMusicSearch() {
        val url = SONGS[0].youtubeMusicUrl
        assertTrue(url.startsWith("https://music.youtube.com/search?q="))
        assertTrue(url.contains("Here+Comes+the+Sun"))
        assertEquals(songFor(0), songFor(SONGS.size))
    }
}
