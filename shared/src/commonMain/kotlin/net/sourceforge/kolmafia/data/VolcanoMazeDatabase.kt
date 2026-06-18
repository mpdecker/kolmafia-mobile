package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import net.sourceforge.kolmafia.volcano.VolcanoMap
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.MAPS
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Volcano nemesis maze map sequences from [volcanomaze.txt]. Mirrors desktop
 * [VolcanoMazeManager.readMapSequences].
 */
@OptIn(ExperimentalResourceApi::class)
object VolcanoMazeDatabase {

    private val keyToMapSequence = mutableMapOf<Int, Array<VolcanoMap?>>()
    private val coordsToKey = mutableMapOf<String, Int>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedSequenceCount: Int get() = keyToMapSequence.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/volcanomaze.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun getMapSequence(key: Int): Array<VolcanoMap?>? = keyToMapSequence[key]

    fun keyForCoordinates(platforms: String): Int? = coordsToKey[platforms]

    internal fun parseForTest(text: String): ParseSnapshot = parse(text)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        applyParse(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        keyToMapSequence.clear()
        coordsToKey.clear()
        loaded = false
    }

    data class ParseSnapshot(
        val keyToMapSequence: Map<Int, Array<VolcanoMap?>>,
        val coordsToKey: Map<String, Int>,
    )

    private fun applyParse(snapshot: ParseSnapshot) {
        keyToMapSequence.clear()
        keyToMapSequence.putAll(snapshot.keyToMapSequence)
        coordsToKey.clear()
        coordsToKey.putAll(snapshot.coordsToKey)
    }

    private fun parse(text: String): ParseSnapshot {
        val sequences = mutableMapOf<Int, Array<VolcanoMap?>>()
        val reverse = mutableMapOf<String, Int>()

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size != 3) continue
            val key = parts[0].trim().toIntOrNull() ?: continue
            val level = parts[1].trim().toIntOrNull() ?: continue
            val platforms = parts[2].trim()
            if (level !in 1..MAPS || platforms.isEmpty()) continue

            val mapSequence = sequences.getOrPut(key) { arrayOfNulls(MAPS) }
            if (mapSequence[level - 1] != null) continue
            if (reverse.containsKey(platforms)) continue

            val map = VolcanoMap(platforms)
            mapSequence[level - 1] = map
            sequences[key] = mapSequence
            reverse[platforms] = key
        }

        return ParseSnapshot(sequences, reverse)
    }
}
