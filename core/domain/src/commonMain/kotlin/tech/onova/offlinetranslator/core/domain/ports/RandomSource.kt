package tech.onova.offlinetranslator.core.domain.ports

import kotlin.random.Random

/** Randomness that cannot be predicted from earlier results (spec 004 FR-027, research.md R11). */
public fun interface RandomSource {
    public fun random(): Random
}
