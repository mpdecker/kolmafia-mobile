package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP960TrackKTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun phase960_clanLoungeVisitSync_parsesItems() {
        val p = prefs()
        val html = """<img title="Looking Glass" /><img title="Fax Machine" />"""
        ClanLoungeVisitSync.parseAndWrite("clan_viplounge.php $html", p)
        val lounge = p.getString("clanLounge", "")
        assertEquals(true, lounge.contains("Looking Glass"))
        assertEquals(true, lounge.contains("Fax Machine"))
    }

    @Test
    fun phase961_clanRumpusVisitSync_parsesItems() {
        val p = prefs()
        val html = """clan_rumpus.php <b>Meat Tree</b> (3)"""
        ClanRumpusVisitSync.parseAndWrite(html, p)
        val rumpus = p.getString("clanRumpus", "")
        assertEquals(true, rumpus.contains("Meat Tree (3)"))
    }

    @Test
    fun phase962_chateauVisitSync_parsesMonster() {
        val p = prefs()
        val html = """whichplace=chateau painting blah <b>Knob Goblin</b>"""
        ChateauVisitSync.parseAndWrite(html, p)
        assertEquals("Knob Goblin", p.getString("chateauMonster", ""))
    }

    @Test
    fun phase964_sessionItemTallySync_accumulates() {
        val p = prefs()
        SessionItemTallySync.addItem("seal-clubbing club", 2, p)
        SessionItemTallySync.addItem("seal-clubbing club", 3, p)
        val tally = p.getString("_sessionItemTally", "")
        assertEquals(true, tally.contains("seal-clubbing club:5"))
    }

    @Test
    fun phase965_sessionResultTallySync_accumulates() {
        val p = prefs()
        SessionResultTallySync.addResult("Won the fight", 1, p)
        SessionResultTallySync.addResult("Won the fight", 1, p)
        val tally = p.getString("_sessionResultTally", "")
        assertEquals(true, tally.contains("Won the fight:2"))
    }

    @Test
    fun phase966_sessionAdvSync_increments() {
        val p = prefs()
        SessionAdvSync.incrementAdventures(p)
        SessionAdvSync.incrementAdventures(p, 2)
        assertEquals(3, p.getInt("_sessionAdventuresUsed", 0))
    }

    @Test
    fun trackK_registrationAnchor() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(my_session_adv());"))
    }
}
