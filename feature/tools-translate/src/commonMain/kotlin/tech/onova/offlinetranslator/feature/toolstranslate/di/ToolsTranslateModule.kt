package tech.onova.offlinetranslator.feature.toolstranslate.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.onova.offlinetranslator.core.data.backup.DataPart
import tech.onova.offlinetranslator.core.domain.ports.MonotonicClock
import tech.onova.offlinetranslator.core.domain.ports.SpeechInput
import tech.onova.offlinetranslator.core.domain.ports.SpeechOutput
import tech.onova.offlinetranslator.core.toolapi.ToolEntry
import tech.onova.offlinetranslator.feature.toolstranslate.TranslatorEntry
import tech.onova.offlinetranslator.feature.toolstranslate.data.DataStoreTranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.AppSpeechParts
import tech.onova.offlinetranslator.feature.toolstranslate.domain.EngineSession
import tech.onova.offlinetranslator.feature.toolstranslate.domain.RoutedSpeechInput
import tech.onova.offlinetranslator.feature.toolstranslate.domain.RoutedSpeechOutput
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechChoices
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechDownloads
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.ConversationSpeech
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.ConversationViewModel
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.LanguagesViewModel
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.PacksViewModel
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.SpeakAndHearViewModel
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.SpeechPartsViewModel
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.TranslatorOutputs
import tech.onova.offlinetranslator.feature.toolstranslate.presentation.TranslatorViewModel

/**
 * The Translator's Koin bindings (spec 012). The packs, the engine and the speech are ports bound in `androidApp`'s
 * AppModule; the settings live here, and they go into a data file (spec 010) without the packs. One [EngineSession]
 * for the process: both modes' screens share its models.
 */
public val toolsTranslateModule =
    module {
        single { DataStoreTranslatorSettings(get()) } bind TranslatorSettingsRepository::class
        // Spec 082: the phone's speech or the app's, per language; the app binds both engines by name.
        single { AppSpeechParts(get()) }
        single { RoutedSpeechInput(get(named("phone")), get(named("app")), get(), get(), get()) }
        single<SpeechInput> { get<RoutedSpeechInput>() }
        single { RoutedSpeechOutput(get(named("phone")), get(named("app")), get(), get()) }
        single<SpeechOutput> { get<RoutedSpeechOutput>() }
        single { SpeechChoices(get(), get(), get()) }
        single<DataPart>(named("data-part:translator")) { get<DataStoreTranslatorSettings>().dataPart() }
        single<ToolEntry>(named(TranslatorEntry.ID)) { TranslatorEntry() }
        single { EngineSession(get(), get()) }
        factory { TranslatorOutputs(get()) }
        viewModelOf(::TranslatorViewModel)
        viewModelOf(::PacksViewModel)
        viewModelOf(::SpeakAndHearViewModel)
        // One for the process: the Manage languages page and both modes agree on a language asked for (2026-11-20).
        single { SpeechDownloads { get<MonotonicClock>().now().elapsedMillis } }
        factory { ConversationSpeech(get(), get(), get(), get(), get()) }
        viewModelOf(::ConversationViewModel)
        viewModelOf(::LanguagesViewModel)
        viewModelOf(::SpeechPartsViewModel)
    }
