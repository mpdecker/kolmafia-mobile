package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.UnpermeryChoiceSync

class GameRuntimeLibraryAshP881Test {
    @Test
    fun unpermRemovesSkillAndBanksKarma() {
        val prefs = Preferences(MapSettings())
        val removed = mutableListOf<String>()
        assertTrue(UnpermeryChoiceSync.apply(812, 1, "Turning Liver of Steel (HP) into 100 karma.", prefs) { removed += it })
        assertEquals(listOf("Liver of Steel"), removed)
        assertEquals(100, prefs.getInt("bankedKarma", 0))
    }
}
