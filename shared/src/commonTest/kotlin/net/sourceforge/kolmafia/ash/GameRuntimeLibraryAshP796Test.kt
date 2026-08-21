package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CartographyChoiceSync

class GameRuntimeLibraryAshP796Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_stampsFratHouse() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CartographyChoiceSync.applyVisit(
                choiceId = 1425,
                preferences = prefs,
                ascensionNumber = 42,
            ),
        )
        assertEquals(42, prefs.getInt("lastCartographyFratHouse", 0))
    }

    @Test
    fun visit_stampsHauntedBilliards() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CartographyChoiceSync.applyVisit(
                choiceId = 1436,
                preferences = prefs,
                ascensionNumber = 7,
            ),
        )
        assertEquals(7, prefs.getInt("lastCartographyHauntedBilliards", 0))
    }

    @Test
    fun visit_coversMappedChoices() {
        assertEquals(
            setOf(1425, 1427, 1428, 1429, 1430, 1431, 1432, 1433, 1434, 1436),
            CartographyChoiceSync.CHOICE_IDS,
        )
    }
}
