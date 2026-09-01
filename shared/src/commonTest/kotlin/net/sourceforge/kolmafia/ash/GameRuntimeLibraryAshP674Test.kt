package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LightsOutChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP674Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun elizabeth_storageAdvancesToLaundry() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LightsOutChoiceSync.apply(
                890,
                "BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!",
                prefs,
            ),
        )
        assertEquals("The Haunted Laundry Room", prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF))
    }

    @Test
    fun elizabeth_skipsWhenNone() {
        val prefs = Preferences(MapSettings())
        prefs.setString(LightsOutChoiceSync.ELIZABETH_PREF, "none")
        assertFalse(
            LightsOutChoiceSync.apply(
                890,
                "BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!",
                prefs,
            ),
        )
        assertEquals("none", prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF))
    }

    @Test
    fun stephen_bedroomAdvancesToNursery() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LightsOutChoiceSync.apply(
                897,
                "restock his medical kit in the nursery",
                prefs,
            ),
        )
        assertEquals("The Haunted Nursery", prefs.getString(LightsOutChoiceSync.STEPHEN_PREF))
    }

    @Test
    fun galleryAndLaboratory_areNoOps() {
        val prefs = Preferences(MapSettings())
        prefs.setString(LightsOutChoiceSync.ELIZABETH_PREF, "The Haunted Gallery")
        prefs.setString(LightsOutChoiceSync.STEPHEN_PREF, "The Haunted Laboratory")
        assertFalse(LightsOutChoiceSync.apply(896, "Elizabeth appears!", prefs))
        assertFalse(LightsOutChoiceSync.apply(903, "Stephen appears!", prefs))
        assertEquals("The Haunted Gallery", prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF))
        assertEquals("The Haunted Laboratory", prefs.getString(LightsOutChoiceSync.STEPHEN_PREF))
    }

    @Test
    fun questChoiceRules_wires890() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 890,
                responseText = "BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("The Haunted Laundry Room", prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF))
    }
}
