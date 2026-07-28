package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.PulverizeFlags

class PulverizeAggregateTest {

    @Test
    fun decode_bejeweledCufflinksBitmask_splitsNuggetsAcrossElements() {
        val pulver = PulverizeFlags.PULVERIZE_BITS or 258112
        val decoded = PulverizeAggregate.decode(pulver)

        assertEquals(500_000L, decoded[HOT_NUGGETS])
        assertEquals(500_000L, decoded[COLD_NUGGETS])
        assertEquals(500_000L, decoded[STENCH_NUGGETS])
        assertEquals(500_000L, decoded[SPOOKY_NUGGETS])
        assertEquals(500_000L, decoded[SLEAZE_NUGGETS])
        assertEquals(500_000L, decoded[TWINKLY_NUGGETS])
        assertEquals(6, decoded.size)
    }

    @Test
    fun decode_clusterBitmask_returnsSingleClusterAtOneMillion() {
        val pulver = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.YIELD_1C or PulverizeFlags.ELEM_HOT
        val decoded = PulverizeAggregate.decode(pulver)
        assertEquals(mapOf(HOT_CLUSTER to 1_000_000L), decoded)
    }

    @Test
    fun decode_malusUpgradeReturnsEmpty() {
        val pulver =
            PulverizeFlags.PULVERIZE_BITS or
                PulverizeFlags.MALUS_UPGRADE or
                PulverizeFlags.YIELD_4N_1W or
                PulverizeFlags.YIELD_4P_1N or
                PulverizeFlags.ELEM_HOT
        assertEquals(emptyMap(), PulverizeAggregate.decode(pulver))
    }

    companion object {
        private const val HOT_NUGGETS = 1445
        private const val COLD_NUGGETS = 1446
        private const val SPOOKY_NUGGETS = 1447
        private const val STENCH_NUGGETS = 1448
        private const val SLEAZE_NUGGETS = 1449
        private const val TWINKLY_NUGGETS = 1444
        private const val HOT_CLUSTER = 6551
    }
}
