package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DripHallChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP743Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun door1_orbIncrements() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DripHallChoiceSync.apply(
                choiceId = 1411,
                decision = 1,
                html = "You find a drippy orb!",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_drippingHallDoor1"))
        assertEquals(1, prefs.getInt(DripHallChoiceSync.ORBS_PREF, 0))
        assertEquals(12, prefs.getInt(DripHallChoiceSync.ADVENTURES_PREF, 0))
    }

    @Test
    fun door4_consumesStein() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(
            DripHallChoiceSync.apply(
                choiceId = 1411,
                decision = 4,
                html = "You acquire drippy pilsner",
                preferences = prefs,
                consumeItem = { id, qty ->
                    assertEquals(DripHallChoiceSync.DRIPPY_STEIN, id)
                    consumed = qty
                },
            ),
        )
        assertTrue(prefs.getBoolean("_drippingHallDoor4"))
        assertEquals(1, consumed)
    }

    @Test
    fun door1_poolSkillFloor() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DripHallChoiceSync.apply(
                choiceId = 1411,
                decision = 1,
                html = "No orb this time",
                preferences = prefs,
                estimatedPoolSkill = 45,
            ),
        )
        assertEquals(2, prefs.getInt(DripHallChoiceSync.ORBS_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1411() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1411,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_drippingHallDoor2"))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            DripHallChoiceSync.apply(1219, 1, "drippy orb", prefs),
        )
    }
}
