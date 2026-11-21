package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport

/** What the phone can do for one half's language. */
internal data class SideSupport(
    val recognition: RecognitionSupport = RecognitionSupport.UNKNOWN,
    val voice: VoiceSupport = VoiceSupport.READY,
) {
    /** The phone can listen to this language now, or (Android 12) finds out by trying. */
    val canListen: Boolean
        get() = recognition == RecognitionSupport.READY || recognition == RecognitionSupport.UNKNOWN
}

/** The kinds of speech notice a half's language can have; with its half, a notice the person dismissed (its ×). */
internal enum class SetupKind { RECOGNITION, NOT_INSTALLED, VOICE, NO_RECOGNITION, APP_RECOGNITION, APP_VOICE }

internal data class SetupKey(
    val kind: SetupKind,
    val side: Side,
)

/**
 * One thing the phone's owner sets up before a conversation works (spec 012 story 5, scenario 6). Owner, 2026-11-19:
 * it shows upright in the near half, one thing at a time, like the phone's own dialogs, rather than turned inside
 * the half whose language it is. Owner, 2026-11-20: a speech notice has a small × ([key]); the translation pack's
 * has none ([key] null), since nothing works without it.
 */
internal sealed interface SetupNeed {
    val key: SetupKey?

    /** The translation pack the pair needs. */
    data class Pack(
        val offer: PackOffer,
    ) : SetupNeed {
        override val key: SetupKey? get() = null
    }

    /** The phone's own speech service can get the language of [side], or is getting it ([downloading]). */
    data class Recognition(
        val side: Side,
        val downloading: Boolean,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.RECOGNITION, side)
    }

    /** The phone was asked for the language of [side] and never installed it: its speech service may need an update. */
    data class NotInstalled(
        val side: Side,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.NOT_INSTALLED, side)
    }

    /** No offline voice for the language of [side]: the phone's voice app can download one. */
    data class Voice(
        val side: Side,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.VOICE, side)
    }

    /** The phone can't, and the app's own recognition can, once downloaded or when in ([downloading]) (spec 082). */
    data class AppRecognition(
        val side: Side,
        val downloading: Boolean,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.APP_RECOGNITION, side)
    }

    /** No voice on the phone, and the app's own has one, once downloaded or when it's in ([downloading]) (spec 082). */
    data class AppVoice(
        val side: Side,
        val downloading: Boolean,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.APP_VOICE, side)
    }

    /** The phone can't recognize the language of [side] offline, and can't get it. */
    data class NoRecognition(
        val side: Side,
    ) : SetupNeed {
        override val key: SetupKey get() = SetupKey(SetupKind.NO_RECOGNITION, side)
    }
}

internal object SetupNeeds {
    /** While a language is on its way, the phone is asked again this often: its notice moves on by itself. */
    const val RECHECK_MILLIS: Long = 3_000

    /** And at most this many times (5 minutes): a download the person put off isn't asked about forever. */
    const val RECHECK_TIMES: Int = 100

    private val SIDES = listOf(Side.NEAR, Side.FAR)

    /**
     * The first thing to set up that the person hasn't [dismissed]: the translation pack (nothing works without it);
     * then speech the phone can get, the near half's language first; then a language the phone didn't install; then a
     * voice, when something is read aloud ([voices]); last, what can't be had, so it never hides what can.
     */
    fun first(
        offer: PackOffer?,
        near: SideSupport,
        far: SideSupport,
        voices: Boolean,
        dismissed: Set<SetupKey> = emptySet(),
    ): SetupNeed? = all(offer, near, far, voices).firstOrNull { it.key !in dismissed }

    private fun all(
        offer: PackOffer?,
        near: SideSupport,
        far: SideSupport,
        voices: Boolean,
    ): List<SetupNeed> {
        val supportOf = { side: Side -> if (side == Side.NEAR) near else far }
        return buildList {
            offer?.let { add(SetupNeed.Pack(it)) }
            SIDES.forEach { side ->
                supportOf(side).recognition.onItsWay()?.let { downloading ->
                    add(SetupNeed.Recognition(side, downloading))
                }
                supportOf(side).recognition.appOnItsWay()?.let { downloading ->
                    add(SetupNeed.AppRecognition(side, downloading))
                }
            }
            SIDES
                .filter { supportOf(it).recognition == RecognitionSupport.NOT_INSTALLED }
                .forEach { add(SetupNeed.NotInstalled(it)) }
            SIDES
                .filter { voices && supportOf(it).voice == VoiceSupport.NO_OFFLINE_VOICE }
                .forEach { add(SetupNeed.Voice(it)) }
            SIDES.forEach { side ->
                supportOf(side)
                    .voice
                    .appOnItsWay()
                    ?.takeIf { voices }
                    ?.let { add(SetupNeed.AppVoice(side, it)) }
            }
            SIDES
                .filter { supportOf(it).cantHave() }
                .forEach { add(SetupNeed.NoRecognition(it)) }
        }
    }

    /**
     * Whether a language the phone can get, is getting, or didn't install may still become ready: the phone is asked
     * again (a download the person confirmed late still lands).
     */
    fun waiting(
        near: SideSupport,
        far: SideSupport,
    ): Boolean =
        listOf(near, far).any {
            it.recognition.onItsWay() != null || it.recognition == RecognitionSupport.NOT_INSTALLED ||
                it.recognition.appOnItsWay() != null || it.voice.appOnItsWay() != null
        }
}

/** The phone can't recognize this language and can't get it (not merely not yet). */
private fun SideSupport.cantHave(): Boolean =
    !canListen && recognition.onItsWay() == null && recognition != RecognitionSupport.NOT_INSTALLED &&
        recognition.appOnItsWay() == null

/** Null when nothing is coming; else whether it's downloading now (true) or can be asked for (false). */
private fun RecognitionSupport.onItsWay(): Boolean? =
    when (this) {
        RecognitionSupport.CAN_DOWNLOAD -> false
        RecognitionSupport.DOWNLOADING -> true
        else -> null
    }

/** The app's own recognition (spec 082): null when it isn't the way; else whether it's downloading now. */
private fun RecognitionSupport.appOnItsWay(): Boolean? =
    when (this) {
        RecognitionSupport.APP_CAN_DOWNLOAD -> false
        RecognitionSupport.APP_DOWNLOADING -> true
        else -> null
    }

/** The app's own voice (spec 082): null when it isn't the way; else whether it's downloading now. */
private fun VoiceSupport.appOnItsWay(): Boolean? =
    when (this) {
        VoiceSupport.APP_CAN_DOWNLOAD -> false
        VoiceSupport.APP_DOWNLOADING -> true
        else -> null
    }
