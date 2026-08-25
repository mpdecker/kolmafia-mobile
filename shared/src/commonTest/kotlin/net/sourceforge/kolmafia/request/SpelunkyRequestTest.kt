package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class SpelunkyRequestTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun resetClearsPrefs() {
        prefs.setInt("spelunkyWinCount", 5)
        prefs.setString("spelunkyStatus", "Jungle")
        SpelunkyRequest.reset(prefs)
        assertEquals(0, prefs.getInt("spelunkyWinCount", -1))
        assertEquals("", prefs.getString("spelunkyStatus", "x"))
    }

    @Test
    fun wonFightUnlocksJungle() {
        SpelunkyRequest.wonFight(
            "shopkeeper",
            "New Area Unlocked<br>The Jungle",
            prefs,
        )
        // shopkeeper does not increment win count
        assertEquals(0, prefs.getInt("spelunkyWinCount", 0))
        assertTrue(prefs.getString("spelunkyStatus", "").contains("Jungle"))
    }

    @Test
    fun winCountCyclesAtSix() {
        prefs.setInt("spelunkyWinCount", 5)
        prefs.setInt("spelunkyNextNoncombat", 1)
        SpelunkyRequest.incrementWinCount(prefs)
        assertEquals(3, prefs.getInt("spelunkyWinCount", 0))
        assertEquals(2, prefs.getInt("spelunkyNextNoncombat", 0))
    }

    @Test
    fun upgradeSetsYFlag() {
        prefs.setString("spelunkyUpgrades", "NNNNNNNNN")
        SpelunkyRequest.upgrade(1, prefs)
        assertEquals("YNNNNNNNN", prefs.getString("spelunkyUpgrades", ""))
    }

    @Test
    fun parseChoiceTrapUnlock() {
        SpelunkyRequest.parseChoice(
            1030,
            "You find The Spider Hole",
            1,
            prefs,
        )
        assertTrue(prefs.getString("spelunkyStatus", "").contains("Spider Hole"))
    }

    @Test
    fun parseCharpaneGoldAndTurns() {
        val html = """
            <a>>Last Spelunk</a>
            HP:<b>10/20</b>
            >12 turns left
            >1,234 gold
            >3 bombs
            >2 ropes
            >1 keys
            Buddy:<b>Yomama</b>
        """.trimIndent()
        assertTrue(SpelunkyRequest.parseCharpane(html, prefs))
        assertEquals(1234, SpelunkyRequest.getGold(prefs))
        assertEquals(12, SpelunkyRequest.getTurnsLeft(prefs))
        assertEquals(3, SpelunkyRequest.getBombs(prefs))
        assertEquals("Yomama", SpelunkyRequest.getBuddyName(prefs))
    }
}
