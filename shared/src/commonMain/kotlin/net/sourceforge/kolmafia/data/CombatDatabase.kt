package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object CombatDatabase : ZoneLookup {
    private val byLocation = mutableMapOf<String, ZoneCombatData>()
    private val entries = mutableListOf<ZoneCombatData>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return
        loaded = true

        val text = Res.readBytes("files/data/combats.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val locationName = parts[0]
            val combatPercent = parts[1].toIntOrNull() ?: -1

            val monsters = mutableListOf<MonsterWeight>()
            for (idx in 2 until parts.size) {
                val entry = parts[idx].trim()
                if (entry.isBlank()) continue
                val colonIdx = entry.lastIndexOf(": ")
                if (colonIdx >= 0) {
                    val name = entry.substring(0, colonIdx).trim()
                    val weight = entry.substring(colonIdx + 2).trim().toIntOrNull() ?: 1
                    monsters.add(MonsterWeight(name, weight))
                } else {
                    monsters.add(MonsterWeight(entry, 1))
                }
            }

            val data = ZoneCombatData(
                locationName = locationName,
                combatPercent = combatPercent,
                monsters = monsters
            )

            entries.add(data)
            byLocation[locationName.lowercase()] = data
        }
    }

    override fun getByLocation(name: String): ZoneCombatData? = byLocation[name.lowercase()]

    fun all(): List<ZoneCombatData> = entries.toList()
}
