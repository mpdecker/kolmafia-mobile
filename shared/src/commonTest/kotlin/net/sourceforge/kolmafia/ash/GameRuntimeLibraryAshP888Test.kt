package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP888Test {
    @Test
    fun revisionAndChoiceRulesCorpusSweep() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
        val prefs = Preferences(MapSettings())
        assertTrue(QuestChoiceRules.apply(835, "", QuestDatabase(prefs), decision = 1, preferences = prefs))
        assertTrue(QuestChoiceRules.apply(1089, "You acquire", QuestDatabase(prefs), decision = 2, preferences = prefs))
        assertTrue(prefs.getBoolean("_grimBuff", false))
        assertEquals("Feed The Children", prefs.getString("csServicesPerformed", ""))
    }
}
