package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses cafe_food.txt and cafe_booze.txt from bundled compose resources.
// Format (tab-separated): id  name  (desktop KoLmafia cafe data; version line is a bare integer)
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object CafeDatabase {

    private val byNameFood = mutableMapOf<String, CafeData>()
    private val byNameDrink = mutableMapOf<String, CafeData>()
    private val byIdFood = mutableMapOf<Int, CafeData>()
    private val byIdDrink = mutableMapOf<Int, CafeData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loadFile("files/data/cafe_food.txt", ConsumableType.FOOD)
        loadFile("files/data/cafe_booze.txt", ConsumableType.DRINK)
        loaded = true
    }

    fun getFood(name: String): CafeData? = byNameFood[name.lowercase()]
    fun getDrink(name: String): CafeData? = byNameDrink[name.lowercase()]
    fun getCafeFoodName(id: Int): String? = byIdFood[id]?.name
    fun getCafeBoozeName(id: Int): String? = byIdDrink[id]?.name
    fun cafeFoodIds(): Set<Int> = byIdFood.keys
    fun cafeBoozeIds(): Set<Int> = byIdDrink.keys
    fun allFood(): Collection<CafeData> = byNameFood.values
    fun allDrinks(): Collection<CafeData> = byNameDrink.values

    internal fun injectForTest(id: Int, name: String, type: ConsumableType) {
        val entry = CafeData(id = id, name = name, type = type)
        when (type) {
            ConsumableType.FOOD -> {
                byNameFood[name.lowercase()] = entry
                byIdFood[id] = entry
            }
            ConsumableType.DRINK -> {
                byNameDrink[name.lowercase()] = entry
                byIdDrink[id] = entry
            }
            ConsumableType.SPLEEN, ConsumableType.NONFILLING -> return
        }
        loaded = true
    }

    internal fun resetForTest() {
        byNameFood.clear()
        byNameDrink.clear()
        byIdFood.clear()
        byIdDrink.clear()
        loaded = false
    }

    internal fun applyParseForTest(text: String, type: ConsumableType) {
        parseText(text, type)
        loaded = true
    }

    private suspend fun loadFile(filename: String, type: ConsumableType) {
        val text = Res.readBytes(filename).decodeToString()
        parseText(text, type)
    }

    private fun parseText(text: String, type: ConsumableType) {
        val byName = when (type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN, ConsumableType.NONFILLING -> return
        }
        val byId = when (type) {
            ConsumableType.FOOD -> byIdFood
            ConsumableType.DRINK -> byIdDrink
            ConsumableType.SPLEEN, ConsumableType.NONFILLING -> return
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val id = parts[0].toIntOrNull() ?: continue
            val name = parts[1].trim()
            if (name.isEmpty()) continue

            val entry = CafeData(id = id, name = name, type = type)
            byName[name.lowercase()] = entry
            byId[id] = entry
        }
    }
}
