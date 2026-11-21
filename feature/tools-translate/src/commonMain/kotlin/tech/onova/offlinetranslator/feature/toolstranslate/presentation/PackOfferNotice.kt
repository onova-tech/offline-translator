package tech.onova.offlinetranslator.feature.toolstranslate.presentation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.onova.offlinetranslator.core.designsystem.components.Notice
import tech.onova.offlinetranslator.core.domain.ports.SettingsPage
import tech.onova.offlinetranslator.feature.toolstranslate.domain.PackOffer
import tech.onova.offlinetranslator.feature.toolstranslate.domain.Routes
import tech.onova.offlinetranslator.feature.toolstranslate.resources.Res
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_cancel
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_confirm
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_download
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_download_failed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_downloading
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_installing
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_network_settings
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_space
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_no_store
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_not_now
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_checking
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_needed
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_pack_needed_no_size
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_storage_settings
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_try_again
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_unsupported
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_waiting_confirmation
import tech.onova.offlinetranslator.feature.toolstranslate.resources.translator_waiting_wifi

/**
 * The pack a pair needs, in the translation's place (spec 012 story 2, contracts/ui.md "The pack offer"): its size
 * with Not now and Download, nothing fetched before; then the progress with Cancel, or what stops it with what fixes
 * it.
 */
@Composable
internal fun PackOfferNotice(
    offer: PackOffer,
    actions: PackActions,
    onNotNow: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val language = Routes.languageOf(offer.pack)?.let { languageName(it) } ?: offer.pack.value
    val pack = offer.pack
    when (offer) {
        is PackOffer.Checking -> {
            Notice(text = stringResource(Res.string.translator_pack_checking), modifier = modifier)
        }

        is PackOffer.Offer -> {
            val text =
                offer.megabytes?.let { stringResource(Res.string.translator_pack_needed, language, it.toString()) }
                    ?: stringResource(Res.string.translator_pack_needed_no_size, language)
            Notice(text = text, modifier = modifier) {
                onNotNow?.let { Action(Res.string.translator_not_now, it) }
                Action(Res.string.translator_download) { actions.onDownload(pack) }
            }
        }

        is PackOffer.Downloading -> {
            val done = offer.doneMegabytes.toString()
            val total = offer.totalMegabytes.toString()
            Notice(
                text = stringResource(Res.string.translator_downloading, language, done, total),
                modifier = modifier,
                extra = {
                    LinearProgressIndicator(
                        progress = { offer.percent / PERCENT },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            ) {
                Action(Res.string.translator_cancel) { actions.onCancel(pack) }
            }
        }

        is PackOffer.Installing -> {
            Notice(
                text = stringResource(Res.string.translator_installing, language),
                modifier = modifier,
                extra = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            )
        }

        else -> {
            StoppedOffer(offer, language, actions, modifier)
        }
    }
}

/** A download that waits, failed, or can't happen here: what happened, and what the person can do about it. */
@Composable
private fun StoppedOffer(
    offer: PackOffer,
    language: String,
    actions: PackActions,
    modifier: Modifier = Modifier,
) {
    val retry: @Composable RowScope.() -> Unit = {
        Action(Res.string.translator_try_again) { actions.onDownload(offer.pack) }
    }
    when (offer) {
        is PackOffer.WaitingForWifi -> {
            Notice(text = stringResource(Res.string.translator_waiting_wifi, language), modifier = modifier) {
                Action(Res.string.translator_network_settings) { actions.onOpen(SettingsPage.NETWORK) }
                retry()
            }
        }

        // The button opens Google Play's own dialog (PacksViewModel.download).
        is PackOffer.WaitingForConfirmation -> {
            Notice(text = stringResource(Res.string.translator_waiting_confirmation, language), modifier = modifier) {
                Action(Res.string.translator_confirm) { actions.onDownload(offer.pack) }
            }
        }

        is PackOffer.NoSpace -> {
            val needed = offer.neededMegabytes.toString()
            Notice(text = stringResource(Res.string.translator_no_space, language, needed), modifier = modifier) {
                Action(Res.string.translator_storage_settings) { actions.onOpen(SettingsPage.STORAGE) }
                retry()
            }
        }

        is PackOffer.Failed -> {
            Notice(
                text = stringResource(Res.string.translator_download_failed, language),
                modifier = modifier,
                actions = retry,
            )
        }

        is PackOffer.NoStore -> {
            Notice(text = stringResource(Res.string.translator_no_store), modifier = modifier)
        }

        else -> {
            Notice(text = stringResource(Res.string.translator_unsupported), modifier = modifier)
        }
    }
}

@Composable
private fun Action(
    label: StringResource,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) { Text(stringResource(label)) }
}

private const val PERCENT = 100f
