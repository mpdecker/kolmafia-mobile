package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class YegDemonNameSyncTest {

    private val scrapPockets = listOf(373, 222, 7, 602, 172, 251, 282)

    @AfterTest
    fun tearDown() {
        CultShortsDatabase.resetForTest()
    }

    private fun sync(): YegDemonNameSync {
        CultShortsDatabase.injectForTest(scrapPockets)
        return YegDemonNameSync(Preferences(MapSettings()))
    }

    private fun scrapHtml(code: String, syllable: String): String =
        """This pocket contains a scrap of paper that reads: <b>$code: $syllable</b>"""

    @Test
    fun parsePocketPick_extractsSyllable() {
        val s = sync()
        s.parsePocketPick(373, scrapHtml("XTNQ", "Ga"))
        assertEquals(mapOf(373 to "Ga"), s.knownScrapPockets())
    }

    @Test
    fun parsePocketPick_skipsWaterloggedScrap() {
        val s = sync()
        val html =
            """This pocket contains a waterlogged scrap of paper that reads: <b>QDL XLR KVSJGGJV QRGL</b>"""
        s.parsePocketPick(373, html)
        assertTrue(s.knownScrapPockets().isEmpty())
    }

    @Test
    fun knownScrapPockets_legacyThreePartEntry() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CARGO_POCKET_SCRAPS, "7:ESUQQ: Go")
        CultShortsDatabase.injectForTest(scrapPockets)
        val s = YegDemonNameSync(prefs)
        assertEquals(mapOf(7 to "Go"), s.knownScrapPockets())
    }

    @Test
    fun saveScrapPockets_preservesScrapOrder() {
        val s = sync()
        s.saveScrapPockets(
            mapOf(
                282 to "last",
                373 to "first",
                7 to "mid",
            ),
        )
        assertEquals(
            listOf(373 to "first", 7 to "mid", 282 to "last"),
            s.knownScrapPockets().toList().sortedBy { scrapPockets.indexOf(it.first) },
        )
    }

    @Test
    fun updateYegName_requiresAllSevenSyllables() {
        val s = sync()
        s.updateYegName(mapOf(373 to "A", 222 to "B"))
        assertEquals("", s.demonName())
    }

    @Test
    fun updateYegName_assemblesNameAndReplacesUnderscores() {
        val s = sync()
        val syllables = mapOf(
            373 to "Yeg",
            222 to "the",
            7 to "Eld",
            602 to "ritch",
            172 to "One",
            251 to "True",
            282 to "Name",
        )
        s.updateYegName(syllables)
        assertEquals("YegtheEldritchOneTrueName", s.demonName())

        val underscored = syllables.toMutableMap()
        underscored[282] = "True_Name"
        s.updateYegName(underscored)
        assertEquals("YegtheEldritchOneTrueTrue Name", s.demonName())
    }

    @Test
    fun parsePocketPick_allSevenSetsDemonName13() {
        val s = sync()
        val syllables = listOf("Yeg", "the", "Eld", "ritch", "One", "True", "Name")
        scrapPockets.forEachIndexed { index, pocket ->
            s.parsePocketPick(pocket, scrapHtml("X$index", syllables[index]))
        }
        assertEquals("YegtheEldritchOneTrueName", s.demonName())
    }

    @Test
    fun parsePocketPickFromUrl_extractsPocket() {
        val s = sync()
        s.parsePocketPickFromUrl(
            "choice.php?whichchoice=1420&option=1&pocket=373",
            scrapHtml("XTNQ", "Ga"),
        )
        assertEquals("Ga", s.knownScrapPockets()[373])
    }

    @Test
    fun parsePocketPick_skipsEmptyPocketResponse() {
        val s = sync()
        s.parsePocketPick(373, "That pocket is empty.")
        assertTrue(s.knownScrapPockets().isEmpty())
    }

    @Test
    fun parsePocketPick_marksDailyExhausted() {
        val prefs = Preferences(MapSettings())
        CultShortsDatabase.injectForTest(scrapPockets)
        val s = YegDemonNameSync(prefs)
        s.parsePocketPick(7, "It seems like the power of the pockets has been exhausted for the day.")
        assertTrue(prefs.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false))
    }

    @Test
    fun extractPocketFromUrl_returnsZeroWhenMissing() {
        assertEquals(0, YegDemonNameSync.extractPocketFromUrl("choice.php?whichchoice=1420"))
        assertEquals(373, YegDemonNameSync.extractPocketFromUrl("choice.php?pocket=373&option=1"))
    }
}
