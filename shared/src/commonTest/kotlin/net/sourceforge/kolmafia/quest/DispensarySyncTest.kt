package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class DispensarySyncTest {

    @Test
    fun applyFromResponse_setsLastDispensaryOpenOnFarquar() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(ascensionNumber = 7)
        DispensarySync.applyFromResponse("You meet FARQUAR in the woods.", state, prefs)
        assertEquals(7, prefs.getInt(DispensarySync.LAST_DISPENSARY_OPEN_PREF, -1))
    }

    @Test
    fun applyFromResponse_setsLastDispensaryOpenOnSleepingNearTheEnemy() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(ascensionNumber = 3)
        DispensarySync.applyFromResponse("Sleeping Near the Enemy is unlocked.", state, prefs)
        assertEquals(3, prefs.getInt(DispensarySync.LAST_DISPENSARY_OPEN_PREF, -1))
    }

    @Test
    fun applyFromResponse_ignoresUnrelatedHtml() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(ascensionNumber = 3)
        DispensarySync.applyFromResponse("Nothing to see here.", state, prefs)
        assertEquals(-1, prefs.getInt(DispensarySync.LAST_DISPENSARY_OPEN_PREF, -1))
    }
}
