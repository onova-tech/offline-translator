package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.RecognitionSupport
import tech.onova.offlinetranslator.core.domain.ports.SpeechEvent
import tech.onova.offlinetranslator.core.domain.ports.VoiceSupport
import tech.onova.offlinetranslator.core.domain.speech.AppSpeechPart
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeContentPacks
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.fakes.FakeSpeechOutput
import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec 082 T013: the Translator's speech goes to the phone's or the app's, per language. */
class RoutedSpeechTest {
    private class InMemorySettings(
        initial: TranslatorSettings = TranslatorSettings(),
    ) : TranslatorSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
            settings.value = transform(settings.value)
        }
    }

    private val phoneIn = FakeSpeechInput()
    private val appIn = FakeSpeechInput()
    private val phoneOut = FakeSpeechOutput()
    private val appOut = FakeSpeechOutput()
    private val settings = InMemorySettings()
    private val whisper = SpeechCatalog.recognitionPack
    private val portugueseVoice = SpeechCatalog.voices.getValue(LanguageCode("pt")).pack

    private fun packs(installed: List<PackId> = emptyList()): FakeContentPacks {
        val all = listOf(whisper, portugueseVoice, SpeechCatalog.voiceDataPack)
        return FakeContentPacks(
            *all
                .map { if (it in installed) FakeContentPacks.installed(it) else FakeContentPacks.notInstalled(it) }
                .toTypedArray(),
        )
    }

    private var now = 0L
    private val downloads = SpeechDownloads { now }

    private fun input(packs: FakeContentPacks) =
        RoutedSpeechInput(phoneIn, appIn, AppSpeechParts(packs), settings, downloads)

    private fun output(packs: FakeContentPacks) = RoutedSpeechOutput(phoneOut, appOut, AppSpeechParts(packs), settings)

    /** The owner's phone has no Arabic recognizer: the app's is offered, then heard through once it's in. */
    @Test
    fun aLanguageThePhoneCantHearGoesToTheApp() =
        runTest {
            phoneIn.support["ar-SA"] = RecognitionSupport.UNSUPPORTED
            assertEquals(RecognitionSupport.APP_CAN_DOWNLOAD, input(packs()).support("ar-SA"))
            val ready = input(packs(listOf(whisper)))
            assertEquals(RecognitionSupport.READY, ready.support("ar-SA"))
            appIn.hear(SpeechEvent.Final("مرحبا"))
            assertEquals(listOf<SpeechEvent>(SpeechEvent.Final("مرحبا")), ready.listen("ar-SA").toList())
            assertEquals(listOf("ar-SA"), appIn.listenedTo)
            assertEquals(emptyList(), phoneIn.listenedTo)
        }

    @Test
    fun aLanguageThePhoneHearsStaysWithThePhone() =
        runTest {
            val routed = input(packs(listOf(whisper)))
            assertEquals(RecognitionSupport.READY, routed.support("pt-BR"))
            phoneIn.hear(SpeechEvent.Final("bom dia"))
            routed.listen("pt-BR").toList()
            assertEquals(listOf("pt-BR"), phoneIn.listenedTo)
        }

    /** US3: "App" chosen for Portuguese, with its model in, is used even though the phone could. */
    @Test
    fun thePersonsChoiceIsFollowed() =
        runTest {
            val choice = SpeechChoice(recognition = SpeechEngine.APP)
            settings.settings.value = TranslatorSettings(speech = mapOf(LanguageCode("pt") to choice))
            appIn.hear(SpeechEvent.Final("bom dia"))
            input(packs(listOf(whisper))).listen("pt-BR").toList()
            assertEquals(listOf("pt-BR"), appIn.listenedTo)
        }

    /** A voice needs its own pack and espeak-ng's data: until both are in, it's offered. */
    @Test
    fun aVoiceNeedsBothItsPacks() =
        runTest {
            phoneOut.voices["pt-BR"] = VoiceSupport.NO_OFFLINE_VOICE
            assertEquals(VoiceSupport.APP_CAN_DOWNLOAD, output(packs(listOf(portugueseVoice))).voice("pt-BR"))
            val ready = output(packs(listOf(portugueseVoice, SpeechCatalog.voiceDataPack)))
            assertEquals(VoiceSupport.READY, ready.voice("pt-BR"))
        }

    @Test
    fun aDownloadOnItsWayShowsAsSuch() =
        runTest {
            phoneOut.voices["pt-BR"] = VoiceSupport.NO_OFFLINE_VOICE
            val packs = packs(listOf(SpeechCatalog.voiceDataPack))
            packs.put(PackStatus(portugueseVoice, 1, null, PackState.Downloading(1, 2)))
            assertEquals(VoiceSupport.APP_DOWNLOADING, output(packs).voice("pt-BR"))
        }

    /** Arabic has no free voice (research R4): the phone's state stays. */
    @Test
    fun aLanguageWithoutAnAppVoiceKeepsThePhones() =
        runTest {
            phoneOut.voices["ar-SA"] = VoiceSupport.NO_OFFLINE_VOICE
            assertEquals(VoiceSupport.NO_OFFLINE_VOICE, output(packs()).voice("ar-SA"))
        }

    @Test
    fun stopStopsBoth() {
        output(packs()).stop()
        assertEquals(1, phoneOut.stops)
        assertEquals(1, appOut.stops)
    }

    /** The owner's Japanese: asked for, never installed; the app's recognition is offered instead of asking again. */
    @Test
    fun aLanguageThePhoneNeverInstallsGoesToTheApp() =
        runTest {
            phoneIn.support["ja-JP"] = RecognitionSupport.CAN_DOWNLOAD
            downloads.asked("ja-JP")
            now += SpeechDownloads.ASKED_MILLIS + 1
            assertEquals(RecognitionSupport.APP_CAN_DOWNLOAD, input(packs()).support("ja-JP"))
            // A screen reads the routed answer again: what was learned about the phone stays.
            assertEquals(
                RecognitionSupport.APP_CAN_DOWNLOAD,
                downloads.read("ja-JP", RecognitionSupport.APP_CAN_DOWNLOAD),
            )
            assertEquals(RecognitionSupport.APP_CAN_DOWNLOAD, input(packs()).support("ja-JP"), "it stays with the app")
        }

    /** Without the app's recognition (Uyghur), the phone's own answer goes on, for the screens to read. */
    @Test
    fun withoutTheAppsRecognitionThePhonesAnswerGoesOn() =
        runTest {
            phoneIn.support["ug-CN"] = RecognitionSupport.CAN_DOWNLOAD
            downloads.asked("ug-CN")
            now += SpeechDownloads.ASKED_MILLIS + 1
            assertEquals(RecognitionSupport.CAN_DOWNLOAD, input(packs()).support("ug-CN"))
        }

    /** Spec 082 US3: the choice is offered only where both can, and shows the person's even before the app's is in. */
    @Test
    fun theChoiceIsOfferedWhereBothCan() =
        runTest {
            phoneIn.support["pt-BR"] = RecognitionSupport.READY
            phoneIn.support["ar-SA"] = RecognitionSupport.UNSUPPORTED
            val input = input(packs())
            assertEquals(EngineUse(SpeechEngine.PHONE, null, choosable = true), input.supportAndUse("pt-BR").second)
            assertEquals(EngineUse(SpeechEngine.APP, null, choosable = false), input.supportAndUse("ar-SA").second)
            val choices = SpeechChoices(input, output(packs()), settings)
            choices.choose(LanguageCode("pt"), SpeechKind.RECOGNITION, SpeechEngine.APP)
            assertEquals(
                EngineUse(SpeechEngine.PHONE, SpeechEngine.APP, choosable = true),
                input.supportAndUse("pt-BR").second,
            )
            assertEquals(true, input.supportAndUse("pt-BR").second.app, "the row offers the app's download")
            phoneOut.voices["pt-BR"] = VoiceSupport.READY
            assertEquals(EngineUse(SpeechEngine.PHONE, null, choosable = true), choices.voice("pt-BR").second)
        }

    /** A pack this build doesn't have (no Spanish voice among the test's packs) is no app speech at all. */
    @Test
    fun aPackTheBuildDoesntKnowIsNoAppSpeech() {
        assertEquals(AppSpeechPart.NONE, AppSpeechParts(packs()).voice(LanguageCode("es")))
        assertEquals(AppSpeechPart.NOT_DOWNLOADED, AppSpeechParts(packs()).voice(LanguageCode("pt")))
    }
}
