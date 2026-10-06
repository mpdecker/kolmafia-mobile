package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrepareForURLTest {

    @AfterTest
    fun tearDown() {
        PrepareForURL.resetForTest()
    }

    @Test
    fun hermitAutoworthless_retrievesWorthless() = runBlocking {
        val retrieved = mutableListOf<Int>()
        PrepareForURL.retrieveItem = { id, _ ->
            retrieved += id
            1
        }
        val prefs = Preferences(MapSettings())
        val result = PrepareForURL.prepare(
            location = "hermit.php?auto=1&autoworthless=on",
            preferences = prefs,
        )
        assertTrue(result.proceed)
        assertTrue(PrepareForURL.WORTHLESS_ITEM in retrieved)
        assertFalse(prefs.getBoolean("autoSatisfyWithNPCs", false))
    }

    @Test
    fun casino_retrievesPass() = runBlocking {
        val retrieved = mutableListOf<Int>()
        PrepareForURL.retrieveItem = { id, _ ->
            retrieved += id
            1
        }
        PrepareForURL.inZombiecore = { false }
        val result = PrepareForURL.prepare(location = "casino.php")
        assertTrue(result.proceed)
        assertEquals(listOf(PrepareForURL.CASINO_PASS), retrieved)
    }

    @Test
    fun orcChasm_retrievesBridge() = runBlocking {
        val retrieved = mutableListOf<Int>()
        PrepareForURL.retrieveItem = { id, _ ->
            retrieved += id
            1
        }
        val result = PrepareForURL.prepare(
            location = "place.php?whichplace=orc_chasm&action=bridge0",
        )
        assertTrue(result.proceed)
        assertEquals(listOf(PrepareForURL.BRIDGE), retrieved)
    }

    @Test
    fun pandamonium_unknownComedy_aborts() = runBlocking {
        val result = PrepareForURL.prepare(
            location = "pandamonium.php?action=mourn&whichitem=1",
        )
        assertFalse(result.proceed)
        assertTrue(result.abortMessage!!.contains("not a comedy item"))
    }
}
