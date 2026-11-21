package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.LanguageNames
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.core.domain.translation.LanguageQuality
import tech.onova.offlinetranslator.feature.toolstranslate.domain.CheckedLanguages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorDefaults
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository

/** One row of the language page: the name in the app's language, its own name, and whether it's basic quality. */
internal data class LanguageRow(
    val code: LanguageCode,
    val name: String,
    val ownName: String,
    val basic: Boolean,
    val checked: Boolean,
    val locked: Boolean,
)

internal data class LanguagesUi(
    val loaded: Boolean = false,
    val query: String = "",
    val rows: List<LanguageRow> = emptyList(),
    /** Only two are checked: they can't be unchecked, and the page says why (FR-005). */
    val atLeast: Boolean = false,
)

/**
 * The language page (spec 080 story 1): every language the Translator offers, by name in the app's language, with a
 * search by either name; a check puts a language in every list, at least [CheckedLanguages.LEAST] stay checked, and a
 * chosen language that gets unchecked is replaced in its list (FR-008).
 */
internal class LanguagesViewModel(
    private val repository: TranslatorSettingsRepository,
    private val names: LanguageNames,
) : ViewModel() {
    private val appLanguage = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<LanguagesUi> =
        combine(repository.settings, appLanguage.filterNotNull(), query) { settings, app, typed ->
            val checked = CheckedLanguages.resolve(settings.languages)
            val locked = CheckedLanguages.locked(checked)
            val rows =
                LanguageCatalog.all
                    .map { language ->
                        LanguageRow(
                            code = language.code,
                            name = names.nameOf(language.tag, app),
                            ownName = names.ownName(language.tag),
                            basic = language.quality == LanguageQuality.BASIC,
                            checked = language.code in checked,
                            locked = language.code in locked,
                        )
                    }.sortedBy { sortable(it.name) }
            val wanted = sortable(typed.trim())
            val shown =
                if (wanted.isEmpty()) {
                    rows
                } else {
                    rows.filter { sortable(it.name).contains(wanted) || sortable(it.ownName).contains(wanted) }
                }
            LanguagesUi(loaded = true, query = typed, rows = shown, atLeast = locked.isNotEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LanguagesUi())

    /** The app's language, as the screen shows it ("pt"): the names' language. */
    fun setAppLanguage(language: String) {
        appLanguage.value = language
    }

    fun search(text: String) {
        query.value = text
    }

    /** Checks or unchecks [language]; a chosen language unchecked is replaced in its list or half (FR-008). */
    fun toggle(language: LanguageCode) {
        val app = appLanguage.value ?: return
        viewModelScope.launch {
            repository.update { stored ->
                val now = TranslatorDefaults.resolve(stored, LanguageCode(app))
                val checked = CheckedLanguages.toggle(now.languages, language)
                if (checked == now.languages) return@update stored
                val byName =
                    checked.sortedBy { code ->
                        sortable(LanguageCatalog.of(code)?.let { names.nameOf(it.tag, app) } ?: code.value)
                    }
                val keep = { chosen: LanguageCode, other: LanguageCode ->
                    if (chosen in checked) chosen else CheckedLanguages.replacementFor(other, byName)
                }
                val source = keep(now.source, now.target)
                val near = keep(now.near, now.far)
                stored.copy(
                    languages = checked,
                    source = source,
                    target = keep(now.target, source),
                    near = near,
                    far = keep(now.far, near),
                )
            }
        }
    }

    /** Puts [language] in the lists, if it isn't there: its pack is being downloaded (owner, 2026-11-20). */
    fun enable(language: LanguageCode) {
        viewModelScope.launch {
            repository.update { stored ->
                val checked = CheckedLanguages.resolve(stored.languages)
                if (language in checked) stored else stored.copy(languages = checked + language)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * A name as it sorts and searches: lower case, without the accents of the app's languages ("Árabe" with the A's).
 * Other scripts are left as they are.
 */
internal fun sortable(name: String): String = name.lowercase().map { ACCENTED[it] ?: it }.joinToString("")

private val ACCENTED: Map<Char, Char> =
    buildMap {
        "áàâãäå".forEach { put(it, 'a') }
        "éèêë".forEach { put(it, 'e') }
        "íìîï".forEach { put(it, 'i') }
        "óòôõö".forEach { put(it, 'o') }
        "úùûü".forEach { put(it, 'u') }
        put('ç', 'c')
        put('ñ', 'n')
    }
