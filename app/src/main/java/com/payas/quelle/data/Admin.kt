package com.payas.quelle.data

import java.security.MessageDigest
import java.time.LocalDate

// ---- Yönetim paneli: veri özeti, temizlik, içerik ve PIN ----

data class DataCount(val label: String, val count: Int)

/** Yönetim panelindeki "Veri özeti" kartı için her türden kaç kayıt olduğu. */
fun AppData.summary(): List<DataCount> = listOf(
    DataCount("Görev", tasks.size),
    DataCount("Program", schedule.size),
    DataCount("Alışkanlık", habits.size),
    DataCount("Alışkanlık kaydı", habitEntries.size),
    DataCount("Ruh hali", moods.size),
    DataCount("Odak oturumu", focusSessions.size),
    DataCount("Sınav ve ödev", assignments.size),
    DataCount("Not", notes.size),
    DataCount("Günlük hedef", dailyGoals.size),
    DataCount("Haftalık değerlendirme", weeklyReviews.size),
    DataCount("Kendi sözün", customQuotes.size),
    DataCount("Kendi şarkın", customSongs.size),
)

fun AppData.totalRecords(): Int = summary().sumOf { it.count }

/** Panelden tek dokunuşla yapılabilen temizlikler. */
enum class Cleanup(val label: String, val description: String) {
    DoneTasks("Tamamlanan görevler", "İşaretlenmiş tüm görevleri siler."),
    OldTasks("30 günden eski görevler", "Tarihi 30 günden eski görevleri siler."),
    DoneAssignments("Biten sınav ve ödevler", "Tamamlandı olarak işaretlenenleri siler."),
    FocusHistory("Odak geçmişi", "Tüm odak oturumu kayıtlarını siler."),
    Moods("Ruh hali geçmişi", "Kaydedilen tüm ruh hallerini siler."),
    HabitHistory("Alışkanlık geçmişi", "Alışkanlıklar kalır, günlük sayımları sıfırlanır."),
    Goals("Günlük hedefler", "Geçmiş tüm günlük hedefleri siler."),
    Reviews("Haftalık değerlendirmeler", "Tüm haftalık değerlendirmeleri siler."),
}

/** Temizliğin uygulanmış hali. Hiçbir şey değişmezse aynı nesne döner. */
fun AppData.applyCleanup(cleanup: Cleanup, today: LocalDate): AppData = when (cleanup) {
    Cleanup.DoneTasks -> copy(tasks = tasks.filterNot { it.done })
    Cleanup.OldTasks -> {
        val limit = today.minusDays(30).toString()
        copy(tasks = tasks.filterNot { it.date < limit })
    }
    Cleanup.DoneAssignments -> copy(assignments = assignments.filterNot { it.done })
    Cleanup.FocusHistory -> copy(focusSessions = emptyList())
    Cleanup.Moods -> copy(moods = emptyList())
    Cleanup.HabitHistory -> copy(habitEntries = emptyList())
    Cleanup.Goals -> copy(dailyGoals = emptyList())
    Cleanup.Reviews -> copy(weeklyReviews = emptyList())
}

/** Temizlik uygulanırsa kaç kaydın silineceği. */
fun AppData.cleanupCount(cleanup: Cleanup, today: LocalDate): Int =
    totalRecords() - applyCleanup(cleanup, today).totalRecords()

fun AppData.addQuote(quote: Quote): AppData {
    if (quote.text.isBlank()) return this
    return copy(customQuotes = customQuotes + Quote(quote.text.trim(), quote.author.trim().ifEmpty { "Bilinmiyor" }))
}

fun AppData.removeQuote(index: Int): AppData =
    copy(customQuotes = customQuotes.filterIndexed { i, _ -> i != index })

fun AppData.addSong(song: Song): AppData {
    if (song.title.isBlank()) return this
    return copy(customSongs = customSongs + Song(song.title.trim(), song.artist.trim()))
}

fun AppData.removeSong(index: Int): AppData =
    copy(customSongs = customSongs.filterIndexed { i, _ -> i != index })

// ---- PIN ----

/** PIN 4 ile 8 rakam arasında olmalı. */
fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all { it.isDigit() }

/** PIN düz metin olarak saklanmaz; tuzla birlikte SHA-256 özeti tutulur. */
fun hashPin(pin: String, salt: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$salt:$pin".toByteArray())
        .joinToString("") { "%02x".format(it) }
