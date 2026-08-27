package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BarelyTalesChoiceSync

class GameRuntimeLibraryAshP882Test {
    @Test
    fun barelyTalesOnlyMarksChosenBuff() {
        val prefs = Preferences(MapSettings())
        assertFalse(BarelyTalesChoiceSync.apply(835, 0, prefs))
        assertTrue(BarelyTalesChoiceSync.apply(835, 2, prefs))
        assertTrue(prefs.getBoolean("_grimBuff", false))
    }
}
