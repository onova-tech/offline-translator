package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.ContentPacks
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.ports.PackWait
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.core.domain.ports.SettingsPages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.CheckedLanguages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository

internal data class PacksUi(
    val wifiOnly: Boolean = true,
    val packs: List<PackStatus> = emptyList(),
    /** The checked languages (spec 080): Settings lists their packs, and any other pack that takes space. */
    val languages: List<LanguageCode> = CheckedLanguages.DEFAULT,
) {
    /**
     * Settings' rows (spec 080 FR-010): the checked languages' packs, then every other pack that takes or is getting
     * space, each group in the catalog's order. English has no pack.
     */
    val shownPacks: List<PackStatus>
        get() {
            val checked = packs.filter { Routes.languageOf(it.id) in languages }
            val others = packs.filter { it !in checked && it.state.takesSpace() }
            return checked + others
        }
}

private fun PackState.takesSpace(): Boolean =
    this is PackState.Installed || this is PackState.Downloading || this == PackState.Installing ||
        this is PackState.Waiting

/**
 * The language packs' actions (spec 012 stories 2 and 3): a download starts only from [download], which the person
 * confirmed with the pack's size in front of them, and follows the "Download only on Wi-Fi" switch (FR-010, FR-011).
 * The pack offer and Settings → Offline languages share it.
 */
internal class PacksViewModel(
    private val packs: ContentPacks,
    private val repository: TranslatorSettingsRepository,
    private val pages: SettingsPages,
) : ViewModel() {
    val uiState: StateFlow<PacksUi> =
        combine(repository.settings, packs.packs) { settings, statuses ->
            PacksUi(settings.wifiOnly, statuses, CheckedLanguages.resolve(settings.languages))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PacksUi())

    /** Asks the store for the packs' sizes and states; nothing is downloaded. */
    fun refresh() {
        viewModelScope.launch { packs.refresh() }
    }

    /** A pack Google Play holds for a confirmation gets its dialog instead: fetching again would wait again. */
    fun download(pack: PackId) {
        viewModelScope.launch {
            val state =
                packs.packs.value
                    .firstOrNull { it.id == pack }
                    ?.state
            if (state == PackState.Waiting(PackWait.FOR_CONFIRMATION)) {
                packs.confirm(pack)
            } else {
                packs.download(pack, wifiOnly = repository.settings.first().wifiOnly)
            }
        }
    }

    /** Stops a download; the store leaves nothing behind (FR-012). */
    fun cancel(pack: PackId) {
        viewModelScope.launch { packs.cancel(pack) }
    }

    fun remove(pack: PackId) {
        viewModelScope.launch { packs.remove(pack) }
    }

    fun setWifiOnly(on: Boolean) {
        viewModelScope.launch { repository.update { it.copy(wifiOnly = on) } }
    }

    fun open(page: SettingsPage) = pages.open(page)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
