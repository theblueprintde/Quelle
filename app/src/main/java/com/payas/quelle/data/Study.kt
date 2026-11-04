package com.payas.quelle.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Serializable
enum class AssignmentKind(val label: String) { Sinav("Sınav"), Odev("Ödev") }

@Serializable
data class Assignment(
    val id: String,
    val title: String,
    val kind: AssignmentKind = AssignmentKind.Odev,
    val dueDate: String,
    val detail: String = "",
    /** Hazırlık yüzdesi (0-100). */
    val progress: Int = 0,
    val done: Boolean = false,
)

@Serializable
enum class NoteColor(val argb: Long) {
    Mavi(0xFFA7D8F0),
    Yesil(0xFFDCEEDC),
    Acik(0xFFDCEFFA),
    Beyaz(0xFFFFFFFF),
    Sari(0xFFFCE9C8),
}

@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String = "",
    val color: NoteColor = NoteColor.Mavi,
    val updatedAt: Long = 0,
)

@Serializable
data class DailyGoal(val date: String, val text: String, val done: Boolean = false)

@Serializable
data class WeeklyReview(
    /** Haftanın pazartesisi. */
    val weekStart: String,
    val achieved: String = "",
    val missed: String = "",
    val nextGoal: String = "",
)

fun daysUntil(dueDate: String, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, LocalDate.parse(dueDate))

fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun AppData.goalFor(date: String): DailyGoal? = dailyGoals.firstOrNull { it.date == date }

fun AppData.setGoal(goal: DailyGoal?, date: String): AppData {
    val others = dailyGoals.filterNot { it.date == date }
    return copy(dailyGoals = if (goal == null || goal.text.isBlank()) others else others + goal)
}

fun AppData.reviewFor(weekStart: String): WeeklyReview =
    weeklyReviews.firstOrNull { it.weekStart == weekStart } ?: WeeklyReview(weekStart)

fun AppData.saveReview(review: WeeklyReview): AppData =
    copy(weeklyReviews = weeklyReviews.filterNot { it.weekStart == review.weekStart } + review)

/** Sınavlar/ödevler: bitmeyenler tarih sırasıyla önde, bitenler sonda. */
fun AppData.sortedAssignments(kind: AssignmentKind): List<Assignment> =
    assignments.filter { it.kind == kind }.sortedWith(compareBy({ it.done }, { it.dueDate }))

data class WeekStats(
    val tasksDone: Int,
    val tasksTotal: Int,
    val focusSessions: Int,
    val bestStreak: Int,
    /** Pazartesiden pazara her gün tamamlanan görev sayısı. */
    val doneByDay: List<Int>,
)

fun AppData.weekStats(weekStart: LocalDate, today: LocalDate): WeekStats {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val keys = days.map { it.toString() }.toSet()
    val weekTasks = tasks.filter { it.date in keys }
    val lastDay = minOf(days.last(), today)
    return WeekStats(
        tasksDone = weekTasks.count { it.done },
        tasksTotal = weekTasks.size,
        focusSessions = focusSessions.count { it.date in keys },
        bestStreak = habits.maxOfOrNull { streak(it, lastDay) } ?: 0,
        doneByDay = days.map { d -> weekTasks.count { it.date == d.toString() && it.done } },
    )
}
