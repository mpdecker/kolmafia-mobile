package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PantogramChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP770Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun parse_writesModifierString() {
        val prefs = Preferences(MapSettings())
        var gained = 0
        assertTrue(
            PantogramChoiceSync.apply(
                choiceId = 1270,
                html = "You acquire an item: pants",
                preferences = prefs,
                choiceUrl = "whichchoice=1270&m=1&e=2&s1=-1&s2=-2&s3=-1",
                gainItem = { id, qty -> if (id == 9574) gained += qty },
            ),
        )
        assertEquals(1, gained)
        val mods = prefs.getString("_pantogramModifier", "")
        assertTrue(mods.contains("Muscle: +10"))
        assertTrue(mods.contains("Cold Resistance: +2"))
        assertTrue(mods.contains("Maximum HP: +40"))
        assertTrue(mods.contains("Spell Damage Percent: +20"))
        assertTrue(mods.contains("Combat Rate: -5"))
        assertTrue(mods.contains("Lasts Until Rollover"))
    }

    @Test
    fun questChoiceRules_wires1270() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1270,
                responseText = "You acquire an item",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "m=3&e=1&s1=-2&s2=-1&s3=-2",
            ),
        )
        assertTrue(prefs.getString("_pantogramModifier", "").contains("Moxie: +10"))
    }
}
