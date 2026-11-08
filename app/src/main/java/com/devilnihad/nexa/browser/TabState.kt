/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 DEVILNIHAD Ltd
 */

package com.devilnihad.nexa.browser

import org.mozilla.geckoview.GeckoSession

data class TabState(
    val id: String,
    val session: GeckoSession,
    val title: String,
    val url: String,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loadError: String? = null,
    val isEditModeEnabled: Boolean = false,
    val settingsVersion: Int = 0,
    val isUriLoaded: Boolean = true,
    val isIncognito: Boolean = false,
    val lastActiveTime: Long = System.currentTimeMillis(),
    /** True when the GeckoSession has been closed to reclaim memory. The tab
     *  metadata (url, title, history flags) is preserved; the session is
     *  re-created and the page reloaded when the tab is focused again. */
    val isSuspended: Boolean = false,
    /** Optional low-resolution thumbnail captured just before suspension,
     *  used to show a preview in the tab strip while the tab is suspended. */
    val suspendThumbnail: android.graphics.Bitmap? = null,
    /** Serialized GeckoSession state preserved during suspension to restore
     *  exact page state, form inputs, scroll position, and history stack. */
    val savedSessionState: GeckoSession.SessionState? = null,
    /** Monotonic generation identifier for this tab's current GeckoSession.
     *  Used to prevent stale callbacks from a dead session from corrupting
     *  a replacement session after onKill or process death recovery. */
    val sessionGenerationId: Long = 0L,
    /** True when this tab's GeckoSession has been created but not yet opened
     *  on the runtime. Used for lazy tab restoration on cold startup. */
    val isLazy: Boolean = false,
    /** ID of the opener tab that spawned this tab (e.g., auth popups via window.open).
     *  When this tab closes, the browser smoothly switches back to the opener tab. */
    val parentId: String? = null,
    /** Whether the underlying GeckoSession can navigate back in its web history stack. */
    val canGoBackInSession: Boolean = false,
    /** Whether the underlying GeckoSession can navigate forward in its web history stack. */
    val canGoForwardInSession: Boolean = false,
    /** The last real webpage URL loaded in this tab (preserved when visiting Home so Forward can restore it). */
    val lastWebUrl: String? = null,
    /** The title of the last real webpage URL loaded in this tab. */
    val lastWebTitle: String? = null,
    /** Current session history entries for long-press history jump navigation. */
    val sessionHistory: List<SessionHistoryEntry> = emptyList()
)

data class SessionHistoryEntry(
    val index: Int,
    val url: String,
    val title: String
)
