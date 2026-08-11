package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object PokefamDatabase {
    private val byId = mutableMapOf<Int, PokefamData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        FamiliarDefinitionDatabase.load()
        val text = Res.readBytes("files/data/fambattle.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getById(familiarId: Int): PokefamData? = byId[familiarId]

    fun getByName(race: String): PokefamData? {
        val familiarId = FamiliarDefinitionDatabase.getByName(race)?.id ?: return null
        return byId[familiarId]
    }

    fun registerFromFight(
        race: String,
        level: Int,
        power: Int,
        hp: Int,
        attribute: String,
        move1: String?,
        move2: String?,
        move3: String?,
        sessionLogger: SessionLogger? = null,
    ) {
        if (race.isBlank()) return
        val familiarId = FamiliarDefinitionDatabase.getByName(race)?.id ?: return
        val m1 = move1.orEmpty()
        val m2 = move2.orEmpty()
        val m3 = move3.orEmpty()
        val current = byId[familiarId]
        if (current == null) {
            val data = newPokefamData(familiarId, level, power, hp, attribute, m1, m2, m3)
            byId[familiarId] = data
            logNewPokefamData(race, data, sessionLogger)
            return
        }

        var updated = current
        var changed = false
        if (m1.isNotBlank() && m1 != updated.move1) {
            updated = updated.copy(move1 = m1)
            changed = true
        }
        if (m2.isNotBlank() && m2 != updated.move2) {
            updated = updated.copy(move2 = m2)
            changed = true
        }
        if (attribute.isNotBlank() && attribute != updated.attribute) {
            updated = updated.copy(attribute = attribute)
            changed = true
        }
        when (level) {
            2 -> {
                if (power != updated.power2) {
                    updated = updated.copy(power2 = power)
                    changed = true
                }
                if (hp != updated.hp2) {
                    updated = updated.copy(hp2 = hp)
                    changed = true
                }
            }
            3 -> {
                if (power != updated.power3) {
                    updated = updated.copy(power3 = power)
                    changed = true
                }
                if (hp != updated.hp3) {
                    updated = updated.copy(hp3 = hp)
                    changed = true
                }
            }
            5 -> {
                if (m3.isNotBlank() && m3 != updated.move3) {
                    updated = updated.copy(move3 = m3)
                    changed = true
                }
                // fall through
            }
            4 -> {
                if (power != updated.power4) {
                    updated = updated.copy(power4 = power)
                    changed = true
                }
                if (hp != updated.hp4) {
                    updated = updated.copy(hp4 = hp)
                    changed = true
                }
            }
        }
        if (changed) {
            byId[familiarId] = updated
            logNewPokefamData(race, updated, sessionLogger)
        }
    }

    internal fun registerForTest(data: PokefamData) {
        byId[data.familiarId] = data
    }

    internal fun resetForTest() {
        byId.clear()
        loaded = false
    }

    private fun newPokefamData(
        familiarId: Int,
        level: Int,
        power: Int,
        hp: Int,
        attribute: String,
        move1: String,
        move2: String,
        move3: String,
    ): PokefamData {
        val levelData = power to hp
        return PokefamData(
            familiarId = familiarId,
            power2 = if (level == 2) levelData.first else 0,
            hp2 = if (level == 2) levelData.second else 0,
            power3 = if (level == 3) levelData.first else 0,
            hp3 = if (level == 3) levelData.second else 0,
            power4 = if (level >= 4) levelData.first else 0,
            hp4 = if (level >= 4) levelData.second else 0,
            move1 = move1.ifBlank { "Unknown" },
            move2 = move2.ifBlank { "Unknown" },
            move3 = move3.ifBlank { "Unknown" },
            attribute = attribute.ifBlank { "None" },
        )
    }

    private fun logNewPokefamData(race: String, data: PokefamData, sessionLogger: SessionLogger?) {
        sessionLogger?.appendRawLines(
            listOf(
                "--------------------",
                formatPokefamRow(race, data),
                "--------------------",
            ),
        )
    }

    private fun formatPokefamRow(race: String, data: PokefamData): String {
        fun levelStr(power: Int, hp: Int): String =
            if (power <= 0 && hp <= 0) "x/x" else "$power/$hp"
        return listOf(
            race,
            levelStr(data.power2, data.hp2),
            levelStr(data.power3, data.hp3),
            levelStr(data.power4, data.hp4),
            data.move1,
            data.move2,
            data.move3,
            data.attribute,
        ).joinToString("\t")
    }

    private fun parse(text: String) {
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (line.startsWith("Type")) continue

            val parts = line.split('\t')
            if (parts.size != 8) continue

            val familiar = FamiliarDefinitionDatabase.getByName(parts[0].trim()) ?: continue
            val level2 = parseLevel(parts[1].trim()) ?: continue
            val level3 = parseLevel(parts[2].trim()) ?: continue
            val level4 = parseLevel(parts[3].trim()) ?: continue

            byId[familiar.id] = PokefamData(
                familiarId = familiar.id,
                power2 = level2.first,
                hp2 = level2.second,
                power3 = level3.first,
                hp3 = level3.second,
                power4 = level4.first,
                hp4 = level4.second,
                move1 = parts[4].trim(),
                move2 = parts[5].trim(),
                move3 = parts[6].trim(),
                attribute = parts[7].trim(),
            )
        }
    }

    private fun parseLevel(raw: String): Pair<Int, Int>? {
        val pieces = raw.split('/')
        if (pieces.size != 2) return null
        val power = pieces[0].toIntOrNull() ?: return null
        val hp = pieces[1].toIntOrNull() ?: return null
        return power to hp
    }
}
