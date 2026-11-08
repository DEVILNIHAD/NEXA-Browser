package com.devilnihad.nexabrowser.data

data class Tab(val id: Long, var url: String, var title: String = "New tab", var favicon: String? = null, val privateMode: Boolean = false)
