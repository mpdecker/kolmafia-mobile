package net.sourceforge.kolmafia.data

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

    internal fun registerForTest(data: PokefamData) {
        byId[data.familiarId] = data
    }

    internal fun resetForTest() {
        byId.clear()
        loaded = false
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
