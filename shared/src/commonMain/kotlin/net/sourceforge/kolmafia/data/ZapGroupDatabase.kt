package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object ZapGroupDatabase {
    private val groups = mutableListOf<List<String>>()
    private val itemToGroup = mutableMapOf<String, List<String>>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/zapgroups.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val items = line.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (items.size > 1) {
                groups += items
                for (item in items) itemToGroup[item.lowercase()] = items
            }
        }
        loaded = true
    }

    fun groupFor(itemName: String): List<String>? = itemToGroup[itemName.lowercase()]

    fun nextZap(itemName: String): String? {
        val group = groupFor(itemName) ?: return null
        val idx = group.indexOfFirst { it.equals(itemName, ignoreCase = true) }
        return if (idx >= 0) group[(idx + 1) % group.size] else null
    }

    fun allGroups(): List<List<String>> = groups
}
