package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP510Test {

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    private fun pricedLib(): GameRuntimeLibrary {
        val prices = MallPriceManager()
        prices.cachePrice(2, 100L, 1, 1)
        prices.cachePrice(705, 50L, 1, 1)
        return GameRuntimeLibrary(mallPriceManager = prices)
    }

    @Test
    fun revision_phase510() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cheapest_printsLowPriceFirst() {
        val out = outputLib(pricedLib(), """cli_execute("cheapest seal tooth, baconstone");""")
        assertTrue(out.contains("baconstone @ 50"))
        assertTrue(out.contains("seal tooth @ 100"))
        assertTrue(out.indexOf("baconstone") < out.indexOf("seal tooth"))
    }

    @Test
    fun expensive_printsHighPriceFirst() {
        val out = outputLib(pricedLib(), """cli_execute("expensive seal tooth, baconstone");""")
        assertTrue(out.indexOf("seal tooth") < out.indexOf("baconstone"))
        assertTrue(out.contains("seal tooth @ 100"))
        assertTrue(out.contains("baconstone @ 50"))
    }

    @Test
    fun cheapest_withCommands_replacesIt() {
        val out = outputLib(pricedLib(), """cli_execute("cheapest seal tooth, baconstone; echo it");""")
        assertTrue(out.contains("baconstone"))
        assertFalse(out.contains(" @ "))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cheapestCheckOnly_listsNamesWithoutPrices() {
        val out = outputLib(pricedLib(), """cli_execute("cheapest? seal tooth, baconstone");""")
        assertTrue(out.contains("baconstone"))
        assertTrue(out.contains("seal tooth"))
        assertFalse(out.contains(" @ "))
    }
}
