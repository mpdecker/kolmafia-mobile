package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PingRequest

class IotmUtilityManagerTest {

    @Test
    fun malformedDadClueDoesNotOverwritePreviousSolution() {
        DadManager.elementalWeakness[1] = DadManager.Element.HOT
        assertFalse(DadManager.solve("not a Dad clue"))
        assertEquals(DadManager.Element.HOT, DadManager.weakness(1))
        assertEquals(DadManager.Element.NONE, DadManager.weakness(0))
        assertEquals(DadManager.Element.NONE, DadManager.weakness(11))
    }

    @Test
    fun unknownConstructColorClearsDisc() {
        assertFalse(UnusualConstructManager.solve("LANO chartreuse"))
        assertEquals(0, UnusualConstructManager.disc())
        assertTrue(UnusualConstructManager.solve("ROUTING GOLD"))
        assertNotEquals(0, UnusualConstructManager.disc())
        assertFalse(UnusualConstructManager.solve("ROUTING ???"))
        assertEquals(0, UnusualConstructManager.disc())
    }

    @Test
    fun votingBoothIsDeterministicAndReturnsFourModifiers() {
        val first = VotingBoothManager.getInitiatives(6, 0, 42)
        val second = VotingBoothManager.getInitiatives(6, 0, 42)
        assertEquals(4, first.size)
        assertEquals(first, second)
        assertNotEquals(
            VotingBoothManager.calculateSeed(6, 0, 42),
            VotingBoothManager.calculateSeed(6, 1, 42),
        )
    }

    @Test
    fun pingSerializationAndPageNormalizationAreStable() {
        val prefs = Preferences(MapSettings())
        prefs.setString("pingDefaultTestPage", "api.php")
        val result = PingManager.PingTest("api")
        result.addPing(10, 100)
        result.addPing(20, 300)
        val parsed = PingManager.parse(result.toString())
        assertEquals("api", parsed.page)
        assertEquals(2, parsed.count)
        assertEquals(10, parsed.low)
        assertEquals(20, parsed.high)
        assertEquals(30, parsed.total)
        assertEquals(400, parsed.bytes)
        assertTrue(result.isSaveable(prefs))
        assertEquals("(events)", PingRequest.normalizePage("events.php"))
        assertEquals("(status)", PingRequest.normalizePage(" STATUS "))
    }
}
