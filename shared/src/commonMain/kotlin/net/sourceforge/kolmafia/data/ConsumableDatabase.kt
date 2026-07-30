package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses fullness.txt, inebriety.txt, spleenhit.txt, and nonfilling.txt from bundled compose resources.
// Food/drink/spleen format (tab-separated): name  amount  levelReq  quality  adv  musc  myst  moxie  [notes]
// Nonfilling format: name  levelReq  [notes]
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ConsumableDatabase {

    private val byNameFood = mutableMapOf<String, ConsumableData>()
    private val byNameDrink = mutableMapOf<String, ConsumableData>()
    private val byNameSpleen = mutableMapOf<String, ConsumableData>()
    private val byNameNonFilling = mutableMapOf<String, ConsumableData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loadFile("files/data/fullness.txt", ConsumableType.FOOD)
        loadFile("files/data/inebriety.txt", ConsumableType.DRINK)
        loadFile("files/data/spleenhit.txt", ConsumableType.SPLEEN)
        loadNonFilling()
        loaded = true
    }

    fun getFood(name: String): ConsumableData? = byNameFood[name.lowercase()]
    fun getDrink(name: String): ConsumableData? = byNameDrink[name.lowercase()]
    fun getSpleen(name: String): ConsumableData? = byNameSpleen[name.lowercase()]
    fun getNonFilling(name: String): ConsumableData? = byNameNonFilling[name.lowercase()]

    fun get(name: String, type: ConsumableType): ConsumableData? = when (type) {
        ConsumableType.FOOD -> getFood(name)
        ConsumableType.DRINK -> getDrink(name)
        ConsumableType.SPLEEN -> getSpleen(name)
        ConsumableType.NONFILLING -> getNonFilling(name)
    }

    fun getConsumableByName(name: String): ConsumableData? {
        val key = name.lowercase()
        return byNameFood[key]
            ?: byNameDrink[key]
            ?: byNameSpleen[key]
            ?: byNameNonFilling[key]
    }

    fun getLevelReqByName(name: String): Int? = getConsumableByName(name)?.levelReq

    fun getFullnessByName(name: String): Int = getFood(name)?.amount ?: 0

    fun getInebrietyByName(name: String): Int = getDrink(name)?.amount ?: 0

    fun getSpleenByName(name: String): Int = getSpleen(name)?.amount ?: 0

    fun getQualityName(name: String): String = getConsumableByName(name)?.quality?.displayName() ?: ""

    fun getAdventureRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.advMin, it.advMax) } ?: "0"

    fun getMuscleRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.muscMin, it.muscMax) } ?: "0"

    fun getMysticalityRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.mystMin, it.mystMax) } ?: "0"

    fun getMoxieRange(name: String): String =
        getConsumableByName(name)?.let { formatRange(it.moxieMin, it.moxieMax) } ?: "0"

    fun getNotesByName(name: String): String = getConsumableByName(name)?.notes ?: ""

    fun allFood(): Collection<ConsumableData> = byNameFood.values
    fun allDrinks(): Collection<ConsumableData> = byNameDrink.values
    fun allSpleen(): Collection<ConsumableData> = byNameSpleen.values
    fun allNonFilling(): Collection<ConsumableData> = byNameNonFilling.values

    fun bestFoods(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameFood.values.filter { it.quality >= minQuality }

    fun bestDrinks(minQuality: ConsumableQuality): List<ConsumableData> =
        byNameDrink.values.filter { it.quality >= minQuality }

    internal fun resetForTest() {
        byNameFood.clear()
        byNameDrink.clear()
        byNameSpleen.clear()
        byNameNonFilling.clear()
        loaded = false
    }

    internal fun applyNonFillingParse(text: String) {
        byNameNonFilling.clear()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val entryName = parts[0].trim()
            val levelReq = parts[1].trim().toIntOrNull() ?: continue
            val notes = parts.getOrNull(2)?.trim() ?: ""

            byNameNonFilling[entryName.lowercase()] = ConsumableData(
                name = entryName,
                type = ConsumableType.NONFILLING,
                amount = 0,
                levelReq = levelReq,
                quality = ConsumableQuality.NONE,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = notes,
            )
        }
    }

    private suspend fun loadNonFilling() {
        val text = Res.readBytes("files/data/nonfilling.txt").decodeToString()
        applyNonFillingParse(text)
    }

    private suspend fun loadFile(filename: String, type: ConsumableType) {
        val text = Res.readBytes(filename).decodeToString()
        val map = when (type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN -> byNameSpleen
            ConsumableType.NONFILLING -> byNameNonFilling
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 8) continue

            val entryName = parts[0]
            val amount = parts[1].toIntOrNull() ?: continue
            val levelReq = parts[2].toIntOrNull() ?: continue
            val quality = ConsumableQuality.fromString(parts[3])
            val (advMin, advMax) = parseRange(parts[4])
            val (muscMin, muscMax) = parseRange(parts[5])
            val (mystMin, mystMax) = parseRange(parts[6])
            val (moxieMin, moxieMax) = parseRange(parts[7])
            val notes = parts.getOrNull(8)?.trim() ?: ""

            val entry = ConsumableData(
                name = entryName,
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
                notes = notes,
            )
            map[entryName.lowercase()] = entry
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

    internal fun formatRange(min: Int, max: Int): String = when {
        min == 0 && max == 0 -> "0"
        min == max -> min.toString()
        else -> "$min-$max"
    }
}
