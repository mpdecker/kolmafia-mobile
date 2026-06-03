package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object ZoneParentDatabase {
    private val byName = mutableMapOf<String, ZoneParent>()
    private val zones = mutableListOf<ZoneParent>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return
        loaded = true

        val text = Res.readBytes("files/data/zonelist.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 3) continue

            val name = parts[0]
            val parent = parts[1]
            val description = parts[2]
            val requirement = if (parts.size > 3) parts[3] else ""

            val zone = ZoneParent(
                name = name,
                parent = parent,
                description = description,
                requirement = requirement
            )

            zones.add(zone)
            byName[name.lowercase()] = zone
        }
    }

    fun getByName(name: String): ZoneParent? = byName[name.lowercase()]

    fun children(parentName: String): List<ZoneParent> =
        zones.filter { it.parent.equals(parentName, ignoreCase = true) && !it.isTopLevel }

    fun topLevel(): List<ZoneParent> = zones.filter { it.isTopLevel }

    fun all(): List<ZoneParent> = zones.toList()
}
