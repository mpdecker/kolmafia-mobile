package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.StalagmiteChoiceSync

class GameRuntimeLibraryAshP749Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_marksUsed() {
        val prefs = Preferences(MapSettings())
        assertTrue(StalagmiteChoiceSync.applyVisit(1491, prefs))
        assertTrue(prefs.getBoolean(StalagmiteChoiceSync.USED_PREF))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(StalagmiteChoiceSync.applyVisit(1176, Preferences(MapSettings())))
    }
}
