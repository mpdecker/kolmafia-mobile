package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class DreadScrollManagerTest {

    @Test
    fun handleLibrary1_setsDreadScroll1() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleLibrary(
            "somebody has scrawled &quot;<b>LONELY</b>&quot; on the cover",
            prefs,
            null,
        )
        assertEquals(1, prefs.getInt("dreadScroll1", 0))
    }

    @Test
    fun handleLibrary2_setsDreadScroll6() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleLibrary(
            "there seem to be a lot of references to <b>blind</b> creatures.",
            prefs,
            null,
        )
        assertEquals(1, prefs.getInt("dreadScroll6", 0))
    }

    @Test
    fun handleLibrary3_setsDreadScroll8() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleLibrary(
            "consists of the phrase <b>conjoined triplets</b> over and over",
            prefs,
            null,
        )
        assertEquals(3, prefs.getInt("dreadScroll8", 0))
    }

    @Test
    fun handleHealscroll_setsDreadScroll2() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleHealscroll(
            "a magnificent <b>moonfish</b> smiling warmly",
            prefs,
            null,
        )
        assertEquals(2, prefs.getInt("dreadScroll2", 0))
    }

    @Test
    fun handleKillscroll_setsDreadScroll5() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleKillscroll(
            """recognize one of them: <b>&quot;green&quot;</b>""",
            prefs,
            null,
        )
        assertEquals(3, prefs.getInt("dreadScroll5", 0))
    }

    @Test
    fun handleKnucklebone_setsDreadScroll4() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleKnucklebone(
            "it bounces straight <b>west</b>.",
            prefs,
            null,
        )
        assertEquals(4, prefs.getInt("dreadScroll4", 0))
    }

    @Test
    fun handleDeepDarkVisions_setsDreadScroll3() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleDeepDarkVisions(
            "You close your eyes and let Deep visions wash over you...<b>The House of Pain</b>...itemimages/hp.gif",
            prefs,
            null,
        )
        assertEquals(4, prefs.getInt("dreadScroll3", 0))
    }

    @Test
    fun handleWorktea_setsDreadScroll7AndWorkteaClue() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.handleWorktea(
            "the leaves in the bottom look just like <b>a shark</b>!",
            prefs,
            null,
            null,
        )
        assertEquals(3, prefs.getInt("dreadScroll7", 0))
        assertEquals("a shark", prefs.getString("workteaClue", ""))
    }

    @Test
    fun getScrollText_assemblesKnownClues() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dreadScroll1", 3)
        prefs.setInt("dreadScroll2", 2)
        prefs.setInt("dreadScroll3", 1)
        prefs.setInt("dreadScroll4", 1)
        prefs.setInt("dreadScroll5", 1)
        prefs.setInt("dreadScroll6", 1)
        prefs.setInt("dreadScroll7", 3)
        prefs.setInt("dreadScroll8", 2)

        val text = DreadScrollManager.getScrollText(prefs)
        assertTrue(text.contains("thrice-cursed moonfish is in the House of Cards"))
        assertTrue(text.contains("Northern Current runs as red as blood"))
        assertTrue(text.contains("blind shark births two and twenty stillborn spawn"))
        assertTrue(text.endsWith("the Elder shall awaken. "))
    }

    @Test
    fun getClues_listsAllPrefKeys() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dreadScroll1", 1)
        val clues = DreadScrollManager.getClues(prefs)
        assertTrue(clues.contains("dreadScroll1 (Mer-kin Library 1): 1 (LONELY)"))
        assertTrue(clues.contains("dreadScroll8 (Mer-kin Library 3): 0 (unknown)"))
    }

    @Test
    fun recordFailure_appendsGuessWithDurationThird() {
        val prefs = Preferences(MapSettings())
        val url =
            "choice.php?whichchoice=703&pro1=a&pro2=b&pro3=c&pro4=d&pro5=e&pro6=f&pro7=g&pro8=h"
        DreadScrollManager.recordFailure(
            url,
            "You fail. (duration: 9 Adventures)",
            prefs,
        )
        assertEquals("abcdefgh:3", prefs.getString("dreadScrollGuesses", ""))
    }

    @Test
    fun recordFailure_appendsCommaSeparatedGuesses() {
        val prefs = Preferences(MapSettings())
        prefs.setString("dreadScrollGuesses", "first:1")
        val url =
            "choice.php?whichchoice=703&pro1=1&pro2=2&pro3=3&pro4=4&pro5=5&pro6=6&pro7=7&pro8=8"
        DreadScrollManager.recordFailure(
            url,
            "(3 Adventures)",
            prefs,
        )
        assertEquals("first:1,12345678:1", prefs.getString("dreadScrollGuesses", ""))
    }

    @Test
    fun handleHighPriestSuccess_setsPrefsAndEmitsConsume() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.handleHighPriestSuccess(
            "I guess you're the Mer-kin High Priest now. Cool!",
            prefs,
            bus,
            null,
        )

        assertTrue(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
        assertEquals(1, consumed.size)
        assertEquals(DreadScrollManager.DREADSCROLL_ID, consumed.single().itemId)
        assertEquals(1, consumed.single().quantity)
    }

    @Test
    fun handleHighPriestSuccess_noMatch_noOp() {
        val prefs = Preferences(MapSettings())
        prefs.setString("merkinQuestPath", "none")

        DreadScrollManager.handleHighPriestSuccess(
            "The prophecy does not match.",
            prefs,
            null,
            null,
        )

        assertFalse(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("none", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun recordFailure_stillRunsOnNonSuccessChoice703() {
        val prefs = Preferences(MapSettings())
        val url =
            "choice.php?whichchoice=703&pro1=a&pro2=b&pro3=c&pro4=d&pro5=e&pro6=f&pro7=g&pro8=h"
        DreadScrollManager.recordFailure(
            url,
            "Wrong prophecy. (duration: 6 Adventures)",
            prefs,
        )
        assertEquals("abcdefgh:2", prefs.getString("dreadScrollGuesses", ""))
    }

    @Test
    fun applyFromResponse_choice703Success_setsHighPriestPrefs() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.applyFromResponse(
            url = "choice.php?whichchoice=703&pro1=a&pro2=b&pro3=c&pro4=d&pro5=e&pro6=f&pro7=g&pro8=h",
            html = "I guess you're the Mer-kin High Priest now. Cool!",
            preferences = prefs,
            sessionLogger = null,
            eventBus = bus,
        )

        assertTrue(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
        assertEquals(1, consumed.size)
    }

    @Test
    fun applyFromResponse_routesChoice704ToLibrary() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.applyFromResponse(
            url = "choice.php?whichchoice=704&option=1",
            html = "somebody has scrawled &quot;<b>DOUBLED</b>&quot;",
            preferences = prefs,
            sessionLogger = null,
            eventBus = null,
        )
        assertEquals(2, prefs.getInt("dreadScroll1", 0))
    }

    @Test
    fun applyFromResponse_routesFightHtmlToKillAndHealScroll() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.applyFromResponse(
            url = "fight.php",
            html = """
                You're fighting a monster.
                a magnificent <b>sunfish</b> in the distance
                recognize one of them: <b>&quot;yellow&quot;</b>
            """.trimIndent(),
            preferences = prefs,
            sessionLogger = null,
            eventBus = null,
        )
        assertEquals(3, prefs.getInt("dreadScroll2", 0))
        assertEquals(4, prefs.getInt("dreadScroll5", 0))
    }

    @Test
    fun applyFromResponse_itemIdHintHandlesKnucklebone() {
        val prefs = Preferences(MapSettings())
        DreadScrollManager.applyFromResponse(
            url = null,
            html = "it bounces straight <b>south</b>.",
            preferences = prefs,
            sessionLogger = null,
            eventBus = null,
            itemIdHint = DreadScrollManager.KNUCKLEBONE_ID,
        )
        assertEquals(2, prefs.getInt("dreadScroll4", 0))
    }

    @Test
    fun handleGladiatorChampionSuccess_setsPrefsAndEmitsConsume() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.handleGladiatorChampionSuccess(
            "The sigil burned into your forehead flares up in intense pain.",
            prefs,
            bus,
            null,
        )

        assertTrue(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("gladiator", prefs.getString("merkinQuestPath", ""))
        assertEquals(15, prefs.getInt("lastColosseumRoundWon", 0))
        assertEquals(1, consumed.size)
        assertEquals(DreadScrollManager.DREADSCROLL_ID, consumed.single().itemId)
    }

    @Test
    fun handleGladiatorChampionSuccess_skipsWhenQuestPathDone() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        prefs.setString("merkinQuestPath", "done")
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.handleGladiatorChampionSuccess(
            "The sigil burned into your forehead flares up in intense pain.",
            prefs,
            bus,
            null,
        )

        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
        assertEquals(0, prefs.getInt("lastColosseumRoundWon", 0))
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun handleGladiatorChampionSuccess_noMatch_noOp() {
        val prefs = Preferences(MapSettings())

        DreadScrollManager.handleGladiatorChampionSuccess(
            "You read the scroll but nothing happens.",
            prefs,
            null,
            null,
        )

        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("none", prefs.getString("merkinQuestPath", "none"))
    }

    @Test
    fun parseDreadscrollUse_routesGladiatorWhenNotHighPriest() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.parseDreadscrollUse(
            "The sigil burned into your forehead curls out and burns the scroll to ashes.",
            prefs,
            bus,
            null,
        )

        assertTrue(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("gladiator", prefs.getString("merkinQuestPath", ""))
        assertEquals(1, consumed.size)
    }

    @Test
    fun parseDreadscrollUse_highPriestTakesPrecedence() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        DreadScrollManager.parseDreadscrollUse(
            """
                I guess you're the Mer-kin High Priest now. Cool!
                The sigil burned into your forehead also flares up.
            """.trimIndent(),
            prefs,
            bus,
            null,
        )

        assertTrue(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals(1, consumed.size)
    }
}
