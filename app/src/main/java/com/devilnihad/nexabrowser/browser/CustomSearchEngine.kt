package com.devilnihad.nexabrowser.browser

data class CustomSearchEngine(
    val name: String,
    val queryUrl: String,
    val suggestUrl: String = ""
)
