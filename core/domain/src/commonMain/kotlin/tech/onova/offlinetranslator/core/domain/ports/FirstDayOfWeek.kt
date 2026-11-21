package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.datetime.DayOfWeek

/** The first day of the week for the app's locale; the weekday chips start there (spec 013 FR-013). */
public fun interface FirstDayOfWeek {
    public fun get(): DayOfWeek
}
