package com.payas.quelle.reminders

import com.payas.quelle.data.ScheduleItem
import java.time.LocalDateTime

/** Dersten kaç dakika önce hatırlatılacağı. */
const val REMINDER_LEAD_MINUTES = 15L

data class UpcomingReminder(val item: ScheduleItem, val triggerAt: LocalDateTime, val startsAt: LocalDateTime)

/** Şu andan sonraki ilk hatırlatmayı bulur (en fazla bir hafta ileriye bakar). */
fun nextReminder(
    items: List<ScheduleItem>,
    now: LocalDateTime,
    leadMinutes: Long = REMINDER_LEAD_MINUTES,
): UpcomingReminder? {
    var best: UpcomingReminder? = null
    for (offset in 0..7L) {
        val date = now.toLocalDate().plusDays(offset)
        for (item in items) {
            if (item.dayOfWeek != date.dayOfWeek.value) continue
            val startsAt = date.atStartOfDay().plusMinutes(item.startMinutes.toLong())
            val triggerAt = startsAt.minusMinutes(leadMinutes)
            if (!triggerAt.isAfter(now)) continue
            if (best == null || triggerAt.isBefore(best.triggerAt)) {
                best = UpcomingReminder(item, triggerAt, startsAt)
            }
        }
        if (best != null) return best
    }
    return best
}
