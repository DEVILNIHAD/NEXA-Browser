/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 DEVILNIHAD Ltd
 */

package com.devilnihad.nexa.browser

import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtensionController

internal const val NEXA_SYNC_EXTENSION_ID = "nexa-sync@devilnihad.com"

internal fun BrowserViewModel.installNEXASyncExtension(runtime: GeckoRuntime) {
    runtime.webExtensionController.ensureBuiltIn(
        "resource://android/assets/web_extensions/nexa_sync/",
        NEXA_SYNC_EXTENSION_ID
    ).accept(
        { ext ->
            ext?.let {
                runtime.webExtensionController.setAllowedInPrivateBrowsing(it, true)
                runtime.webExtensionController.enable(it, WebExtensionController.EnableSource.APP)
                Log.i(BrowserViewModel.Companion.TAG, "NEXA Sync pre-installed built-in WebExtension active.")
            }
        },
        { error ->
            Log.e(BrowserViewModel.Companion.TAG, "Failed to load NEXA Sync WebExtension", error)
        }
    )
}
