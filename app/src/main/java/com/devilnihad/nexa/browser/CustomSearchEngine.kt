package com.devilnihad.nexa.browser

data class CustomSearchEngine(
    val name: String,
    val queryUrl: String,
    val suggestUrl: String = ""
)
