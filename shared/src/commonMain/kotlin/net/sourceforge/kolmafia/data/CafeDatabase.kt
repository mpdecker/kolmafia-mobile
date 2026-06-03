package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses cafe_food.txt and cafe_booze.txt from bundled compose resources.
// Format (tab-separated): name  price
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object CafeDatabase {

    private val byNameFood = mutableMapOf<String, CafeData>()
    private val byNameDrink = mutableMapOf<String, CafeData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loadFile("files/data/cafe_food.txt", ConsumableType.FOOD)
        loadFile("files/data/cafe_booze.txt", ConsumableType.DRINK)
        loaded = true
    }

    fun getFood(name: String): CafeData? = byNameFood[name.lowercase()]
    fun getDrink(name: String): CafeData? = byNameDrink[name.lowercase()]
    fun allFood(): Collection<CafeData> = byNameFood.values
    fun allDrinks(): Collection<CafeData> = byNameDrink.values

    private suspend fun loadFile(filename: String, type: ConsumableType) {
        val text = Res.readBytes(filename).decodeToString()
        val map = when (type) {
            ConsumableType.FOOD -> byNameFood
            ConsumableType.DRINK -> byNameDrink
            ConsumableType.SPLEEN -> return
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val name = parts[0]
            val price = parts[1].toIntOrNull() ?: continue

            val entry = CafeData(name = name, price = price, type = type)
            map[name.lowercase()] = entry
        }
    }
}
