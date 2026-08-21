package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WildfireNpcChoiceSync

class GameRuntimeLibraryAshP758Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sprinkler_raindropSetsPref() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            WildfireNpcChoiceSync.apply(1452, 1, """<img src="raindrop.gif">""", prefs),
        )
        assertTrue(prefs.getBoolean("wildfireSprinkled"))
    }

    @Test
    fun fracker_thanksText() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            WildfireNpcChoiceSync.apply(1453, 0, "Thanks for the help!", prefs),
        )
        assertTrue(prefs.getBoolean("wildfireFracked"))
    }

    @Test
    fun cropster_questChoiceRules() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1454,
                responseText = "raindrop.gif",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("wildfireDusted"))
    }

    @Test
    fun ignoresWrongDecisionWithoutThanks() {
        assertFalse(
            WildfireNpcChoiceSync.apply(1452, 2, "nope", Preferences(MapSettings())),
        )
    }
}
