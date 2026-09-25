package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UseItemRegisterLogTest {

    private val names = mapOf(
        223 to "orange",
        100 to "meat paste",
        4908 to "Loathing Legion knife",
        4926 to "Loathing Legion universal screwdriver",
        5285 to "d4",
        5288 to "d10",
        5511 to "What card",
        5648 to "Boris's Helm",
        5650 to "Boris's Helm (askew)",
        8568 to "little firkin",
        8866 to "Heinz beans",
        3162 to "El Vibrato helmet",
        55 to "battery",
        3146 to "punchcard (attack)",
    )

    private fun name(id: Int) = names[id].orEmpty()

    @Test
    fun fold_logsTheNewFormAndSkipsThePicker() {
        val picker = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=4908&switch=1",
            4908, 1, ::name, 0, 0,
        )
        assertIs<UseItemRegisterLog.Outcome.Skip>(picker)
        val folded = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=4908&switch=1&fold=4915",
            4908, 1, ::name, 0, 0,
        )
        assertEquals(
            "fold Loathing Legion knife",
            (folded as UseItemRegisterLog.Outcome.Line).text,
        )
    }

    @Test
    fun dice_rollPercentileForTwoD10s() {
        val percentile = UseItemRegisterLog.describe("inv_use.php?whichitem=5288&quantity=2", 5288, 2, ::name, 0, 0)
        assertEquals("roll percentile dice", (percentile as UseItemRegisterLog.Outcome.Line).text)
        val d4 = UseItemRegisterLog.describe("inv_use.php?whichitem=5285&quantity=3", 5285, 3, ::name, 0, 0)
        assertEquals("roll 3d4", (d4 as UseItemRegisterLog.Outcome.Line).text)
    }

    @Test
    fun barrel_partyOrSmash() {
        val party = UseItemRegisterLog.describe("inv_use.php?whichitem=8568&choice=1", 8568, 1, ::name, 0, 0)
        assertEquals("Throw a barrel smashing party!", (party as UseItemRegisterLog.Outcome.Line).text)
        val smash = UseItemRegisterLog.describe("inv_use.php?whichitem=8568", 8568, 1, ::name, 0, 0)
        assertEquals("smash little firkin", (smash as UseItemRegisterLog.Outcome.Line).text)
    }

    @Test
    fun trivia_skipsTheQuestionPage() {
        val question = UseItemRegisterLog.describe("inv_use.php?whichitem=5511", 5511, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Skip>(question)
        val answer = UseItemRegisterLog.describe("inv_use.php?whichitem=5511&answerplz=1", 5511, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Fallback>(answer)
    }

    @Test
    fun bricko_logsOnlyTheCheckedUse() {
        val first = UseItemRegisterLog.describe("inv_use.php?whichitem=4474", 4474, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Skip>(first)
        val second = UseItemRegisterLog.describe("inv_use.php?whichitem=4474&checked=1", 4474, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Fallback>(second)
    }

    @Test
    fun jacking_insertsTheFruit() {
        val line = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=4560&action=addfruit&whichfruit=223",
            4560, 1, ::name, 0, 0,
        )
        assertEquals(
            "insert orange into pneumatic tube interface",
            (line as UseItemRegisterLog.Outcome.Line).text,
        )
    }

    @Test
    fun moon_tunesToTheSign() {
        val line = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=10254&whichsign=2&doit=96",
            10254, 1, ::name, 0, 0,
        )
        assertEquals("tuning moon to The Wallaby", (line as UseItemRegisterLog.Outcome.Line).text)
    }

    @Test
    fun screwdriver_unscrewAll() {
        val line = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=4926&action=screw&dowhichitem=100&untinkerall=on",
            4926, 1, ::name, 0, 0,
        )
        assertEquals("unscrew * meat paste", (line as UseItemRegisterLog.Outcome.Line).text)
    }

    @Test
    fun helmet_insertsTheHelper() {
        val missing = UseItemRegisterLog.describe("inv_use.php?whichitem=3162", 3162, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Skip>(missing)
        val line = UseItemRegisterLog.describe(
            "inv_use.php?whichitem=3162&utensil=55",
            3162, 1, ::name, 0, 0,
        )
        assertEquals("insert battery into El Vibrato helmet", (line as UseItemRegisterLog.Outcome.Line).text)
    }

    @Test
    fun punchcard_requiresTheMegadrone() {
        val idle = UseItemRegisterLog.describe("inv_use.php?whichitem=3146", 3146, 1, ::name, 0, 0)
        assertIs<UseItemRegisterLog.Outcome.Skip>(idle)
        val line = UseItemRegisterLog.describe("inv_use.php?whichitem=3146", 3146, 1, ::name, 0, 81)
        assertEquals(
            "insert punchcard (attack) into El Vibrato Megadrone",
            (line as UseItemRegisterLog.Outcome.Line).text,
        )
    }

    @Test
    fun reflection_marksTheLocationLogged() {
        AdventureSession.locationLogged = false
        RequestLogger.currentRound = { 0 }
        val prefs = Preferences(MapSettings())
        prefs.setInt("turnsPlayed", 40)
        val logger = SessionLogger(prefs, GameEventBus())
        val claimed = RequestLogger.registerRequest(
            "inv_use.php?whichitem=4509",
            logger,
            prefs,
        )
        assertTrue(claimed)
        assertTrue(AdventureSession.locationLogged)
        assertTrue(logger.recentLines().any { it.contains("[40] Reflection of a Map") })
    }

    @Test
    fun twistHorns_logsTheHelmSwap() {
        RequestLogger.equippedItemId = { slot -> if (slot == "hat") 5648 else -1 }
        val line = UseItemRegisterLog.equipped("inv_use.php?action=twisthorns&slot=hat", RequestLogger.equippedItemId, ::name)
        assertEquals("Twisted Boris's Helm into Boris's Helm (askew)", line?.text)
        assertEquals(5650, line?.newItemId)
        assertFalse(line?.discardPrevious == true)
    }
}
