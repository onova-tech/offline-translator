package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.onova.offlinetranslator.core.designsystem.components.ConfirmScreen
import tech.onova.offlinetranslator.core.designsystem.components.IconAction
import tech.onova.offlinetranslator.core.designsystem.components.MainReading
import tech.onova.offlinetranslator.core.designsystem.components.NO_READING
import tech.onova.offlinetranslator.core.designsystem.components.ReservedSlot
import tech.onova.offlinetranslator.core.designsystem.components.SearchField
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.ShortNote
import tech.onova.offlinetranslator.core.designsystem.components.ToolLayout
import tech.onova.offlinetranslator.core.designsystem.icons.PocketIcons
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.ports.PackId
import tech.onova.offlinetranslator.core.domain.ports.PackState
import tech.onova.offlinetranslator.core.domain.ports.PackStatus
import tech.onova.offlinetranslator.core.domain.speech.SpeechCatalog
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog
import tech.onova.offlinetranslator.core.toolapi.HelpSheet
import tech.onova.offlinetranslator.core.toolapi.resources.host_help
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackMessages
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_cancel
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete_text
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_delete_title
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_downloaded_languages
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_back
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_clear
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_none
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_search
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_title
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_languages_two
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_more_languages
import tech.onova.offlinetranslator.core.toolapi.resources.Res as HostRes

/**
 * Manage languages (spec 080 story 4, owner 2026-11-20, contracts/ui.md): over the tool, back, its title and a search
 * on top; "Your languages" (the checked ones and any whose pack is on the phone), each with its check, its translation
 * pack, the phone's speech recognition and its voice; then "Other languages" to check. Nothing at the bottom: a change
 * applies at once. [focus] opens it at that language (from a speech popup).
 */
@Composable
internal fun ManageLanguagesPage(
    focus: LanguageCode?,
    onClose: () -> Unit,
    languages: LanguagesViewModel = koinViewModel(),
    packs: PacksViewModel = koinViewModel(),
    speech: SpeechPartsViewModel = koinViewModel(),
) {
    val ui by languages.uiState.collectAsStateWithLifecycle()
    val packUi by packs.uiState.collectAsStateWithLifecycle()
    val speechUi by speech.uiState.collectAsStateWithLifecycle()
    val appLanguage = Locale.current.language
    val phoneLocales = LocaleList.current.localeList.map { it.toLanguageTag() }
    LaunchedEffect(appLanguage) { languages.setAppLanguage(appLanguage) }
    LaunchedEffect(Unit) { packs.refresh() }
    val known = packUi.packs.mapNotNull { status -> Routes.languageOf(status.id)?.let { it to status } }.toMap()
    val downloaded = rememberDownloaded(packUi.packs)
    // Opened from a speech popup, its language is open (owner, 2026-11-20).
    var expanded by rememberSaveable { mutableStateOf(listOfNotNull(focus?.value)) }
    val asked = downloaded + expanded.map(::LanguageCode)
    LaunchedEffect(asked, phoneLocales) { speech.show(asked.toList(), phoneLocales) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var helping by rememberSaveable { mutableStateOf(false) }
    val actions = partActions(languages, packs, speech) { deleting = it.value }
    val sections =
        Sections(downloaded, expanded.toSet(), focus) { code, open ->
            expanded = if (open) expanded + code.value else expanded - code.value
        }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            ToolLayout(
                top = { PageTop(ui.query, languages::search, onClose, onHelp = { helping = true }) },
                scrollContent = false,
            ) {
                // Its room is kept, so the list doesn't jump when the last two lock (M1–M5).
                ReservedSlot(shown = ui.atLeast, keepInSmallWindow = true) {
                    ShortNote(
                        text = stringResource(Res.string.translator_languages_two),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (ui.loaded && ui.rows.isEmpty()) {
                    MainReading(value = NO_READING, caption = stringResource(Res.string.translator_languages_none))
                } else {
                    val byId = packUi.packs.associateBy { it.id }
                    val state = { row: LanguageRow ->
                        LanguageState(
                            packOf(known, row.code),
                            speechUi.parts[row.code],
                            speechUi.serviceUpdatable,
                            appRecognition =
                                byId[SpeechCatalog.recognitionPack].takeIf {
                                    row.code in
                                        SpeechCatalog.recognition
                                },
                            appVoice =
                                SpeechCatalog.voices[row.code]
                                    ?.let { listOfNotNull(byId[it.pack], byId[SpeechCatalog.voiceDataPack]) }
                                    .orEmpty(),
                        )
                    }
                    LanguageList(ui, sections, state, actions, Modifier.weight(1f))
                }
            }
        }
        deleting?.let { id ->
            val bytes = packUi.packs.firstOrNull { it.id.value == id }?.installedBytes
            DeleteConfirmation(PackId(id), bytes, onDelete = packs::remove) { deleting = null }
        }
        if (helping) HelpSheet(onClose = { helping = false }) { ManageLanguagesHelp() }
    }
}

/** What the page's rows do; downloading a language's pack puts it in the lists (owner, 2026-11-20). */
private fun partActions(
    languages: LanguagesViewModel,
    packs: PacksViewModel,
    speech: SpeechPartsViewModel,
    onDelete: (PackId) -> Unit,
): PartActions =
    PartActions(
        onToggle = languages::toggle,
        pack =
            PackPartActions(
                onDownload = { pack ->
                    packs.download(pack)
                    Routes.languageOf(pack)?.let(languages::enable)
                },
                onCancel = packs::cancel,
                onDelete = onDelete,
            ),
        speech = SpeechPartActions(speech::get, speech::update, speech::voice),
    )

/** Where each language goes, which are open, and where the page opens. */
private class Sections(
    val downloaded: Set<LanguageCode>,
    val expanded: Set<String>,
    val focus: LanguageCode?,
    val onExpand: (LanguageCode, Boolean) -> Unit,
)

/**
 * The languages downloaded when the page opened, English (built in) among them: one downloaded or deleted meanwhile
 * keeps its place, so the person doesn't have to find it again (owner, 2026-11-20). Taken once the store has answered.
 */
@Composable
private fun rememberDownloaded(packs: List<PackStatus>): Set<LanguageCode> {
    val now =
        (
            packs.filter { it.state == PackState.Installed }.mapNotNull {
                Routes.languageOf(
                    it.id,
                )
            } + BUILT_IN
        ).toSet()
    var frozen by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val answered = packs.isNotEmpty() && packs.none { it.state == PackState.Unknown }
    LaunchedEffect(answered) { if (answered && frozen == null) frozen = now.map { it.value } }
    return frozen?.map(::LanguageCode)?.toSet() ?: now
}

private val BUILT_IN: List<LanguageCode> = LanguageCatalog.all.filter { it.pack == null }.map { it.code }

/** English has no pack (built in); another language the store hasn't answered for yet reads as being checked. */
private fun packOf(
    known: Map<LanguageCode, PackStatus>,
    code: LanguageCode,
): PackStatus? = known[code] ?: LanguageCatalog.of(code)?.pack?.let { PackStatus(it, null, null, PackState.Unknown) }

/** "Downloaded languages", then "Other languages", each a card that opens; the only part that scrolls. */
@Composable
private fun LanguageList(
    ui: LanguagesUi,
    sections: Sections,
    state: (LanguageRow) -> LanguageState,
    actions: PartActions,
    modifier: Modifier = Modifier,
) {
    val (mine, others) = ui.rows.partition { it.code in sections.downloaded }
    val keys =
        (if (mine.isEmpty()) emptyList() else listOf(HEADER_MINE) + mine.map { it.code.value }) +
            (if (others.isEmpty()) emptyList() else listOf(HEADER_OTHERS) + others.map { it.code.value })
    val list = rememberLazyListState()
    ScrollToStart(list, ui.query, focusIndex = sections.focus?.let { keys.indexOf(it.value) }?.takeIf { it >= 0 })
    val card: @Composable (LanguageRow) -> Unit = { row ->
        LanguageCard(
            row = row,
            state = state(row),
            expanded = row.code.value in sections.expanded,
            onExpandedChange = { sections.onExpand(row.code, it) },
            actions = actions,
        )
    }
    LazyColumn(
        state = list,
        verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (mine.isNotEmpty()) {
            item(key = HEADER_MINE) { SectionHeader(text = stringResource(Res.string.translator_downloaded_languages)) }
            items(mine, key = { it.code.value }) { card(it) }
        }
        if (others.isNotEmpty()) {
            item(key = HEADER_OTHERS) { SectionHeader(text = stringResource(Res.string.translator_more_languages)) }
            items(others, key = { it.code.value }) { card(it) }
        }
    }
}

@Composable
private fun PageTop(
    query: String,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    onHelp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(
                icon = PocketIcons.Back,
                label = stringResource(Res.string.translator_languages_back),
                onClick = onClose,
            )
            Text(
                text = stringResource(Res.string.translator_languages_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconAction(icon = PocketIcons.Info, label = stringResource(HostRes.string.host_help), onClick = onHelp)
        }
        SearchField(
            query = query,
            onQueryChange = onSearch,
            placeholder = stringResource(Res.string.translator_languages_search),
            clearLabel = stringResource(Res.string.translator_languages_clear),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Opens at [focusIndex] (a speech popup's language) the first time; afterwards each new search starts at the top,
 * rather than where the last one's first match was.
 */
@Composable
private fun ScrollToStart(
    list: LazyListState,
    query: String,
    focusIndex: Int?,
) {
    var focused by remember { mutableStateOf(false) }
    // Keyed on whether there's a place to open at, not on its index: a pack arriving doesn't move the list.
    LaunchedEffect(query, focusIndex != null) {
        val target = focusIndex?.takeIf { !focused } ?: 0
        if (focusIndex != null) focused = true
        list.scrollToItem(target)
    }
}

/** Delete asks first, with the space it frees (spec 012 FR-012). */
@Composable
private fun DeleteConfirmation(
    pack: PackId,
    bytes: Long?,
    onDelete: (PackId) -> Unit,
    onDone: () -> Unit,
) {
    val name = packName(pack)
    ConfirmScreen(
        title = stringResource(Res.string.translator_delete_title, name),
        text = stringResource(Res.string.translator_delete_text, PackMessages.megabytes(bytes ?: 0).toString()),
        confirmLabel = stringResource(Res.string.translator_delete),
        cancelLabel = stringResource(Res.string.translator_cancel),
        onConfirm = {
            onDone()
            onDelete(pack)
        },
        onCancel = onDone,
    )
}

private const val HEADER_MINE = "header-yours"
private const val HEADER_OTHERS = "header-others"
