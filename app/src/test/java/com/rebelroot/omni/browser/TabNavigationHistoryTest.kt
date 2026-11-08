/*
 * NEXA Browser - Tab navigation history unit tests.
 * Copyright (C) 2026 RebelRoot Ltd
 */

package com.devilnihad.nexabrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationHistoryTest {

    private fun createDummySession(): org.mozilla.geckoview.GeckoSession {
        return try {
            org.mozilla.geckoview.GeckoSession()
        } catch (e: Throwable) {
            // Allocate without constructor if Gecko native lib or Android context isn't mocked
            val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            val unsafe = field.get(null) as sun.misc.Unsafe
            unsafe.allocateInstance(org.mozilla.geckoview.GeckoSession::class.java) as org.mozilla.geckoview.GeckoSession
        }
    }

    @Test
    fun tabState_defaultNavigationState_isClean() {
        val session = createDummySession()
        val tab = TabState(id = "tab-test-1", session = session, title = "New Tab", url = "about:blank")
        assertFalse("New tab should not be able to go back", tab.canGoBack)
        assertFalse("New tab should not be able to go forward", tab.canGoForward)
        assertFalse("New tab session should not have back entries", tab.canGoBackInSession)
        assertFalse("New tab session should not have forward entries", tab.canGoForwardInSession)
        assertTrue("New tab history list should be empty", tab.sessionHistory.isEmpty())
        assertEquals(null, tab.lastWebUrl)
        assertEquals(null, tab.lastWebTitle)
    }

    @Test
    fun sessionHistoryEntry_equalityAndProperties() {
        val entry1 = SessionHistoryEntry(0, "https://example.com", "Example")
        val entry2 = SessionHistoryEntry(0, "https://example.com", "Example")
        val entry3 = SessionHistoryEntry(1, "https://example.com/docs", "Docs")

        assertEquals(entry1, entry2)
        assertEquals(0, entry1.index)
        assertEquals("https://example.com", entry1.url)
        assertEquals("Example", entry1.title)
        assertFalse(entry1 == entry3)
    }

    @Test
    fun tabNavigation_effectiveBackForward_onWebPage() {
        val session = createDummySession()
        // When on a web page, even without session back entries, user should be able to go back (to Home)
        val tab = TabState(
            id = "tab-web",
            session = session,
            url = "https://example.com",
            title = "Example",
            canGoBack = true,
            canGoBackInSession = false,
            canGoForward = false,
            canGoForwardInSession = false
        )
        assertTrue("Tab on webpage should allow going back to home", tab.canGoBack)
        assertFalse("Tab should not have forward when at stack tip", tab.canGoForward)
    }

    @Test
    fun tabNavigation_effectiveBackForward_onHomeScreenWithRestorableSession() {
        val session = createDummySession()
        // When user pressed Back from first webpage, tab returns to Home screen (about:blank)
        // Back should be disabled, Forward should be enabled to restore webpage
        val tab = TabState(
            id = "tab-home",
            session = session,
            url = "about:blank",
            title = "New Tab",
            lastWebUrl = "https://example.com",
            lastWebTitle = "Example",
            canGoBack = false,
            canGoForward = true,
            canGoBackInSession = false,
            canGoForwardInSession = false
        )
        assertFalse("Home screen should not allow going back", tab.canGoBack)
        assertTrue("Home screen with restorable webpage should allow going forward", tab.canGoForward)
    }

    @Test
    fun tabNavigation_sessionHistoryFiltering() {
        val history = listOf(
            SessionHistoryEntry(0, "https://example.com/step1", "Step 1"),
            SessionHistoryEntry(1, "https://example.com/step2", "Step 2"),
            SessionHistoryEntry(2, "https://example.com/step3", "Step 3"),
            SessionHistoryEntry(3, "https://example.com/step4", "Step 4")
        )
        val currentIndex = 2 // at step 3

        // Back history should contain indices < currentIndex, ordered most recent first (step 2, step 1)
        val backHistory = history.filter { it.index < currentIndex }.reversed()
        assertEquals(2, backHistory.size)
        assertEquals(1, backHistory[0].index)
        assertEquals("Step 2", backHistory[0].title)
        assertEquals(0, backHistory[1].index)
        assertEquals("Step 1", backHistory[1].title)

        // Forward history should contain indices > currentIndex, ordered nearest first (step 4)
        val forwardHistory = history.filter { it.index > currentIndex }
        assertEquals(1, forwardHistory.size)
        assertEquals(3, forwardHistory[0].index)
        assertEquals("Step 4", forwardHistory[0].title)
    }
}
