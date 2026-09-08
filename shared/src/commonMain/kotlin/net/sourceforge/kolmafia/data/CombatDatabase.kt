package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.session.EncounterManager
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
                monsters.add(parseMonsterEntry(entry))
            }

            val data = ZoneCombatData(
                locationName = locationName,
                combatPercent = combatPercent,
                monsters = monsters,
            )

            entries.add(data)
            byLocation[locationName.lowercase()] = data
        }
    }

    /**
     * Parse `Name`, `Name: 2`, `Name: 1r50`, `Name: 1o`, `Name: 1e` combats.txt tokens.
     */
    internal fun parseMonsterEntry(entry: String): MonsterWeight {
        val colonIdx = entry.lastIndexOf(':')
        if (colonIdx < 0) {
            val name = entry.trim()
            return MonsterWeight(
                name = name,
                weight = 1,
                superlikely = EncounterManager.isSuperlikelyMonster(name),
            )
        }
        val name = entry.substring(0, colonIdx).trim()
        var token = entry.substring(colonIdx + 1).trim()
        var rejection = 0
        var parity = 0
        val rIdx = token.indexOf('r')
        if (rIdx >= 0) {
            rejection = token.substring(rIdx + 1).filter { it.isDigit() }.toIntOrNull() ?: 0
            token = token.substring(0, rIdx)
        }
        // Trailing o/e ascension parity flags (after stripping rejection)
        when {
            token.endsWith('o', ignoreCase = true) && token.dropLast(1).toIntOrNull() != null -> {
                parity = 1
                token = token.dropLast(1)
            }
            token.endsWith('e', ignoreCase = true) && token.dropLast(1).toIntOrNull() != null -> {
                parity = 2
                token = token.dropLast(1)
            }
        }
        val weight = token.toIntOrNull() ?: 1
        return MonsterWeight(
            name = name,
            weight = weight,
            rejectionPercent = rejection,
            ascensionParity = parity,
            superlikely = EncounterManager.isSuperlikelyMonster(name),
        )
    }

    override fun getByLocation(name: String): ZoneCombatData? = byLocation[name.lowercase()]

    fun poisonForLocation(locationName: String): Int {
        val combat = getByLocation(locationName) ?: return Int.MAX_VALUE
        var minPoison = Int.MAX_VALUE
        for (monster in combat.monsters) {
            if (monster.weight <= 0) continue
            val poison = MonsterDatabase.getByName(monster.name)?.poison ?: Int.MAX_VALUE
            if (poison < minPoison) minPoison = poison
        }
        return minPoison
    }

    fun all(): List<ZoneCombatData> = entries.toList()
}
