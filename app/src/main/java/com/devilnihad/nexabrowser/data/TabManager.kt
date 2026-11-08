package com.devilnihad.nexabrowser.data

class TabManager {
    private val tabs = mutableListOf<Tab>()
    private var nextId = 1L
    var currentId: Long? = null; private set
    fun all(): List<Tab> = tabs.toList()
    fun create(privateMode: Boolean = false): Tab { val t = Tab(nextId++, "about:blank", privateMode = privateMode); tabs += t; currentId = t.id; return t }
    fun select(id: Long) { if (tabs.any { it.id == id }) currentId = id }
    fun current(): Tab? = tabs.firstOrNull { it.id == currentId }
    fun close(id: Long): Tab? { val i = tabs.indexOfFirst { it.id == id }; if (i < 0) return null; val removed = tabs.removeAt(i); if (currentId == id) currentId = tabs.getOrNull(i.coerceAtMost(tabs.lastIndex))?.id ?: tabs.lastOrNull()?.id; return removed }
    fun closeAll() { tabs.clear(); currentId = null }
}
