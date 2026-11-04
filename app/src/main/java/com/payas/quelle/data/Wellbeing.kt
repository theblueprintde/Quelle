package com.payas.quelle.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Habit(
    val id: String,
    val name: String,
    /** Günde kaç kez (8 bardak su = 8, kitap okumak = 1). */
    val target: Int = 1,
    val createdAt: Long = 0,
)

@Serializable
data class HabitEntry(val habitId: String, val date: String, val count: Int)

@Serializable
enum class Mood(val label: String, val color: Long) {
    Harika("Harika", 0xFF3E7D52),
    Iyi("İyi", 0xFF7FC3DE),
    Normal("Normal", 0xFFB9C9CD),
    Yorgun("Yorgun", 0xFFE3B66B),
    Kotu("Kötü", 0xFFD98A8A),
}

@Serializable
data class MoodEntry(val date: String, val mood: Mood)

@Serializable
enum class TimerMode(val label: String, val minutes: Int) {
    Odak("Odak", 25),
    KisaMola("Kısa mola", 5),
    UzunMola("Uzun mola", 15),
}

@Serializable
data class FocusSession(val date: String, val minutes: Int, val label: String = "", val endedAt: Long = 0)

/** Çalışan ya da duraklatılmış zamanlayıcı. Uygulama kapansa bile kaldığı yerden devam eder. */
@Serializable
data class ActiveTimer(
    val mode: TimerMode,
    val label: String = "",
    /** Bitiş anı (epoch ms). Duraklatılmışsa kullanılmaz. */
    val endAt: Long = 0,
    /** Duraklatıldıysa kalan süre (ms). */
    val pausedRemaining: Long? = null,
) {
    val isPaused get() = pausedRemaining != null
    fun remaining(nowMs: Long): Long = pausedRemaining ?: (endAt - nowMs).coerceAtLeast(0)
}

const val DAILY_FOCUS_GOAL = 4

// ---- Alışkanlıklar ----

fun AppData.habitCount(habitId: String, date: String): Int =
    habitEntries.firstOrNull { it.habitId == habitId && it.date == date }?.count ?: 0

fun AppData.setHabitCount(habitId: String, date: String, count: Int): AppData {
    val others = habitEntries.filterNot { it.habitId == habitId && it.date == date }
    return copy(habitEntries = if (count <= 0) others else others + HabitEntry(habitId, date, count))
}

/** Hedefin tutturulduğu ardışık gün sayısı. Bugün henüz yapılmadıysa dünden geriye sayar. */
fun AppData.streak(habit: Habit, today: LocalDate): Int {
    fun done(d: LocalDate) = habitCount(habit.id, d.toString()) >= habit.target
    var day = if (done(today)) today else today.minusDays(1)
    var count = 0
    while (done(day)) {
        count++
        day = day.minusDays(1)
    }
    return count
}

// ---- Ruh hali ----

fun AppData.moodFor(date: String): Mood? = moods.firstOrNull { it.date == date }?.mood

fun AppData.setMood(date: String, mood: Mood?): AppData {
    val others = moods.filterNot { it.date == date }
    return copy(moods = if (mood == null) others else others + MoodEntry(date, mood))
}

// ---- Odak zamanlayıcı ----

fun AppData.focusSessionsOn(date: String) = focusSessions.filter { it.date == date }

fun AppData.startTimer(mode: TimerMode, label: String, nowMs: Long): AppData =
    copy(timer = ActiveTimer(mode, label, endAt = nowMs + mode.minutes * 60_000L))

fun AppData.pauseTimer(nowMs: Long): AppData {
    val t = timer ?: return this
    if (t.isPaused) return this
    return copy(timer = t.copy(pausedRemaining = t.remaining(nowMs)))
}

fun AppData.resumeTimer(nowMs: Long): AppData {
    val t = timer ?: return this
    val left = t.pausedRemaining ?: return this
    return copy(timer = t.copy(endAt = nowMs + left, pausedRemaining = null))
}

fun AppData.resetTimer(): AppData = copy(timer = null)

/** Süre dolduysa zamanlayıcıyı kapatır; odak oturumuysa kaydeder. */
fun AppData.completeTimerIfDone(nowMs: Long, today: String): AppData {
    val t = timer ?: return this
    if (t.isPaused || t.endAt > nowMs) return this
    val sessions = if (t.mode == TimerMode.Odak) {
        focusSessions + FocusSession(today, t.mode.minutes, t.label, t.endAt)
    } else {
        focusSessions
    }
    return copy(timer = null, focusSessions = sessions)
}
