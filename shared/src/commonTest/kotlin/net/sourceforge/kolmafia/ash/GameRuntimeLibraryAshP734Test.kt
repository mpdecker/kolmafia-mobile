package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP734Test {

    @Test
    fun decision1_setsForgeUnlockRun() {
        val prefs = Preferences(MapSettings())
        RufusManager(prefs).handleShadowRiftNC(
            choiceId = 1500,
            decision = 1,
            currentRun = 42,
        )
        assertEquals(42, prefs.getInt("lastShadowForgeUnlockAdventure", 0))
        assertFalse(prefs.getBoolean("_shadowForestLooted"))
    }

    @Test
    fun decision3_setsForestLooted() {
        val prefs = Preferences(MapSettings())
        RufusManager(prefs).handleShadowRiftNC(
            choiceId = 1500,
            decision = 3,
            currentRun = 10,
        )
        assertTrue(prefs.getBoolean("_shadowForestLooted"))
        assertEquals(0, prefs.getInt("lastShadowForgeUnlockAdventure", 0))
    }

    @Test
    fun questChoiceRules_wires1500Forge() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1500,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                currentRun = 99,
            ),
        )
        assertEquals(99, prefs.getInt("lastShadowForgeUnlockAdventure", 0))
    }

    @Test
    fun questChoiceRules_wires1500Forest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1500,
                responseText = "",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_shadowForestLooted"))
    }
}
