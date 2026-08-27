package net.sourceforge.kolmafia.inventory

/**
 * Listener-style inventory refresh triggers (desktop fireInventoryChanged subset).
 * Used by concoction/Maximizer glue after local inventory deltas (Phases 2556–2570).
 */
object InventoryRefresh {

    fun interface Listener {
        fun onInventoryChanged()
    }

    private val listeners = mutableListOf<Listener>()

    fun addListener(listener: Listener) {
        if (listener !in listeners) listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    fun fireInventoryChanged() {
        // Snapshot to allow listeners to unregister during notify
        listeners.toList().forEach { it.onInventoryChanged() }
    }
}
