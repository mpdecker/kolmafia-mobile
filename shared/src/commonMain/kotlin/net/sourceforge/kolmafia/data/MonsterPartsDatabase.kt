package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Monster body parts from [monsterparts.txt]. Mirrors desktop [MonsterDatabase.readMonsterParts].
 */
@OptIn(ExperimentalResourceApi::class)
object MonsterPartsDatabase {

    private val partsById = mutableMapOf<Int, List<String>>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedEntryCount: Int get() = partsById.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/monsterparts.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun partsForId(id: Int): List<String> = partsById[id] ?: emptyList()

    internal fun parseForTest(text: String): Map<Int, List<String>> = parse(text)

    internal fun injectForTest(snapshot: Map<Int, List<String>>) {
        partsById.clear()
        partsById.putAll(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        partsById.clear()
        loaded = false
    }

    private fun applyParse(snapshot: Map<Int, List<String>>) {
        partsById.clear()
        partsById.putAll(snapshot)
    }

    private fun parse(text: String): Map<Int, List<String>> {
        val parsed = mutableMapOf<Int, List<String>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val cols = line.split('\t')
            if (cols.size < 3) continue
            val id = cols[0].toIntOrNull() ?: continue
            // cols[1] is monster name (developer aid); parts start at index 2
            val parts = cols.drop(2).map { it.trim() }.filter { it.isNotEmpty() }
            parsed[id] = parts
        }
        return parsed
    }
}
