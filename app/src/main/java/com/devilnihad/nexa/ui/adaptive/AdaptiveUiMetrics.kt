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
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized adaptive UI metrics for NEXA Browser. Every dimension a screen needs
 * to lay itself out on large screens comes from here — screens must not scatter
 * new tablet-specific constants. All values are derived from the current
 * [WindowAdaptiveLayout]; the optional user scale only affects glyph/chrome
 * heights, never structural widths.
 */
@Immutable
data class AdaptiveUiMetrics(
    val layout: WindowAdaptiveLayout,

    // ── Chrome (browser toolbar / tab strip) ─────────────────────────────────────
    /** Outer horizontal gutter for chrome rows on medium+ widths. */
    val chromeHorizontalPadding: Dp,
    /** Spacing between logical control groups (nav cluster ↔ address field ↔ actions). */
    val chromeGroupSpacing: Dp,
    /** Height of the tab strip row. */
    val tabStripHeight: Dp,
    /** Height of a single tab inside the strip. */
    val tabHeight: Dp,
    /** Smallest usable tab width before the strip must scroll. */
    val tabMinWidth: Dp,
    /** Widest a tab may grow before extra space goes to gutters. */
    val tabMaxWidth: Dp,
    /** Horizontal gap between neighbouring tabs. */
    val tabSpacing: Dp,
    /** Favicon glyph size inside a tab. */
    val tabFaviconSize: Dp,
    /** Hit-target width of a tab's close button (full tab height, centered glyph). */
    val tabCloseTargetWidth: Dp,
    /** Height of the address/toolbar row. */
    val toolbarHeight: Dp,
    /** Icon glyph size inside toolbar buttons. */
    val toolbarIconSize: Dp,
    /** Square touch target for toolbar icon buttons (≥44dp). */
    val toolbarTouchTarget: Dp,
    /** Height of the address field pill. */
    val addressFieldHeight: Dp,
    /** Address field never shrinks below this, even when actions crowd it. */
    val addressFieldMinWidth: Dp,
    /** Fixed width reserved for the Cancel button in focused mode. */
    val cancelActionWidth: Dp,

    // ── Navigation ───────────────────────────────────────────────────────────────
    /** Height of the phone-style bottom bar (compact/medium). */
    val navigationBarHeight: Dp,
    /** Bottom bar content never stretches wider than this. */
    val navigationBarMaxWidth: Dp,
    /** Square touch target for bottom bar items (≥48dp). */
    val navigationBarTouchTarget: Dp,
    /** Width of the persistent navigation rail (expanded+). */
    val navigationRailWidth: Dp,

    // ── Surfaces & content ───────────────────────────────────────────────────────
    /** Max width for bottom-sheet/dialog content before it looks absurd. */
    val sheetMaxWidth: Dp,
    /** Max width for a standard full-screen content column (lists, tools). */
    val screenContentMaxWidth: Dp,
    /** Max readable width for settings content. */
    val settingsContentMaxWidth: Dp,
    /** Width of the settings category pane (list-detail). */
    val settingsPaneWidth: Dp,
    /** Browser home content max width (aligned with existing HomeScreenContent cap). */
    val homeContentMaxWidth: Dp,
) {
    val widthClass: AdaptiveWidthClass get() = layout.widthClass
    val heightClass: AdaptiveHeightClass get() = layout.heightClass
    val atLeastMedium: Boolean get() = layout.atLeastMedium
    val atLeastExpanded: Boolean get() = layout.atLeastExpanded
    val atLeastLarge: Boolean get() = layout.atLeastLarge
}

/**
 * Builds the adaptive metric set for the current window. `userScale` is NEXA's
 * user chrome scale (0.75–1.15); it scales interactive chrome heights/sizes only,
 * never breakpoint structure, so layouts stay stable across scale changes.
 */
fun adaptiveUiMetrics(
    layout: WindowAdaptiveLayout,
    userScale: Float = 1f,
): AdaptiveUiMetrics {
    // Damp the user scale on large screens so chrome doesn't bloat; layout
    // geometry (widths, panes) is intentionally scale-independent.
    val s = userScale.coerceIn(0.85f, 1.12f)
    return when (layout.widthClass) {
        AdaptiveWidthClass.COMPACT -> AdaptiveUiMetrics(
            layout = layout,
            chromeHorizontalPadding = 8.dp,
            chromeGroupSpacing = 8.dp,
            tabStripHeight = 56.dp,
            tabHeight = (44 * s).dp,
            tabMinWidth = 108.dp,
            tabMaxWidth = 220.dp,
            tabSpacing = 6.dp,
            tabFaviconSize = 16.dp,
            tabCloseTargetWidth = 32.dp,
            toolbarHeight = (56 * s).dp,
            toolbarIconSize = 20.dp,
            toolbarTouchTarget = (44 * s).dp,
            addressFieldHeight = (46 * s).dp,
            addressFieldMinWidth = 200.dp,
            cancelActionWidth = 64.dp,
            navigationBarHeight = (52 * s).dp,
            navigationBarMaxWidth = Dp.Unspecified,
            navigationBarTouchTarget = (48 * s).dp,
            navigationRailWidth = 80.dp,
            sheetMaxWidth = 560.dp,
            screenContentMaxWidth = Dp.Unspecified,
            settingsContentMaxWidth = Dp.Unspecified,
            settingsPaneWidth = 280.dp,
            homeContentMaxWidth = Dp.Unspecified,
        )
        AdaptiveWidthClass.MEDIUM -> AdaptiveUiMetrics(
            layout = layout,
            chromeHorizontalPadding = 12.dp,
            chromeGroupSpacing = 10.dp,
            tabStripHeight = 56.dp,
            tabHeight = (44 * s).dp,
            tabMinWidth = 116.dp,
            tabMaxWidth = 200.dp,
            tabSpacing = 6.dp,
            tabFaviconSize = 16.dp,
            tabCloseTargetWidth = 32.dp,
            toolbarHeight = (58 * s).dp,
            toolbarIconSize = 20.dp,
            toolbarTouchTarget = (44 * s).dp,
            addressFieldHeight = (48 * s).dp,
            addressFieldMinWidth = 260.dp,
            cancelActionWidth = 68.dp,
            navigationBarHeight = (54 * s).dp,
            navigationBarMaxWidth = 640.dp,
            navigationBarTouchTarget = (48 * s).dp,
            navigationRailWidth = 80.dp,
            sheetMaxWidth = 600.dp,
            screenContentMaxWidth = 680.dp,
            settingsContentMaxWidth = 600.dp,
            settingsPaneWidth = 280.dp,
            homeContentMaxWidth = 720.dp,
        )
        AdaptiveWidthClass.EXPANDED -> AdaptiveUiMetrics(
            layout = layout,
            chromeHorizontalPadding = 24.dp,
            chromeGroupSpacing = 12.dp,
            tabStripHeight = 60.dp,
            tabHeight = (46 * s).dp,
            tabMinWidth = 128.dp,
            tabMaxWidth = 240.dp,
            tabSpacing = 8.dp,
            tabFaviconSize = 18.dp,
            tabCloseTargetWidth = 34.dp,
            toolbarHeight = (60 * s).dp,
            toolbarIconSize = 21.dp,
            toolbarTouchTarget = (46 * s).dp,
            addressFieldHeight = (50 * s).dp,
            addressFieldMinWidth = 320.dp,
            cancelActionWidth = 72.dp,
            navigationBarHeight = (56 * s).dp,
            navigationBarMaxWidth = 640.dp,
            navigationBarTouchTarget = (48 * s).dp,
            navigationRailWidth = 80.dp,
            sheetMaxWidth = 640.dp,
            screenContentMaxWidth = 760.dp,
            settingsContentMaxWidth = 600.dp,
            settingsPaneWidth = 280.dp,
            homeContentMaxWidth = 760.dp,
        )
        AdaptiveWidthClass.LARGE -> AdaptiveUiMetrics(
            layout = layout,
            chromeHorizontalPadding = 32.dp,
            chromeGroupSpacing = 12.dp,
            tabStripHeight = 60.dp,
            tabHeight = (46 * s).dp,
            tabMinWidth = 140.dp,
            tabMaxWidth = 260.dp,
            tabSpacing = 8.dp,
            tabFaviconSize = 18.dp,
            tabCloseTargetWidth = 34.dp,
            toolbarHeight = (60 * s).dp,
            toolbarIconSize = 21.dp,
            toolbarTouchTarget = (46 * s).dp,
            addressFieldHeight = (50 * s).dp,
            addressFieldMinWidth = 360.dp,
            cancelActionWidth = 72.dp,
            navigationBarHeight = (56 * s).dp,
            navigationBarMaxWidth = 640.dp,
            navigationBarTouchTarget = (48 * s).dp,
            navigationRailWidth = 88.dp,
            sheetMaxWidth = 640.dp,
            screenContentMaxWidth = 800.dp,
            settingsContentMaxWidth = 640.dp,
            settingsPaneWidth = 320.dp,
            homeContentMaxWidth = 800.dp,
        )
        AdaptiveWidthClass.EXTRA_LARGE -> AdaptiveUiMetrics(
            layout = layout,
            chromeHorizontalPadding = 40.dp,
            chromeGroupSpacing = 12.dp,
            tabStripHeight = 60.dp,
            tabHeight = (46 * s).dp,
            tabMinWidth = 150.dp,
            tabMaxWidth = 280.dp,
            tabSpacing = 8.dp,
            tabFaviconSize = 18.dp,
            tabCloseTargetWidth = 34.dp,
            toolbarHeight = (62 * s).dp,
            toolbarIconSize = 21.dp,
            toolbarTouchTarget = (46 * s).dp,
            addressFieldHeight = (52 * s).dp,
            addressFieldMinWidth = 400.dp,
            cancelActionWidth = 72.dp,
            navigationBarHeight = (56 * s).dp,
            navigationBarMaxWidth = 640.dp,
            navigationBarTouchTarget = (48 * s).dp,
            navigationRailWidth = 88.dp,
            sheetMaxWidth = 720.dp,
            screenContentMaxWidth = 880.dp,
            settingsContentMaxWidth = 680.dp,
            settingsPaneWidth = 320.dp,
            homeContentMaxWidth = 880.dp,
        )
    }
}

/** Convenience accessor for composables that need layout + metrics together. */
@Composable
fun rememberAdaptiveUiMetrics(userScale: Float = 1f): AdaptiveUiMetrics {
    val layout = rememberWindowAdaptiveLayout()
    return remember(layout, userScale) { adaptiveUiMetrics(layout, userScale) }
}
