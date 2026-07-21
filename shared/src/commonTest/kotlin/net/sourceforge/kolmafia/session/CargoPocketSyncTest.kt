package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class CargoPocketSyncTest {

    private val scrapPockets = listOf(373, 222, 7, 602, 172, 251, 282)

    @AfterTest
    fun tearDown() {
        CultShortsDatabase.resetForTest()
    }

    private fun sync(): CargoPocketSync {
        CultShortsDatabase.injectForTest(scrapPockets)
        val prefs = Preferences(MapSettings())
        return CargoPocketSync(prefs, YegDemonNameSync(prefs))
    }

    @Test
    fun loadAndSave_roundTrip() {
        val s = sync()
        s.parsePocketPick(7, "success")
        s.loadPockets()
        assertEquals(setOf(7), s.pickedPocketIds())
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CARGO_POCKETS_EMPTIED, "1,2,3")
        val reloaded = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        assertEquals(setOf(1, 2, 3), reloaded.pickedPocketIds())
    }

    @Test
    fun parseAvailablePockets_infersEmptiedPockets() {
        val s = sync()
        val html = buildString {
            append("There appear to be 666 pockets on these shorts.")
            append("""<form method="post" action="choice.php" style="display: inline"><input name="pocket" value="5"></form>""")
            append("""<form method="post" action="choice.php" style="display: inline"><input name="pocket" value="10"></form>""")
        }
        s.parseAvailablePockets(html)
        val picked = s.pickedPocketIds()
        assertTrue(4 in picked)
        assertTrue(9 in picked)
        assertFalse(5 in picked)
        assertFalse(10 in picked)
        assertTrue(666 in picked)
    }

    @Test
    fun parsePocketPick_addsPocketAndDailyFlag() {
        val prefs = Preferences(MapSettings())
        CultShortsDatabase.injectForTest(scrapPockets)
        val s = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        s.parsePocketPick(
            373,
            """This pocket contains a scrap of paper that reads: <b>XTNQ: Ga</b>""",
        )
        assertEquals(setOf(373), s.pickedPocketIds())
        assertTrue(prefs.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false))
        assertEquals(mapOf(373 to "Ga"), s.let { YegDemonNameSync(prefs).knownScrapPockets() })
    }

    @Test
    fun parsePocketPick_marksDailyExhausted() {
        val prefs = Preferences(MapSettings())
        CultShortsDatabase.injectForTest(scrapPockets)
        val s = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        s.parsePocketPick(7, "It seems like the power of the pockets has been exhausted for the day.")
        assertTrue(prefs.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false))
    }

    @Test
    fun parsePocketPickFromUrl_extractsPocket() {
        val s = sync()
        s.parsePocketPickFromUrl(
            "choice.php?whichchoice=1420&option=1&pocket=373",
            """This pocket contains a scrap of paper that reads: <b>XTNQ: Ga</b>""",
        )
        assertEquals(setOf(373), s.pickedPocketIds())
    }

    @Test
    fun registerPocketFight_marksPocketAndDailyFlag() {
        val prefs = Preferences(MapSettings())
        val s = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        s.registerPocketFight("choice.php?whichchoice=1420&option=1&pocket=30")
        assertEquals(setOf(30), s.pickedPocketIds())
        assertTrue(prefs.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false))
    }

    @Test
    fun extractMeatNote_parsesBlockquote() {
        val s = sync()
        val note = s.extractMeatNote(
            """You pull a note out of your pocket.  It's wrapped around a pile of meat.<blockquote style='border: 1px solid black'>Being at the level of the narrowest part of the torso</blockquote>""",
        )
        assertEquals("Being at the level of the narrowest part of the torso", note)
    }

    @Test
    fun extractPocketFromUrl_returnsZeroWhenMissing() {
        assertEquals(0, CargoPocketSync.extractPocketFromUrl("choice.php?whichchoice=1420"))
        assertEquals(373, CargoPocketSync.extractPocketFromUrl("choice.php?pocket=373&option=1"))
    }
}
