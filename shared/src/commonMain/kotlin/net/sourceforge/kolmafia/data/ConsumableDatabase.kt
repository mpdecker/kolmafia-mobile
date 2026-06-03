package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses fullness.txt, inebriety.txt, and spleenhit.txt from bundled compose resources.
// Format (tab-separated): name  amount  levelReq  quality  adv  musc  myst  moxie  [notes]
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ConsumableDatabase {

    private val byNameFood = mutableMapOf<String, ConsumableData>()
    private val byNameDrink = mutableMapOf<String, ConsumableData>()
    private val byNameSpleen = mutableMapOf<String, ConsumableData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loadFile("files/data/fullness.txt", ConsumableType.FOOD)
        loadFile("files/data/inebriety.txt", ConsumableType.DRINK)
        loadFile("files/data/spleenhit.txt", ConsumableType.SPLEEN)
        loaded = true
    }

    fun getFood(name: String): ConsumableData? = byNameFood[name.lowercase()]
    fun getDrink(name: String): ConsumableData? = byNameDrink[name.lowercase()]
    fun getSpleen(name: String): ConsumableData? = byNameSpleen[name.lowercase()]

    fun get(name: String, type: ConsumableType): ConsumableData? = when (type) {
        ConsumableType.FOOD -> getFood(name)
        ConsumableType.DRINK -> getDrink(name)
        ConsumableType.SPLEEN -> getSpleen(name)
    }

    fun allFood(): Collection<ConsumableData> = byNameFood.values
    fun allDrinks(): Collection<ConsumableData> = byNameDrink.values
    fun allSpleen(): Collection<ConsumableData> = byNameSpleen.values

    fun bestFoods(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameFood.values.filter { it.quality >= minQuality }

    fun bestDrinks(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameDrink.values.filter { it.quality >= minQuality }

    private suspend fun loadFile(filename: String, type: ConsumableType) {
        val text = Res.readBytes(filename).decodeToString()
        val map = when (type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN -> byNameSpleen
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 8) continue

            val name = parts[0]
            val amount = parts[1].toIntOrNull() ?: continue
            val levelReq = parts[2].toIntOrNull() ?: continue
            val quality = ConsumableQuality.fromString(parts[3])
            val (advMin, advMax) = parseRange(parts[4])
            val (muscMin, muscMax) = parseRange(parts[5])
            val (mystMin, mystMax) = parseRange(parts[6])
            val (moxieMin, moxieMax) = parseRange(parts[7])
            val notes = parts.getOrNull(8)?.trim() ?: ""

            val entry = ConsumableData(
                name = name,
                type = type,
                amount = amount,
                levelReq = levelReq,
                quality = quality,
                advMin = advMin,
                advMax = advMax,
                muscMin = muscMin,
                muscMax = muscMax,
                mystMin = mystMin,
                mystMax = mystMax,
                moxieMin = moxieMin,
                moxieMax = moxieMax,
                notes = notes
            )
            map[name.lowercase()] = entry
        }
    }

    /** Parses "10-14" into Pair(10,14) and "8" into Pair(8,8). */
    private fun parseRange(s: String): Pair<Int, Int> {
        val trimmed = s.trim()
        val dashIdx = trimmed.indexOf('-', startIndex = 1) // skip possible leading minus
        return if (dashIdx > 0) {
            val min = trimmed.substring(0, dashIdx).toIntOrNull() ?: 0
            val max = trimmed.substring(dashIdx + 1).toIntOrNull() ?: 0
            Pair(min, max)
        } else {
            val v = trimmed.toIntOrNull() ?: 0
            Pair(v, v)
        }
    }
}
