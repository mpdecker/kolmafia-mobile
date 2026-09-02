package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class VolcanoIslandRequestTest {

    private fun logger(): SessionLogger =
        SessionLogger(Preferences(MapSettings()), GameEventBus())

    @Test
    fun parseResponse_incrementsSlimeHarvestPrefForSauceror() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(characterClass = CharacterClass.SAUCEROR.id)
        VolcanoIslandRequest.parseResponse(
            url = "volcanoisland.php?action=npc&subaction=getslime",
            html = "You ladle some slime out of one of the drums.",
            state = state,
            preferences = prefs,
            equipmentManager = null,
        )
        assertEquals(1, prefs.getInt("_slimeVialsHarvested", 0))
    }

    @Test
    fun parseResponse_doesNotIncrementSlimeHarvestForNonSauceror() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(characterClass = CharacterClass.PASTAMANCER.id)
        VolcanoIslandRequest.parseResponse(
            url = "volcanoisland.php?action=npc&subaction=getslime",
            html = "You ladle some slime out of one of the drums.",
            state = state,
            preferences = prefs,
            equipmentManager = null,
        )
        assertEquals(0, prefs.getInt("_slimeVialsHarvested", 0))
    }

    @Test
    fun registerRequest_logsNpcVisitForPastamancer() {
        val sessionLogger = logger()
        val state = CharacterState(characterClass = CharacterClass.PASTAMANCER.id)
        val logged = VolcanoIslandRequest.registerRequest(
            url = "volcanoisland.php?action=npc",
            sessionLogger = sessionLogger,
            state = state,
            adventureCount = 42,
        )
        assertTrue(logged)
        assertTrue(sessionLogger.recentLines().any { it.contains("Protestor") })
    }

    @Test
    fun registerRequest_logsSlimeHarvestForSauceror() {
        val sessionLogger = logger()
        val state = CharacterState(characterClass = CharacterClass.SAUCEROR.id)
        val logged = VolcanoIslandRequest.registerRequest(
            url = "volcanoisland.php?action=npc&subaction=getslime",
            sessionLogger = sessionLogger,
            state = state,
            adventureCount = 7,
        )
        assertTrue(logged)
        assertTrue(sessionLogger.recentLines().any { it.contains("[7] Volcano Island (Drums of Slime)") })
    }

    @Test
    fun getAdventuresUsed_matchesDesktopTubaAction() {
        assertEquals(1, VolcanoIslandRequest.getAdventuresUsed("volcanoisland.php?action=tuba"))
        assertEquals(0, VolcanoIslandRequest.getAdventuresUsed("volcanoisland.php?action=tniat"))
    }

    @Test
    fun npcName_matchesDesktopClassMapping() {
        assertEquals("a Protestor", VolcanoIslandRequest.npcName(CharacterClass.PASTAMANCER))
        assertEquals("a Boat", VolcanoIslandRequest.npcName(CharacterClass.SAUCEROR))
    }
}
