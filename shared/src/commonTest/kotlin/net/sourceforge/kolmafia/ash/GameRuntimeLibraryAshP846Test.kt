package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BeachCombChoiceSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP846Test {
    @Test
    fun beachHeadBlessingUnlocksAndMarksHeadUsed() {
        val prefs = Preferences(MapSettings())
        val html = """
            It gives you some kind of magical blessing as a tip.
            You acquire an effect: <b>A Brush with Grossness</b>
        """.trimIndent()
        assertTrue(BeachCombChoiceSync.apply(1388, 3, html, prefs))
        assertEquals("3", prefs.getString("beachHeadsUnlocked", ""))
        assertEquals("3", prefs.getString("_beachHeadsUsed", ""))
    }
}
