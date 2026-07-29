package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BastilleDatabaseLoaderTest {

    @AfterTest
    fun tearDown() {
        BastilleDatabase.resetForTest()
    }

    @Test
    fun readsFirstRowStats() {
        val fixture = "1\tBARBECUE\tBRUTALIST\tCANNON\tSHARKS\t6\t5\t2\t0\t3\t1"
        val stats = BastilleDatabase.parseForTest(fixture).statsByKey[0]
        assertNotNull(stats)
        assertEquals(6, stats.ma)
        assertEquals(5, stats.md)
        assertEquals(2, stats.ca)
        assertEquals(0, stats.cd)
        assertEquals(3, stats.pa)
        assertEquals(1, stats.pd)
    }

    @Test
    fun stylesToKey_roundTripsKeyZero() {
        val styles = setOf(
            BastilleDatabase.Style.BARBECUE,
            BastilleDatabase.Style.BRUTALIST,
            BastilleDatabase.Style.CANNON,
            BastilleDatabase.Style.SHARKS,
        )
        assertEquals(0, BastilleDatabase.stylesToKey(styles))
        assertEquals(styles, BastilleDatabase.keyToStyles(0))
    }

    @Test
    fun stylesToKey_roundTripsKey80() {
        val styles = setOf(
            BastilleDatabase.Style.BARBERSHOP,
            BastilleDatabase.Style.NOUVEAU,
            BastilleDatabase.Style.GESTURE,
            BastilleDatabase.Style.TRUTH,
        )
        val key = BastilleDatabase.stylesToKey(styles)
        assertEquals(80, key)
        assertEquals(styles, BastilleDatabase.keyToStyles(key))
    }

    @Test
    fun load_fullFile_has81StyleSets() {
        runBlocking {
            BastilleDatabase.load()
            assertTrue(BastilleDatabase.isLoaded)
            assertEquals(81, BastilleDatabase.styleSetCount)
            assertNotNull(BastilleDatabase.statsForKey(0))
            assertNotNull(BastilleDatabase.statsForKey(80))
        }
    }

    @Test
    fun predictedStats_matchesKeyZeroFileRow() {
        runBlocking {
            BastilleDatabase.load()
            val styles = mapOf(
                BastilleDatabase.Upgrade.BARBICAN to BastilleDatabase.Style.BARBECUE,
                BastilleDatabase.Upgrade.DRAWBRIDGE to BastilleDatabase.Style.BRUTALIST,
                BastilleDatabase.Upgrade.MURDER_HOLES to BastilleDatabase.Style.CANNON,
                BastilleDatabase.Upgrade.MOAT to BastilleDatabase.Style.SHARKS,
            )
            val predicted = BastilleDatabase.predictedStats(styles)
            assertNotNull(predicted)
            assertEquals(BastilleDatabase.statsForKey(0), predicted)
        }
    }
}
