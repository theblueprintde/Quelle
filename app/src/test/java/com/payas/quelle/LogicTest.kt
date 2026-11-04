package com.payas.quelle

import com.payas.quelle.data.AppData
import com.payas.quelle.data.ScheduleItem
import com.payas.quelle.data.Task
import com.payas.quelle.data.formatMinutes
import com.payas.quelle.data.quoteFor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test
    fun greetingChangesWithHour() {
        assertEquals("Günaydın", greetingFor(5))
        assertEquals("Günaydın", greetingFor(11))
        assertEquals("Tünaydın", greetingFor(12))
        assertEquals("Tünaydın", greetingFor(16))
        assertEquals("İyi akşamlar", greetingFor(17))
        assertEquals("İyi akşamlar", greetingFor(21))
        assertEquals("İyi geceler", greetingFor(22))
        assertEquals("İyi geceler", greetingFor(0))
        assertEquals("İyi geceler", greetingFor(4))
    }

    @Test
    fun scheduleIsSortedAndFilteredByDay() {
        val data = AppData(
            schedule = listOf(
                ScheduleItem("a", "Fizik", 3, 600, 680),
                ScheduleItem("b", "Matematik", 3, 510, 590),
                ScheduleItem("c", "Tarih", 2, 510, 590),
            )
        )
        assertEquals(listOf("Matematik", "Fizik"), data.scheduleFor(3).map { it.title })
    }

    @Test
    fun scheduleItemKnowsWhenItIsNow() {
        val item = ScheduleItem("a", "Fizik", 3, 600, 680)
        assertFalse(item.isNow(599))
        assertTrue(item.isNow(600))
        assertTrue(item.isNow(679))
        assertFalse(item.isNow(680))
    }

    @Test
    fun tasksAreFilteredByDate() {
        val data = AppData(
            tasks = listOf(
                Task("1", "Bugün", "2026-11-04", createdAt = 2),
                Task("2", "Yarın", "2026-11-05", createdAt = 1),
                Task("3", "Bugün erken", "2026-11-04", createdAt = 1),
            )
        )
        assertEquals(listOf("Bugün erken", "Bugün"), data.tasksFor("2026-11-04").map { it.title })
    }

    @Test
    fun dataSurvivesSaveAndLoad() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val data = AppData(
            tasks = listOf(Task("1", "Ödev", "2026-11-04", done = true)),
            schedule = listOf(ScheduleItem("a", "Fizik", 3, 600, 680)),
        )
        val text = json.encodeToString(AppData.serializer(), data)
        assertEquals(data, json.decodeFromString(AppData.serializer(), text))
    }

    @Test
    fun minutesAreFormatted() {
        assertEquals("08:30", formatMinutes(510))
        assertEquals("23:05", formatMinutes(1385))
    }

    @Test
    fun quoteRotatesDaily() {
        assertEquals(quoteFor(0), quoteFor(15))
        assertTrue(quoteFor(1) != quoteFor(2))
    }
}
