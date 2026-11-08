package com.devilnihad.nexabrowser.data

class TabManager {
    private val tabs = mutableListOf<Tab>()
    private var nextId = 1L
    var currentId: Long? = null
        private set

    fun all(): List<Tab> = tabs.toList()
    fun current(): Tab? = tabs.firstOrNull { it.id == currentId }

    fun create(privateMode: Boolean = false): Tab {
        val tab = Tab(nextId++, privateMode = privateMode)
        tabs += tab
        currentId = tab.id
        return tab
    }

    fun select(id: Long): Boolean {
        if (tabs.none { it.id == id }) return false
        currentId = id
        return true
    }

    fun close(id: Long): Tab? {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = tabs.removeAt(index)
        if (currentId == id) {
            currentId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id
                ?: tabs.lastOrNull()?.id
        }
        return removed
    }

    fun size(): Int = tabs.size
}
