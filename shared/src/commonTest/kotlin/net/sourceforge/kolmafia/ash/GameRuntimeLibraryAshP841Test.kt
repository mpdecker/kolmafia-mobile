package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SaberChoiceSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP841Test {
    @Test
    fun saberUpgradeRequiresMatchingSuccessText() {
        val prefs = Preferences(MapSettings())
        assertTrue(SaberChoiceSync.applyUpgrade(1386, 1, "You fit the Kaiburr crystal.", prefs))
        assertEquals(1, prefs.getInt("_saberMod", 0))
        assertFalse(SaberChoiceSync.applyUpgrade(1386, 2, "Nothing happens.", prefs))
        assertEquals(1, prefs.getInt("_saberMod", 0))
    }

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }
}
