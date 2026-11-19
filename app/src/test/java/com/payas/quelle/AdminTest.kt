package com.payas.quelle

import com.payas.quelle.data.AppData
import com.payas.quelle.data.Cleanup
import com.payas.quelle.data.FocusSession
import com.payas.quelle.data.QUOTES
import com.payas.quelle.data.Quote
import com.payas.quelle.data.SONGS
import com.payas.quelle.data.Song
import com.payas.quelle.data.Task
import com.payas.quelle.data.addQuote
import com.payas.quelle.data.addSong
import com.payas.quelle.data.applyCleanup
import com.payas.quelle.data.cleanupCount
import com.payas.quelle.data.hashPin
import com.payas.quelle.data.isValidPin
import com.payas.quelle.data.quoteFor
import com.payas.quelle.data.removeQuote
import com.payas.quelle.data.songFor
import com.payas.quelle.data.totalRecords
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdminTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val data = AppData(
        tasks = listOf(
            Task("1", "Eski ve bitmiş", "2026-09-01", done = true),
            Task("2", "Eski", "2026-10-01"),
            Task("3", "Bugün bitti", "2026-11-19", done = true),
            Task("4", "Bugün", "2026-11-19"),
        ),
        focusSessions = listOf(FocusSession("2026-11-18", 25)),
    )

    @Test
    fun doneTasksCleanupKeepsOpenOnes() {
        val cleaned = data.applyCleanup(Cleanup.DoneTasks, today)
        assertEquals(listOf("2", "4"), cleaned.tasks.map { it.id })
        assertEquals(2, data.cleanupCount(Cleanup.DoneTasks, today))
    }

    @Test
    fun oldTasksCleanupUsesThirtyDays() {
        val cleaned = data.applyCleanup(Cleanup.OldTasks, today)
        assertEquals(listOf("3", "4"), cleaned.tasks.map { it.id })
    }

    @Test
    fun focusCleanupOnlyTouchesFocus() {
        val cleaned = data.applyCleanup(Cleanup.FocusHistory, today)
        assertTrue(cleaned.focusSessions.isEmpty())
        assertEquals(data.tasks, cleaned.tasks)
        assertEquals(data.totalRecords() - 1, cleaned.totalRecords())
    }

    @Test
    fun customQuotesJoinRotation() {
        val mine = Quote("Kendi sözüm", "Payas")
        val withMine = AppData().addQuote(mine)
        assertEquals(mine, quoteFor(QUOTES.size, withMine.customQuotes))
        assertEquals(QUOTES[0], quoteFor(0, withMine.customQuotes))
        assertTrue(withMine.removeQuote(0).customQuotes.isEmpty())
    }

    @Test
    fun blankContentIsIgnored() {
        assertEquals(AppData(), AppData().addQuote(Quote("  ", "x")))
        assertEquals(AppData(), AppData().addSong(Song("", "x")))
        assertEquals("Bilinmiyor", AppData().addQuote(Quote("Söz", " ")).customQuotes[0].author)
    }

    @Test
    fun customSongsJoinRotation() {
        val withMine = AppData().addSong(Song("Benim şarkım", "Ben"))
        assertEquals("Benim şarkım", songFor(SONGS.size, withMine.customSongs).title)
    }

    @Test
    fun backupRoundTripKeepsEverything() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val full = data.addQuote(Quote("Söz", "Ben")).addSong(Song("Şarkı", "Ben"))
        val text = json.encodeToString(AppData.serializer(), full)
        assertEquals(full, json.decodeFromString(AppData.serializer(), text))
    }

    @Test
    fun pinRules() {
        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("12345678"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("12a4"))
        assertFalse(isValidPin("123456789"))
    }

    @Test
    fun pinHashDependsOnSalt() {
        assertEquals(hashPin("1234", "a"), hashPin("1234", "a"))
        assertNotEquals(hashPin("1234", "a"), hashPin("1234", "b"))
        assertNotEquals(hashPin("1234", "a"), hashPin("4321", "a"))
    }
}
