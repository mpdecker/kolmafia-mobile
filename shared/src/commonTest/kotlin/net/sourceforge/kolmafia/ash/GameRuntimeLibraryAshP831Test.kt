package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LatteChoiceSync

class GameRuntimeLibraryAshP831Test {

    @Test
    fun fightUnlock_discoversBasilOnce() {
        val prefs = Preferences(MapSettings())
        prefs.setString("latteUnlocks", "cinnamon")
        val html = "You spot a clump of wild basil and make a note of it."

        assertTrue(LatteChoiceSync.applyFight("The Overgrown Lot", html, prefs))
        assertEquals("cinnamon,basil", prefs.getString("latteUnlocks", ""))
        assertFalse(LatteChoiceSync.applyFight("The Overgrown Lot", html, prefs))
        assertEquals("cinnamon,basil", prefs.getString("latteUnlocks", ""))
    }

    @Test
    fun fightVisitHook_usesLastLocationPreference() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "The Overgrown Lot")
        val lib = GameRuntimeLibrary(preferences = prefs)

        lib.processVisitResponseHooks(
            "After the fight you find a clump of wild basil.",
            "fight.php?action=done",
        )

        assertEquals("basil", prefs.getString("latteUnlocks", ""))
    }
}
