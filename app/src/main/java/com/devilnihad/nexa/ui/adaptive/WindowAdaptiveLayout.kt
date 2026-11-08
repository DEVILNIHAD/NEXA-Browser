/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 DEVILNIHAD Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.devilnihad.nexa.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.roundToInt

/**
 * Width size classes following the official AndroidX Window Size Class model with
 * the large & extra-large buckets enabled (the layout equivalent of
 * `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)`):
 *
 *  - [COMPACT]     < 600dp   (most phones in portrait)
 *  - [MEDIUM]      600–839dp (small tablets / large phones in landscape / split screen)
 *  - [EXPANDED]    840–1199dp (tablet portrait / landscape phones on foldables)
 *  - [LARGE]       1200–1599dp (tablet landscape / desktop windows)
 *  - [EXTRA_LARGE] >= 1600dp (ChromeOS / large desktop windows)
 *
 * The project's Compose BOM (2024.09.03 / Material3 1.3.0) ships
 * `material3-adaptive` 1.0.x, whose [androidx.window.core.layout.WindowSizeClass]
 * only knows the three legacy buckets and cannot express Large/ExtraLarge. Rather
 * than forcing a risky Compose platform upgrade, this enum re-implements the exact
 * official breakpoints on top of the live window size, which additionally gives us
 * continuous updates in freeform/split-screen resizes.
 */
enum class AdaptiveWidthClass {
    COMPACT, MEDIUM, EXPANDED, LARGE, EXTRA_LARGE;

    companion object {
        /** Official width breakpoints: 600 / 840 / 1200 / 1600 dp. */
        fun fromWidthDp(widthDp: Int): AdaptiveWidthClass = when {
            widthDp < 600 -> COMPACT
            widthDp < 840 -> MEDIUM
            widthDp < 1200 -> EXPANDED
            widthDp < 1600 -> LARGE
            else -> EXTRA_LARGE
        }
    }
}

/**
 * Height size classes following the official AndroidX Window Size Class model:
 *
 *  - [COMPACT]  < 480dp (landscape phones, short freeform windows)
 *  - [MEDIUM]   480–899dp
 *  - [EXPANDED] >= 900dp (tablets in portrait, desktop windows)
 */
enum class AdaptiveHeightClass {
    COMPACT, MEDIUM, EXPANDED;

    companion object {
        /** Official height breakpoints: 480 / 900 dp. */
        fun fromHeightDp(heightDp: Int): AdaptiveHeightClass = when {
            heightDp < 480 -> COMPACT
            heightDp < 900 -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * Immutable snapshot of the current window's adaptive layout state. Screens read
 * this instead of raw `configuration.screenWidthDp` so that every breakpoint
 * decision lives in one place.
 */
@Immutable
data class WindowAdaptiveLayout(
    val widthDp: Int,
    val heightDp: Int,
    val widthClass: AdaptiveWidthClass,
    val heightClass: AdaptiveHeightClass,
) {
    // ── Width convenience flags ──────────────────────────────────────────────────
    val isCompactWidth: Boolean get() = widthClass == AdaptiveWidthClass.COMPACT
    val isMediumWidth: Boolean get() = widthClass == AdaptiveWidthClass.MEDIUM
    val isExpandedWidth: Boolean get() = widthClass == AdaptiveWidthClass.EXPANDED
    val atLeastMedium: Boolean get() = widthClass >= AdaptiveWidthClass.MEDIUM
    val atLeastExpanded: Boolean get() = widthClass >= AdaptiveWidthClass.EXPANDED
    val atLeastLarge: Boolean get() = widthClass >= AdaptiveWidthClass.LARGE
    val atLeastExtraLarge: Boolean get() = widthClass >= AdaptiveWidthClass.EXTRA_LARGE

    // ── Height convenience flags ─────────────────────────────────────────────────
    val isCompactHeight: Boolean get() = heightClass == AdaptiveHeightClass.COMPACT
    val atLeastMediumHeight: Boolean get() = heightClass >= AdaptiveHeightClass.MEDIUM
    val atLeastExpandedHeight: Boolean get() = heightClass >= AdaptiveHeightClass.EXPANDED

    // ── App-level adaptive decisions ─────────────────────────────────────────────

    /** ≥600dp: show the desktop-class chrome (tab strip + horizontal toolbar). */
    val useTabletChrome: Boolean get() = atLeastMedium

    /**
     * ≥840dp: replace the phone bottom bar with a persistent navigation rail.
     * Never width alone guarantees a two-pane split is comfortable — callers that
     * actually show side-by-side panes must additionally gate on [supportsTwoPane].
     */
    val useNavigationRail: Boolean get() = atLeastExpanded

    /** ≥1200dp: rail grows into an expanded, desktop-class rail. */
    val useExpandedRail: Boolean get() = atLeastLarge

    /**
     * Two-pane/list-detail layouts need horizontal room AND enough height to be
     * usable. A 900×420 freeform window is wide but a pane split would be cramped.
     */
    val supportsTwoPane: Boolean get() = atLeastExpanded && !isCompactHeight
}

/**
 * Returns the current window's [WindowAdaptiveLayout], recomposed whenever the
 * window is resized (rotation, split-screen drag, freeform resize, fold/unfold).
 *
 * The size comes from the live Compose root container size, which tracks the real
 * window continuously; `LocalConfiguration` is only a fallback for the brief
 * moment before first layout. Note: `containerSize` must be read directly in the
 * composition scope (not inside `remember`) for resize recomposition to work.
 */
@Composable
fun rememberWindowAdaptiveLayout(): WindowAdaptiveLayout {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    // Direct state read — recomposes on every window resize.
    val containerSize = LocalWindowInfo.current.containerSize

    return remember(containerSize, density, configuration) {
        val widthPx = containerSize.width
        val heightPx = containerSize.height
        val configW = configuration.screenWidthDp
        val configH = configuration.screenHeightDp

        // Live container metrics can momentarily disagree with the system
        // configuration (e.g. a density-only change lands while the resized
        // frame is already reported). Drawing then happens at the stale pair,
        // so a container-derived dp computed with the old density is wrong.
        // When the two sources diverge materially, trust the system pair —
        // it is always self-consistent — and keep live tracking otherwise.
        // Threshold is deliberately coarse (25%): normal rounding between the
        // two sources is a few dp, while the stale-density signature this guards
        // against is a ~40-50% mismatch. A tight threshold would flip the size
        // class on exact breakpoint boundaries like 600dp.
        fun reconcile(liveDp: Int, configDp: Int): Int = when {
            configDp <= 0 -> liveDp
            liveDp <= 0 -> configDp
            kotlin.math.abs(liveDp - configDp) * 100 > configDp * 25 -> configDp
            else -> liveDp
        }

        val widthDp: Int
        val heightDp: Int
        if (widthPx > 0 && heightPx > 0) {
            widthDp = reconcile(with(density) { widthPx.toDp().value.roundToInt() }, configW)
            heightDp = reconcile(with(density) { heightPx.toDp().value.roundToInt() }, configH)
        } else {
            widthDp = configW
            heightDp = configH
        }
        WindowAdaptiveLayout(
            widthDp = widthDp,
            heightDp = heightDp,
            widthClass = AdaptiveWidthClass.fromWidthDp(widthDp),
            heightClass = AdaptiveHeightClass.fromHeightDp(heightDp),
        )
    }
}
