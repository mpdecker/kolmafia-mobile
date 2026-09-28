package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.PvpManager
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.utilities.SimpleXPath

/** ASH residual polish wrap (phases 7571–7630). */
class GameRuntimeLibraryPhase7630Test {

    @Test
    fun revision_isPhase7630() {
        assertEquals("phase7630", GameRuntimeLibrary.REVISION)
        assertEquals("7630", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun toFloat_entityUsesId() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("1", outputLib(lib, """print(to_int(to_stat("Mysticality")));""").trim())
        val asFloat = outputLib(lib, """print(to_float(to_stat("Mysticality")));""").trim()
        assertTrue(asFloat.startsWith("1"), asFloat)
    }

    @Test
    fun xpath_startsWithIsLive() {
        val html = """<img src="/images/itemimages/seal.gif">"""
        val results = SimpleXPath.evaluate(html, "//img[starts-with(@src,'/images/')]/@src")
        assertEquals(listOf("/images/itemimages/seal.gif"), results)
        val lib = GameRuntimeLibrary.forTesting()
        val snippet = """
            string[int] hits = xpath("<img src='/images/itemimages/seal.gif'>", "//img[starts-with(@src,'/images/')]/@src");
            print(hits[0]);
        """.trimIndent()
        assertEquals("/images/itemimages/seal.gif", outputLib(lib, snippet).trim())
    }

    @Test
    fun pvpBeforeScript_prefersAshRunner() {
        var ash = 0
        var cli = 0
        val prefs = prefs()
        prefs.setString("beforePVPScript", "prep.ash")
        val ok = PvpManager.runBeforePvpScript(
            preferences = prefs,
            cliExecutor = { cli++ },
            ashRunner = { ash++; true },
        )
        assertTrue(ok)
        assertEquals(1, ash)
        assertEquals(0, cli)
    }

    @Test
    fun turnCounter_expiredEntriesMatchRemove() {
        val prefs = prefs()
        TurnCounter.startCounting(prefs, currentRun = 10, turns = 0, label = "Bee window end", image = "bee.gif")
        val expired = TurnCounter.expiredEntries(prefs, currentRun = 10)
        assertEquals(1, expired.size)
        assertEquals("Bee window end", expired[0].parsedLabel())
        TurnCounter.removeExpired(prefs, currentRun = 10)
        assertTrue(TurnCounter.expiredEntries(prefs, currentRun = 10).isEmpty())
    }
}
