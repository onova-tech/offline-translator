package tech.onova.offlinetranslator.core.domain.ads

import kotlinx.datetime.LocalDate

/** Tools opened on [date] since the last shown ad (spec 002 data-model.md). */
public data class DailyUsage(
    val date: LocalDate,
    val count: Int,
) {
    init {
        require(count >= 0) { "count must not be negative: $count" }
    }
}
