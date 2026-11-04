package com.payas.quelle.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Tüm veriler tabletteki tek bir JSON dosyasında durur.
 * Google senkronu eklendiğinde bu sınıfın arkası buluta bağlanacak.
 */
class QuelleStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            json.decodeFromString(AppData.serializer(), file.readText())
        } catch (e: Exception) {
            // Bozuk dosyayı silmek yerine yedekle.
            file.copyTo(File(file.parentFile, file.name + ".bozuk"), overwrite = true)
            AppData()
        }
    }

    private fun update(change: (AppData) -> AppData) {
        _data.updateAndGet(change)
        scope.launch {
            writeLock.withLock {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(AppData.serializer(), _data.value))
                tmp.renameTo(file)
            }
        }
    }

    fun addTask(title: String, date: String) = update { d ->
        d.copy(tasks = d.tasks + Task(UUID.randomUUID().toString(), title.trim(), date, createdAt = System.currentTimeMillis()))
    }

    fun toggleTask(id: String) = update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun deleteTask(id: String) = update { d -> d.copy(tasks = d.tasks.filterNot { it.id == id }) }

    fun saveSchedule(item: ScheduleItem) = update { d ->
        val exists = d.schedule.any { it.id == item.id }
        d.copy(schedule = if (exists) d.schedule.map { if (it.id == item.id) item else it } else d.schedule + item)
    }

    fun deleteSchedule(id: String) = update { d -> d.copy(schedule = d.schedule.filterNot { it.id == id }) }

    fun saveHabit(habit: Habit) = update { d ->
        val exists = d.habits.any { it.id == habit.id }
        d.copy(habits = if (exists) d.habits.map { if (it.id == habit.id) habit else it } else d.habits + habit)
    }

    fun deleteHabit(id: String) = update { d ->
        d.copy(habits = d.habits.filterNot { it.id == id }, habitEntries = d.habitEntries.filterNot { it.habitId == id })
    }

    fun setHabitCount(habitId: String, date: String, count: Int) = update { it.setHabitCount(habitId, date, count) }

    fun setMood(date: String, mood: Mood?) = update { it.setMood(date, mood) }

    fun startTimer(mode: TimerMode, label: String) = update { it.startTimer(mode, label, System.currentTimeMillis()) }

    fun pauseTimer() = update { it.pauseTimer(System.currentTimeMillis()) }

    fun resumeTimer() = update { it.resumeTimer(System.currentTimeMillis()) }

    fun resetTimer() = update { it.resetTimer() }

    fun completeTimerIfDone(today: String) {
        val now = System.currentTimeMillis()
        val current = _data.value
        if (current.completeTimerIfDone(now, today) != current) update { it.completeTimerIfDone(now, today) }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
