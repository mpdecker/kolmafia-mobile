package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

class QuantumTerrariumSyncTest {

    @Test
    fun parseResponse_currentFamiliar_needsStatusRefreshWhenIdDiffers() {
        val html = CURRENT_AND_NEXT_HTML
        val state = CharacterState(familiarId = 1, turnsPlayed = 500)
        val result = QuantumTerrariumSync.parseResponse(html, null, state)
        assertTrue(result.needsStatusRefresh)
    }

    @Test
    fun parseResponse_currentFamiliar_noRefreshWhenIdMatches() {
        val html = CURRENT_AND_NEXT_HTML
        val state = CharacterState(familiarId = 263, turnsPlayed = 500)
        val result = QuantumTerrariumSync.parseResponse(html, null, state)
        assertFalse(result.needsStatusRefresh)
    }

    @Test
    fun parseResponse_nextFamiliar_writesQuantumPrefs() = runBlocking {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val state = CharacterState(turnsPlayed = 1_000)
        QuantumTerrariumSync.parseResponse(CURRENT_AND_NEXT_HTML, prefs, state)

        val expectedName = FamiliarDefinitionDatabase.getById(180)?.name ?: "none"
        assertEquals(expectedName, prefs.getString("nextQuantumFamiliar"))
        assertEquals("Old Elizabeth", prefs.getString("nextQuantumFamiliarName"))
        assertEquals("crusader06", prefs.getString("nextQuantumFamiliarOwner"))
        assertEquals(292_033, prefs.getInt("nextQuantumFamiliarOwnerId"))
        assertEquals(1_003, prefs.getInt("nextQuantumFamiliarTurn"))
    }

    @Test
    fun parseResponse_realignmentCooldown_writesNextAlignmentPref() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(turnsPlayed = 250)
        val html = """
            $CURRENT_AND_NEXT_HTML
            You will be able to align the quanta again in <b>5 adventures.</b>
        """.trimIndent()
        QuantumTerrariumSync.parseResponse(html, prefs, state)
        assertEquals(255, prefs.getInt("_nextQuantumAlignment"))
    }

    @Test
    fun parseResponse_missingRealignment_defaultsToTurnsMinusOne() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(turnsPlayed = 250)
        QuantumTerrariumSync.parseResponse(CURRENT_AND_NEXT_HTML, prefs, state)
        assertEquals(249, prefs.getInt("_nextQuantumAlignment"))
    }

    @Test
    fun parseResponse_nextFamiliar_startsQuantumFamiliarCounter() = runBlocking {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val state = CharacterState(currentRun = 900, turnsPlayed = 1_000)
        QuantumTerrariumSync.parseResponse(CURRENT_AND_NEXT_HTML, prefs, state)

        val entry = TurnCounter.findByLabel(prefs, QuantumTerrariumSync.FAMILIAR_COUNTER)
        assertEquals(903, entry?.absoluteTurn)
        assertEquals("smguy.gif", entry?.image)
    }

    @Test
    fun parseResponse_realignment_startsCooldownCounter() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(currentRun = 100, turnsPlayed = 250)
        val html = """
            $CURRENT_AND_NEXT_HTML
            You will be able to align the quanta again in <b>5 adventures.</b>
        """.trimIndent()
        QuantumTerrariumSync.parseResponse(html, prefs, state)

        val entry = TurnCounter.findByLabel(prefs, QuantumTerrariumSync.COOLDOWN_COUNTER)
        assertEquals(105, entry?.absoluteTurn)
        assertEquals("quantum.gif", entry?.image)
    }

    @Test
    fun parseResponse_forceAlign_detected() {
        val html = """
            $CURRENT_AND_NEXT_HTML
            arranging the quanta to force your desired future
        """.trimIndent()
        val result = QuantumTerrariumSync.parseResponse(html, null, CharacterState())
        assertTrue(result.forcedAlign)
    }

    companion object {
        private val CURRENT_AND_NEXT_HTML = """
            <i>Your Current Familiar</i><br /><img onClick='fam(263)'
            src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/pokefam49.gif" width=30 height=30
            border=0><br><b>Trubastian</b><br><a href=showplayer.php?who=202148>spOOnge</a>'s Bowlet<br /><br />
            <i>Your Familiar in <b>3</b> Adventures</i><br /><img onClick='fam(180)'
            src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/smguy.gif" width=30 height=30
            border=0><br><b>Old Elizabeth</b><br><a href=showplayer.php?who=292033>crusader06</a>'s Miniature Sword & Martini Guy<br /><br />
        """.trimIndent()
    }
}
