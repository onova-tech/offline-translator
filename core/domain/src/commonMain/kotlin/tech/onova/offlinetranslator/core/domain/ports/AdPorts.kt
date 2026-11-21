package tech.onova.offlinetranslator.core.domain.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import tech.onova.offlinetranslator.core.domain.ads.DailyUsage

/** The local calendar date in the device's current time zone (spec 002 research R5). */
public fun interface Today {
    public fun date(): LocalDate
}

/** Stores the day's tool count (`daily_usage.json`). */
public interface DailyUsageRepository {
    /** `null` when nothing was recorded yet, or the stored document is unreadable. */
    public suspend fun read(): DailyUsage?

    public suspend fun write(usage: DailyUsage)
}

/** The single switch for every ad, the consent form and the ad SDK start-up (spec 002 FR-018). */
public fun interface AdsEnabled {
    public suspend fun isEnabled(): Boolean
}

public enum class AdShowOutcome { SHOWN, NOT_READY, FAILED }

/** The full-screen ad slot. Implementations never make the caller wait for an ad to load. */
public interface InterstitialAds {
    /** Starts loading one ad unless a fresh one is ready or loading; returns at once. */
    public fun prepare()

    /**
     * Shows the ready ad and returns once the ad network confirms it is on screen; returns
     * [AdShowOutcome.NOT_READY] at once when no fresh ad is held.
     */
    public suspend fun show(): AdShowOutcome
}

/** The ad network's consent provider (GDPR and similar laws). */
public interface AdConsent {
    /** Refreshes the consent status; returns when done, including when it fails offline. */
    public suspend fun refresh()

    /** Shows the consent form if the user must answer it; returns when it closes or is not needed. */
    public suspend fun showFormIfRequired()

    /** Whether ads may be requested now: consent obtained, or not required. */
    public fun canRequestAds(): Boolean
}

public enum class PrivacyChoicesResult { OPENED, UNAVAILABLE }

/** The "Ad privacy choices" entry in Settings (spec 002 US3). */
public interface PrivacyChoices {
    /** `true` only when the consent provider requires the entry for this user. */
    public val required: Flow<Boolean>

    public suspend fun open(): PrivacyChoicesResult
}
