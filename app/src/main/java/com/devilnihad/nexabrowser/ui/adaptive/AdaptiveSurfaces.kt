/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 RebelRoot Ltd
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

package com.devilnihad.nexabrowser.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Shared adaptive-width building blocks for sheets, dialogs and full-screen
 * content columns. On compact windows these helpers are no-ops (content keeps its
 * existing full-width phone layout); on medium+ windows they cap the content
 * width and center it so nothing stretches edge-to-edge on a 1600dp display.
 */

/**
 * Caps the content of a [androidx.compose.material3.ModalBottomSheet] (or any
 * full-width surface) at [maxWidth] and centers it horizontally. Use inside the
 * sheet's `ColumnScope` around the sheet's existing content, e.g.:
 *
 * ```
 * ModalBottomSheet(...) {
 *     AdaptiveSheetContent(metrics) {
 *         Column(Modifier.fillMaxWidth()...) { ...existing content... }
 *     }
 * }
 * ```
 *
 * The receiver is a [ColumnScope] so the wrapper participates in the sheet's
 * own column without changing its vertical arrangement semantics.
 */
@Composable
fun AdaptiveSheetContent(
    metrics: AdaptiveUiMetrics,
    modifier: Modifier = Modifier,
    maxWidth: Dp = metrics.sheetMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // widthIn BEFORE fillMaxWidth: the cap must clamp the incoming max
        // constraint, otherwise a fillMaxWidth child re-expands to full width.
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Caps a full-screen content column (settings pages, list screens, tool screens)
 * at [maxWidth] and centers it within the available width. On compact widths
 * ([maxWidth] unspecified or window narrower than the cap) behavior is identical
 * to a plain `fillMaxWidth` column.
 */
fun Modifier.adaptiveScreenContainer(
    metrics: AdaptiveUiMetrics,
    maxWidth: Dp = metrics.screenContentMaxWidth,
): Modifier = if (maxWidth == Dp.Unspecified) {
    this
} else {
    this.widthIn(max = maxWidth)
}

/**
 * Caps dialog content so platform dialogs (which can reach ~65% of window width
 * on large screens) keep a readable, phone-like measure. Apply to the dialog's
 * `modifier` parameter.
 */
fun Modifier.adaptiveDialogContainer(
    metrics: AdaptiveUiMetrics,
): Modifier = this.widthIn(max = metrics.sheetMaxWidth)
