package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PyramidCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP567Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun upperChamber_opensMiddle() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PyramidCombatSync.applyChamberProgress(
                questDatabase = db,
                preferences = prefs,
                adventureId = "406",
                responseText = "Down Dooby-Doo Down Down",
            ),
        )
        assertEquals("step1", prefs.getString(Quest.PYRAMID.prefKey, ""))
        assertTrue(prefs.getBoolean("middleChamberUnlock", false))
    }

    @Test
    fun middleChamber_opensLower() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PyramidCombatSync.applyChamberProgress(
                questDatabase = db,
                preferences = prefs,
                adventureId = "407",
                responseText = "Further Down Dooby-Doo Down Down",
            ),
        )
        assertEquals("step2", prefs.getString(Quest.PYRAMID.prefKey, ""))
        assertTrue(prefs.getBoolean("lowerChamberUnlock", false))
    }

    @Test
    fun middleChamber_underControl_opensControlRoom() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PyramidCombatSync.applyChamberProgress(
                questDatabase = db,
                preferences = prefs,
                adventureId = "407",
                responseText = "Under Control",
            ),
        )
        assertEquals("step3", prefs.getString(Quest.PYRAMID.prefKey, ""))
        assertTrue(prefs.getBoolean("controlRoomUnlock", false))
        assertEquals(1, prefs.getInt("pyramidPosition", 0))
    }
}
