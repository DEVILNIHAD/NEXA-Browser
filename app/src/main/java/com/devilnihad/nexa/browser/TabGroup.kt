/*
 * NEXA Browser - A premium, private, and secure web browser.
 * Copyright (C) 2026 DEVILNIHAD Ltd
 */

package com.devilnihad.nexa.browser

data class TabGroup(
    val id: String,
    val title: String,
    val color: Long, // Color values (e.g. 0xFF4CAF50)
    val tabIds: List<String> = emptyList()
)
