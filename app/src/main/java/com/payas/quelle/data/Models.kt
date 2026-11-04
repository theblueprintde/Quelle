package com.payas.quelle.data

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val title: String,
    /** Görevin günü, ISO biçiminde (2026-11-04). */
    val date: String,
    val done: Boolean = false,
    val createdAt: Long = 0,
)

/** Haftalık tekrar eden program öğesi (ders, etkinlik...). */
@Serializable
data class ScheduleItem(
    val id: String,
    val title: String,
    /** 1 = Pazartesi ... 7 = Pazar */
    val dayOfWeek: Int,
    /** Gece yarısından itibaren dakika (08:30 = 510). */
    val startMinutes: Int,
    val endMinutes: Int,
    val kind: ScheduleKind = ScheduleKind.Ders,
) {
    fun isNow(minutesOfDay: Int) = minutesOfDay in startMinutes until endMinutes
}

@Serializable
enum class ScheduleKind(val label: String) {
    Ders("Ders"),
    Etkinlik("Etkinlik"),
    Spor("Spor"),
    Calisma("Çalışma"),
    Diger("Diğer"),
}

@Serializable
data class AppData(
    val tasks: List<Task> = emptyList(),
    val schedule: List<ScheduleItem> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitEntries: List<HabitEntry> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val focusSessions: List<FocusSession> = emptyList(),
    val timer: ActiveTimer? = null,
    val assignments: List<Assignment> = emptyList(),
    val notes: List<Note> = emptyList(),
    val dailyGoals: List<DailyGoal> = emptyList(),
    val weeklyReviews: List<WeeklyReview> = emptyList(),
) {
    fun tasksFor(date: String) = tasks.filter { it.date == date }.sortedBy { it.createdAt }

    fun scheduleFor(dayOfWeek: Int) =
        schedule.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startMinutes }
}

fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
