package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MimicDnaChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP746Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesCounters() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MimicDnaChoiceSync.applyVisit(
                1517,
                "3/11 eggs spawned today and 1/3 donations made for the day",
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt(MimicDnaChoiceSync.OBTAINED_PREF, 0))
        assertEquals(1, prefs.getInt(MimicDnaChoiceSync.DONATED_PREF, 0))
    }

    @Test
    fun donate_consumesEggAndUpdatesMap() {
        val prefs = Preferences(MapSettings())
        prefs.setString(MimicDnaChoiceSync.MONSTERS_PREF, "100:2")
        var consumed = 0
        assertTrue(
            MimicDnaChoiceSync.apply(
                choiceId = 1517,
                decision = 1,
                html = "You donate your egg to science.",
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1517&option=1&mid=100",
                consumeItem = { id, qty ->
                    assertEquals(MimicDnaChoiceSync.MIMIC_EGG, id)
                    consumed = qty
                },
            ),
        )
        assertEquals(1, consumed)
        assertEquals(1, prefs.getInt(MimicDnaChoiceSync.DONATED_PREF, 0))
        assertEquals("100:1", prefs.getString(MimicDnaChoiceSync.MONSTERS_PREF, ""))
    }

    @Test
    fun extract_incrementsObtainedAndMap() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MimicDnaChoiceSync.apply(
                choiceId = 1517,
                decision = 2,
                html = "The sample pops into a backroom",
                preferences = prefs,
                choiceUrl = "mid=55",
            ),
        )
        assertEquals(1, prefs.getInt(MimicDnaChoiceSync.OBTAINED_PREF, 0))
        assertEquals("55:1", prefs.getString(MimicDnaChoiceSync.MONSTERS_PREF, ""))
    }

    @Test
    fun extract_cantExtractCapsObtained() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MimicDnaChoiceSync.apply(
                1517,
                2,
                "You can't extract any more",
                prefs,
            ),
        )
        assertEquals(11, prefs.getInt(MimicDnaChoiceSync.OBTAINED_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1517() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1517,
                responseText = "pops into a backroom",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
                choiceUrl = "mid=9",
            ),
        )
        assertEquals(1, prefs.getInt(MimicDnaChoiceSync.OBTAINED_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(MimicDnaChoiceSync.applyVisit(1410, "eggs spawned", Preferences(MapSettings())))
    }
}
