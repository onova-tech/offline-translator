package tech.onova.offlinetranslator.feature.settings.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.SectionHeader
import tech.onova.offlinetranslator.core.designsystem.components.SettingsRow
import tech.onova.offlinetranslator.core.designsystem.components.ToolFrame
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSizes
import tech.onova.offlinetranslator.core.designsystem.tokens.PocketSpacing
import tech.onova.offlinetranslator.core.toolapi.openedInBrowser
import tech.onova.offlinetranslator.feature.settings.resources.Res
import tech.onova.offlinetranslator.feature.settings.resources.licenses_intro
import tech.onova.offlinetranslator.feature.settings.resources.licenses_none
import tech.onova.offlinetranslator.feature.settings.resources.licenses_other
import tech.onova.offlinetranslator.feature.settings.resources.licenses_read
import tech.onova.offlinetranslator.feature.settings.resources.licenses_source
import tech.onova.offlinetranslator.feature.settings.resources.licenses_title
import tech.onova.offlinetranslator.feature.settings.resources.settings_back

/**
 * Settings → About → Open-source licenses (spec 012 research R4, owner 2026-11-17: "consistent and looks good"): a
 * line of thanks, then every project the app is built with, grouped by author, each with its license; a tap shows the
 * license's full text in a sheet with the project's source link, which opens in the browser like the privacy policy.
 * [readList] gives the build's list (the app's `R.raw.aboutlibraries`).
 */
@Composable
public fun LicensesScreen(
    onBack: () -> Unit,
    readList: suspend () -> String,
    modifier: Modifier = Modifier,
) {
    val groups by produceState<List<AuthorGroup>?>(null, readList) { value = OpenSourceLicenses.parse(readList()) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val other = stringResource(Res.string.licenses_other)
    ToolFrame(
        title = stringResource(Res.string.licenses_title),
        backLabel = stringResource(Res.string.settings_back),
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            verticalArrangement = Arrangement.spacedBy(PocketSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(Res.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.pageWidth(),
                )
            }
            groups.orEmpty().forEach { group ->
                item(key = "author:${group.author}") {
                    SectionHeader(
                        text = group.author.ifEmpty { other },
                        modifier = Modifier.pageWidth().padding(top = PocketSpacing.sm),
                    )
                }
                items(group.projects, key = { "${group.author}/${it.name}" }) { project ->
                    ProjectRow(project, onOpen = { open = "${group.author}/${project.name}" })
                }
            }
        }
    }
    val project =
        open?.let { key ->
            groups.orEmpty().firstNotNullOfOrNull { g -> g.projects.firstOrNull { "${g.author}/${it.name}" == key } }
        }
    project?.let { LicenseSheet(it, onDismiss = { open = null }) }
}

@Composable
private fun ProjectRow(
    project: OpenSourceProject,
    onOpen: () -> Unit,
) {
    val none = stringResource(Res.string.licenses_none)
    SettingsRow(
        title = project.name,
        supporting = project.licenses.joinToString { it.name }.ifEmpty { none },
        onClick = onOpen,
        modifier = Modifier.pageWidth(),
    )
}

/** A project's license, in full, and its source link. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseSheet(
    project: OpenSourceProject,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = PocketSpacing.md, end = PocketSpacing.md, bottom = PocketSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketSpacing.sm),
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            project.website?.let { site ->
                TextButton(onClick = { uriHandler.openedInBrowser(site) }) {
                    Text(stringResource(Res.string.licenses_source))
                }
            }
            project.licenses.forEach { license ->
                Text(text = license.name, style = MaterialTheme.typography.titleSmall)
                if (license.content.isNotEmpty()) {
                    SelectionContainer { Text(text = license.content, style = MaterialTheme.typography.bodySmall) }
                } else {
                    // A standard license the build names without its text (offline): its page in the browser.
                    license.url?.let { url ->
                        TextButton(onClick = { uriHandler.openedInBrowser(url) }) {
                            Text(stringResource(Res.string.licenses_read))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.pageWidth(): Modifier =
    fillMaxWidth().widthIn(max = PocketSizes.contentMaxWidth).padding(horizontal = PocketSpacing.md)
