package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object FoldGroupDatabase {
    private val groups = mutableListOf<FoldGroup>()
    private val itemToGroup = mutableMapOf<String, FoldGroup>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/foldgroups.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 2) continue
            val hpDamagePct = parts[0].trim().toIntOrNull() ?: 0
            val items = parts.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
            if (items.isEmpty()) continue
            val group = FoldGroup(hpDamagePct, items)
            groups += group
            for (item in items) itemToGroup[item.lowercase()] = group
        }
        loaded = true
    }

    fun groupFor(itemName: String): FoldGroup? = itemToGroup[itemName.lowercase()]

    fun nextFold(itemName: String): String? {
        val group = groupFor(itemName) ?: return null
        val idx = group.items.indexOfFirst { it.equals(itemName, ignoreCase = true) }
        return if (idx >= 0) group.items[(idx + 1) % group.items.size] else null
    }

    fun allGroups(): List<FoldGroup> = groups
}
