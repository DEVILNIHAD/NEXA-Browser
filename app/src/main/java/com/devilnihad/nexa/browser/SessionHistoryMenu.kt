/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 DEVILNIHAD Ltd
 */

package com.devilnihad.nexa.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistorySheet(
    isBackHistory: Boolean,
    historyEntries: List<SessionHistoryEntry>,
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onSelectEntry: (SessionHistoryEntry) -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetBg = if (isAmoled) Color(0xFF0A0A0C) else if (isDarkTheme) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface
    val itemBg = if (isAmoled) Color(0xFF141418) else if (isDarkTheme) Color(0xFF2C2C2E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme || isAmoled) Color(0xFFF2F2F7) else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDarkTheme || isAmoled) Color(0xFF8E8E93) else MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = sheetBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme || isAmoled) Color(0xFF48484A) else MaterialTheme.colorScheme.outlineVariant
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBackHistory) Icons.Rounded.ArrowBack else Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isBackHistory) "Back History" else "Forward History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${historyEntries.size} page${if (historyEntries.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary
                )
            }

            if (historyEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBackHistory) "No earlier history entries" else "No forward history entries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyEntries, key = { "${it.index}_${it.url}" }) { entry ->
                        val isHomeEntry = entry.index == -1 || entry.url == "about:blank"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(itemBg)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSelectEntry(entry)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHomeEntry) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHomeEntry) Icons.Rounded.Home else Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = if (isHomeEntry) MaterialTheme.colorScheme.onSecondaryContainer else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isHomeEntry) "New Tab" else entry.title.takeIf { it.isNotBlank() } ?: entry.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isHomeEntry) "Home Screen" else formatUrlDomain(entry.url),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatUrlDomain(rawUrl: String): String {
    return try {
        val uri = android.net.Uri.parse(rawUrl)
        val host = uri.host ?: return rawUrl
        host.removePrefix("www.")
    } catch (_: Exception) {
        rawUrl
    }
}
