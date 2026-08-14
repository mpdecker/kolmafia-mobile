package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.preferences.Preferences

/** Minimal desktop [CandyDatabase] for Sweet Synthesis maximizer pairing checks. */
object CandyDatabase {

    private enum class CandyType {
        NONE,
        UNSPADED,
        SIMPLE,
        COMPLEX,
    }

    private const val FLAG_AVAILABLE = 0x1
    private const val FLAG_CHOCOLATE = 0x2
    private const val FLAG_NO_BLACKLIST = 0x4

    private val tier0Candy = mutableSetOf<Int>()
    private val tier1Candy = mutableSetOf<Int>()
    private val tier2Candy = mutableSetOf<Int>()
    private val tier3Candy = mutableSetOf<Int>()
    private val blacklist = mutableSetOf<Int>()
    private var tiersInitialized = false

    internal fun resetForTest() {
        tier0Candy.clear()
        tier1Candy.clear()
        tier2Candy.clear()
        tier3Candy.clear()
        blacklist.clear()
        tiersInitialized = false
    }

    fun loadBlacklist(prefs: Preferences?) {
        blacklist.clear()
        val raw = prefs?.getString("sweetSynthesisBlacklist", "").orEmpty().trim()
        if (raw.isEmpty()) return
        for (name in raw.split(",")) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) continue
            ItemDatabase.getByName(trimmed)?.let { blacklist += it.id }
        }
    }

    fun synthesisPair(effectId: Int, inventoryCount: (Int) -> Int): Boolean {
        ensureTiersInitialized()
        return synthesisPairByCount(effectId, inventoryCount).isNotEmpty()
    }

    /** First available candy pair item IDs for [effectId], or empty if none. */
    fun synthesisPairIds(effectId: Int, inventoryCount: (Int) -> Int): List<Int> {
        ensureTiersInitialized()
        return synthesisPairByCount(effectId, inventoryCount)
    }

    private fun ensureTiersInitialized() {
        if (tiersInitialized) return
        for (item in ItemDatabase.all()) {
            when (getCandyType(item.id)) {
                CandyType.UNSPADED -> tier0Candy += item.id
                CandyType.SIMPLE -> {
                    tier1Candy += item.id
                    tier2Candy += item.id
                }
                CandyType.COMPLEX -> {
                    tier3Candy += item.id
                    tier2Candy += item.id
                }
                CandyType.NONE -> Unit
            }
        }
        tiersInitialized = true
    }

    private fun synthesisPairByCount(
        effectId: Int,
        inventoryCount: (Int) -> Int,
    ): List<Int> {
        val tier = CandyEffectTier.getEffectTier(effectId)
        if (tier !in 1..3) return emptyList()

        val flags = FLAG_AVAILABLE
        val candy1Ids = candyForTier(tier, flags, inventoryCount)
            .sortedByDescending { inventoryCount(it) }

        for (itemId1 in candy1Ids) {
            val count1 = inventoryCount(itemId1)
            if (count1 == 0) return emptyList()

            val candy2Ids = sweetSynthesisPairing(effectId, itemId1, flags, inventoryCount)
                .sortedByDescending { inventoryCount(it) }

            for (itemId2 in candy2Ids) {
                val count2 = inventoryCount(itemId2)
                if (count2 == 0) break
                if (itemId1 == itemId2 && count2 == 1) continue
                return listOf(itemId1, itemId2)
            }
        }
        return emptyList()
    }

    private fun candyForTier(
        tier: Int,
        flags: Int,
        inventoryCount: (Int) -> Int,
    ): Set<Int> {
        val candies = when (tier) {
            0 -> tier0Candy
            1 -> tier1Candy
            2 -> tier2Candy
            3 -> tier3Candy
            else -> return emptySet()
        }
        val available = (flags and FLAG_AVAILABLE) != 0
        val chocolate = (flags and FLAG_CHOCOLATE) != 0
        val noblacklist = (flags and FLAG_NO_BLACKLIST) != 0
        val nofilter = !available && chocolate && noblacklist
        if (nofilter) return candies.toSet()

        return candies.filterTo(mutableSetOf()) { itemId ->
            if (available && inventoryCount(itemId) == 0) return@filterTo false
            if (!chocolate && ItemDatabase.isChocolateItem(itemId)) return@filterTo false
            !(!noblacklist && itemId in blacklist)
        }
    }

    private fun sweetSynthesisPairing(
        effectId: Int,
        itemId1: Int,
        flags: Int,
        inventoryCount: (Int) -> Int,
    ): Set<Int> {
        val tier = CandyEffectTier.getEffectTier(effectId)
        if (tier !in 1..3) return emptySet()

        val candyType = getCandyType(itemId1)
        if (candyType != CandyType.SIMPLE && candyType != CandyType.COMPLEX) {
            return emptySet()
        }

        val candidates = when (tier) {
            1 -> if (candyType == CandyType.SIMPLE) tier1Candy else emptySet()
            2 -> if (candyType == CandyType.SIMPLE) tier3Candy else tier1Candy
            3 -> if (candyType == CandyType.COMPLEX) tier3Candy else emptySet()
            else -> emptySet()
        }

        val desiredModulus = CandyEffectTier.getEffectModulus(effectId)
        if (desiredModulus < 0) return emptySet()

        val available = (flags and FLAG_AVAILABLE) != 0
        val chocolate = (flags and FLAG_CHOCOLATE) != 0
        val noblacklist = (flags and FLAG_NO_BLACKLIST) != 0

        return candidates.filterTo(mutableSetOf()) { itemId2 ->
            if ((itemId1 + itemId2) % 5 != desiredModulus) return@filterTo false
            if (!chocolate && ItemDatabase.isChocolateItem(itemId2)) return@filterTo false
            if (!noblacklist && itemId2 in blacklist) return@filterTo false
            if (available) {
                val count = inventoryCount(itemId2)
                if (count == 0 || (itemId1 == itemId2 && count == 1)) return@filterTo false
            }
            true
        }
    }

    private fun getCandyType(itemId: Int): CandyType = when (ItemDatabase.getCandyTypeName(itemId)) {
        "unspaded" -> CandyType.UNSPADED
        "simple" -> CandyType.SIMPLE
        "complex" -> CandyType.COMPLEX
        else -> CandyType.NONE
    }
}
