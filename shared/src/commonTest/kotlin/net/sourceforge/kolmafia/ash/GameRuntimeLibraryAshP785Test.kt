package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PeridotChoiceSync

class GameRuntimeLibraryAshP785Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_mergesAdventureIdSortedUnique() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PeridotChoiceSync.LOCATIONS_PREF, "12,3")
        assertTrue(
            PeridotChoiceSync.applyVisit(
                choiceId = 1557,
                preferences = prefs,
                adventureId = "5",
            ),
        )
        assertEquals("3,5,12", prefs.getString(PeridotChoiceSync.LOCATIONS_PREF, ""))
    }

    @Test
    fun visit_resolvesViaCallback() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PeridotChoiceSync.applyVisit(
                choiceId = 1557,
                preferences = prefs,
                lastVisitedLocationName = "The Haunted Kitchen",
                resolveAdventureId = { if (it == "The Haunted Kitchen") "350" else null },
            ),
        )
        assertEquals("350", prefs.getString(PeridotChoiceSync.LOCATIONS_PREF, ""))
    }
}
