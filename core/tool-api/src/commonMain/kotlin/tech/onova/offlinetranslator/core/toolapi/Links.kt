package tech.onova.offlinetranslator.core.toolapi

import androidx.compose.ui.platform.UriHandler

/**
 * Hands [uri] to the phone's browser; false when no app takes it. The app itself opens no connection (constitution
 * Principle I): the browser does, once the person asked.
 */
@Suppress("SwallowedException") // the caller says what happened instead
public fun UriHandler.openedInBrowser(uri: String): Boolean =
    try {
        openUri(uri)
        true
    } catch (noBrowser: IllegalArgumentException) {
        false
    }
