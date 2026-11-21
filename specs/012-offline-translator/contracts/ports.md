# Contract: the translator's ports (spec 012)

Every platform service the translator uses is one of these interfaces in `core/domain` (`ports/` package), so the
iPhone adds its own implementations and nothing else (FR-029, Principle VIII). Android implementations are bound in
`androidApp`'s `AppModule`; tests bind fakes in `KoinFakesRule`. Signatures are the contract; KDoc wording may vary.

## ContentPacks (`ports/ContentPorts.kt`)

Implemented by `core/content` (Google Play asset delivery, research R5); the only network in this feature, and only
after the person confirms (FR-010, FR-013).

```kotlin
public interface ContentPacks {
    /** Every pack the app knows, with its state; updates while a download runs. Hot, never completes. */
    public val packs: StateFlow<List<PackStatus>>

    /** Asks the store for the packs' sizes and states; nothing is downloaded. */
    public suspend fun refresh()

    /** Starts the download the person confirmed. Checks Wi-Fi (when [wifiOnly]) and free space first. */
    public suspend fun download(pack: PackId, wifiOnly: Boolean)

    /** Stops a download and leaves nothing behind. */
    public suspend fun cancel(pack: PackId)

    /** Deletes an installed pack. */
    public suspend fun remove(pack: PackId)

    /** The folder of an installed pack's files, or null when it isn't installed (or is damaged). */
    public fun location(pack: PackId): String?
}

public data class PackStatus(val id: PackId, val downloadBytes: Long?, val installedBytes: Long?, val state: PackState)
```

`PackState` is data-model.md's. Ownership: `core/content`'s implementation is a process-wide singleton, so it follows
`ProcessVisibility` if it ever needs to (it doesn't: downloads continue in the background through the store).

## TranslationEngine (`ports/TranslationPorts.kt`)

Implemented by `core/translation` (Bergamot through JNI, research R3).

```kotlin
public interface TranslationEngine {
    /** Loads the models of the installed [packs] (folders from [ContentPacks.location]). Slow: off the main thread. */
    public suspend fun open(packs: Map<PackId, String>): EngineOpen

    /** Translates [text] along [route]. Runs on the engine's own thread; a newer call doesn't interrupt an older one. */
    public suspend fun translate(text: String, route: Route): String

    /** Frees the models. Called when the tool is left. */
    public fun close()
}

public sealed interface EngineOpen {
    public data object Ready : EngineOpen
    public data class Unavailable(val reason: EngineUnavailable) : EngineOpen // UNSUPPORTED_DEVICE, DAMAGED_PACK
}
```

`Route` is data-model.md's (`Same`, `Direct(from, to, pack)`, `ThroughEnglish(from, to, first, second)`): it carries
the two languages too, since a pack holds both directions with English and the engine picks one (as built,
2026-11-17). For `ThroughEnglish`, Bergamot's pivoting API is used with both models loaded; the fake engine composes two
direct calls.

## SpeechInput (`ports/SpeechPorts.kt`)

Implemented in `core/platform` (`speech/`), Android's on-device recognizer (research R1). iPhone: `SFSpeechRecognizer`
with on-device recognition required.

```kotlin
public interface SpeechInput {
    /** What the phone can do for [speechTag] (BCP-47). Never starts the microphone. */
    public suspend fun support(speechTag: String): RecognitionSupport

    /** Asks the phone's own speech service to get [speechTag] (Android 13+); false when it can't be asked. */
    public fun requestLanguage(speechTag: String): Boolean

    /**
     * Cold flow: listens while collected, on the device only, and ends after the final result, an error, or
     * cancellation. Audio is never stored or sent.
     */
    public fun listen(speechTag: String): Flow<SpeechEvent>
}

public sealed interface SpeechEvent {
    public data class Partial(val text: String) : SpeechEvent
    public data class Final(val text: String) : SpeechEvent
    public data class Failed(val reason: SpeechFailure) : SpeechEvent // NOTHING_HEARD, LANGUAGE_MISSING, BUSY, NO_PERMISSION, OTHER
}
```

`RecognitionSupport` is data-model.md's `SpeechSupport.recognition`.

## SpeechOutput (`ports/SpeechPorts.kt`)

Implemented in `core/platform` (`speech/`), Android's `TextToSpeech` with offline voices only (research R2). iPhone:
`AVSpeechSynthesizer`.

```kotlin
public interface SpeechOutput {
    /** Whether an offline voice exists for [speechTag]. */
    public suspend fun voice(speechTag: String): VoiceSupport

    /**
     * Speaks [text] and returns how it ended: DONE (said, or stopped), INTERRUPTED (a call or an alarm took the audio
     * focus, or it couldn't be had), FAILED. Takes audio focus. (T073: was a Boolean.)
     */
    public suspend fun speak(text: String, speechTag: String): SpeechEnd

    /** Stops speaking at once. */
    public fun stop()

    /** Opens the phone's text-to-speech settings, where voices are added. */
    public fun openVoiceSettings()
}
```

## Ports reused as they are

`PermissionController` (`RECORD_AUDIO`), `AppVisibility`, `ClipboardWriter`, `TextSharer`, `KeepScreenOn`
(`core/tool-api`).

## ScreenReader (`ports/ScreenPorts.kt`, new)

The app has no way yet to know a screen reader is on; Compose's `LocalAccessibilityManager` doesn't say. Android:
`AccessibilityManager.isTouchExplorationEnabled` with its change listener; iPhone: `UIAccessibility.isVoiceOverRunning`.

```kotlin
public interface ScreenReader {
    /** True while TalkBack (or VoiceOver) reads the screen. */
    public val on: StateFlow<Boolean>
}
```

With it on, a conversation announces each translation instead of the app's own voice reading it (research R8).

## Fakes (`androidApp/src/test/.../support/KoinFakesRule` and the feature's `commonTest`)

| Fake | Behaviour the tests drive |
|---|---|
| `FakeContentPacks` | states set by the test; `download` moves through `Downloading` to `Installed` step by step |
| `FakeTranslationEngine` | returns `"[to] text"`; can be made slow (to test the newest-wins rule) or to fail |
| `FakeSpeechInput` | plays a script of `Partial`/`Final`/`Failed` events; support per tag set by the test |
| `FakeSpeechOutput` | records what it said; `speak` suspends until the test says "done" |
