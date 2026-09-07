package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop [CupOf13sDatabase] — item id → cup-of-13s tier from [cup_of_13s.txt]. */
@OptIn(ExperimentalResourceApi::class)
object CupOf13sDatabase {
    private val cupMap = mutableMapOf<Int, Int>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/cup_of_13s.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getTier(itemId: Int): Int = cupMap[itemId] ?: -1

    internal fun parse(text: String) {
        cupMap.clear()
        var versionSkipped = false
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!versionSkipped && !line.contains('\t')) {
                versionSkipped = true
                continue
            }
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val itemId = parts[0].trim().toIntOrNull() ?: continue
            val tier = parts[2].trim().toIntOrNull() ?: continue
            cupMap[itemId] = tier
        }
    }

    internal fun resetForTest() {
        cupMap.clear()
        loaded = false
    }

    internal fun registerForTest(itemId: Int, tier: Int) {
        cupMap[itemId] = tier
        loaded = true
    }
}
