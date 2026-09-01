package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SpecimenBenchChoiceSync

class GameRuntimeLibraryAshP782Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesCount() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpecimenBenchChoiceSync.applyVisit(
                1555,
                " You have done so 4 times today.",
                prefs,
            ),
        )
        assertEquals(4, prefs.getInt("zootSpecimensPrepared", 0))
    }

    @Test
    fun post_incrementsWithXpCallback() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("zootSpecimensPrepared", 2)
        var xp = 0
        assertTrue(
            SpecimenBenchChoiceSync.apply(
                choiceId = 1555,
                decision = 1,
                html = "You inject the viscous liquid into your familiar.",
                preferences = prefs,
                addFamiliarNonCombatExperience = { xp += it },
            ),
        )
        assertEquals(3, prefs.getInt("zootSpecimensPrepared", 0))
        assertEquals(20, xp)
    }

    @Test
    fun questChoiceRules_wires1555() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1555,
                responseText = "You inject the viscous liquid",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("zootSpecimensPrepared", 0))
    }
}
