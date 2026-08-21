package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CommunityServiceChoiceSync

class GameRuntimeLibraryAshP885Test {
    @Test
    fun completedServicesAppendInDesktopFormat() {
        val prefs = Preferences(MapSettings())
        assertTrue(CommunityServiceChoiceSync.apply(1089, 1, "You acquire an item", prefs))
        assertTrue(CommunityServiceChoiceSync.apply(1089, 11, "You acquire an effect", prefs))
        assertEquals("Donate Blood,Coil Wire", prefs.getString("csServicesPerformed", ""))
    }
}
