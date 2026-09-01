package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.session.PvpManager
import net.sourceforge.kolmafia.session.SessionLogger

class GameRuntimeLibraryAshP721Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun character(): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(name = "Hero", pvpfights = "4", hippystone = "1"),
        )
    }

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun invalidTarget_printsAndDoesNotAbort() {
        val printed = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PeeVPeeSync.apply(
            html = "<tr><td>You may not attack players who are in Hardcore",
            url = "peevpee.php?place=fight&action=fight",
            character = character(),
            preferences = prefs,
            sessionLogger = logger,
            print = { printed += it },
        )
        assertEquals(listOf("Invalid target"), printed)
        assertNull(PvpManager.abortReason)
        assertFalse(PvpManager.noFight)
    }

    @Test
    fun winHtml_printsChallengeLine() {
        val printed = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Hero</b> won the fight, <b>8</b> to <b>3</b>!
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = character(),
            preferences = prefs,
            sessionLogger = logger,
            print = { printed += it },
        )
        assertTrue(printed.any { it.contains("You challenged Villain and won the PvP fight, 8 to 3!") })
        assertTrue(
            logger.recentLines().any { it.contains("You challenged Villain and won the PvP fight, 8 to 3!") },
        )
    }
}
