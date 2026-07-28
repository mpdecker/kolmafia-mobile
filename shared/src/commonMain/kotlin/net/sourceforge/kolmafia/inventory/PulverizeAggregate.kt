package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.ash.AggregateType
import net.sourceforge.kolmafia.ash.AggregateValue
import net.sourceforge.kolmafia.ash.AshType
import net.sourceforge.kolmafia.ash.AshValue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.PulverizeFlags

/** Desktop RuntimeLibrary.get_related pulverize decode (values scaled ×1,000,000). */
object PulverizeAggregate {

    private const val WAD2POWDER = -12
    private const val WAD2NUGGET = -6
    private const val WAD2GEM = 1321

    private const val TWINKLY_WAD = 1450
    private const val HOT_WAD = 1451
    private const val COLD_WAD = 1452
    private const val SPOOKY_WAD = 1453
    private const val STENCH_WAD = 1454
    private const val SLEAZE_WAD = 1455

    private const val HOT_CLUSTER = 6551
    private const val COLD_CLUSTER = 6552
    private const val SPOOKY_CLUSTER = 6553
    private const val STENCH_CLUSTER = 6554
    private const val SLEAZE_CLUSTER = 6555

    fun decodeToAggregate(
        pulver: Int,
        itemIntType: AggregateType = AggregateType(AshType.ITEM, AshType.INT),
    ): AggregateValue {
        val result = AggregateValue(itemIntType)
        for ((itemId, amount) in decode(pulver)) {
            val name = ItemDatabase.getById(itemId)?.name ?: itemId.toString()
            result[AshValue.item(name)] = AshValue.of(amount)
        }
        return result
    }

    fun decode(pulver: Int): Map<Int, Long> {
        if (pulver == -1 || (pulver and PulverizeFlags.MALUS_UPGRADE) != 0) {
            return emptyMap()
        }
        if (pulver > 0) {
            return mapOf(pulver to 1_000_000L)
        }

        val clusters = (pulver and PulverizeFlags.YIELD_1C) != 0
        val elems = mutableListOf<Int>()
        if ((pulver and PulverizeFlags.ELEM_HOT) != 0) {
            elems.add(if (clusters) HOT_CLUSTER else HOT_WAD)
        }
        if ((pulver and PulverizeFlags.ELEM_COLD) != 0) {
            elems.add(if (clusters) COLD_CLUSTER else COLD_WAD)
        }
        if ((pulver and PulverizeFlags.ELEM_STENCH) != 0) {
            elems.add(if (clusters) STENCH_CLUSTER else STENCH_WAD)
        }
        if ((pulver and PulverizeFlags.ELEM_SPOOKY) != 0) {
            elems.add(if (clusters) SPOOKY_CLUSTER else SPOOKY_WAD)
        }
        if ((pulver and PulverizeFlags.ELEM_SLEAZE) != 0) {
            elems.add(if (clusters) SLEAZE_CLUSTER else SLEAZE_WAD)
        }
        if ((pulver and PulverizeFlags.ELEM_TWINKLY) != 0) {
            elems.add(TWINKLY_WAD)
        }
        if (elems.isEmpty()) {
            return emptyMap()
        }

        var powders = 0L
        var nuggets = 0L
        var wads = 0L
        when {
            (pulver and PulverizeFlags.YIELD_3W) != 0 -> wads = 3_000_000L
            (pulver and PulverizeFlags.YIELD_1W3N_2W) != 0 -> {
                wads = 1_500_000L
                nuggets = 1_500_000L
            }
            (pulver and PulverizeFlags.YIELD_4N_1W) != 0 -> {
                wads = 500_000L
                nuggets = 2_000_000L
            }
            (pulver and PulverizeFlags.YIELD_3N) != 0 -> nuggets = 3_000_000L
            (pulver and PulverizeFlags.YIELD_1N3P_2N) != 0 -> {
                nuggets = 1_500_000L
                powders = 1_500_000L
            }
            (pulver and PulverizeFlags.YIELD_4P_1N) != 0 -> {
                nuggets = 500_000L
                powders = 2_000_000L
            }
            (pulver and PulverizeFlags.YIELD_3P) != 0 -> powders = 3_000_000L
            (pulver and PulverizeFlags.YIELD_2P) != 0 -> powders = 2_000_000L
            (pulver and PulverizeFlags.YIELD_1P) != 0 -> powders = 1_000_000L
        }

        var gems = wads / 100
        wads -= gems
        val nelems = elems.size
        val out = mutableMapOf<Int, Long>()
        for (wad in elems) {
            if (powders > 0) {
                addAmount(out, wad + WAD2POWDER, powders / nelems)
            }
            if (nuggets > 0) {
                addAmount(out, wad + WAD2NUGGET, nuggets / nelems)
            }
            var localWads = wads
            var localGems = gems
            if (localWads > 0) {
                if (wad == TWINKLY_WAD) {
                    localWads += localGems
                    localGems = 0
                }
                addAmount(out, wad, localWads / nelems)
            }
            if (localGems > 0) {
                addAmount(out, wad + WAD2GEM, localGems / nelems)
            }
            if (clusters) {
                addAmount(out, wad, 1_000_000L)
            }
        }
        return out
    }

    private fun addAmount(out: MutableMap<Int, Long>, itemId: Int, amount: Long) {
        if (amount <= 0) return
        out[itemId] = (out[itemId] ?: 0L) + amount
    }
}
