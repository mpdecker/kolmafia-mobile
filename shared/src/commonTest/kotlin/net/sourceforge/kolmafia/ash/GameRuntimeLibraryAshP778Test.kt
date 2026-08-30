package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TakerSpaceChoiceSync

class GameRuntimeLibraryAshP778Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesSuppliesAndWorkshed() {
        val prefs = Preferences(MapSettings())
        var refreshed = false
        val html = """
            <b>Current Supplies:</b><br>3 stolen spices<br>2 robbed rums<br>1 absconded-with anchor<br>4 misappropriated mainmasts<br>5 snatched silk<br>6 gaffled gold<br>
        """.trimIndent()
        assertTrue(
            TakerSpaceChoiceSync.applyVisit(1537, html, prefs) { refreshed = true },
        )
        assertEquals(
            TakerSpaceChoiceSync.TAKERSPACE_LETTER_OF_MARQUE_ID,
            prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1),
        )
        assertTrue(prefs.getBoolean("_takerSpaceSuppliesDelivered", false))
        assertEquals(3, prefs.getInt("takerSpaceSpice", 0))
        assertEquals(2, prefs.getInt("takerSpaceRum", 0))
        assertEquals(1, prefs.getInt("takerSpaceAnchor", 0))
        assertEquals(4, prefs.getInt("takerSpaceMast", 0))
        assertEquals(5, prefs.getInt("takerSpaceSilk", 0))
        assertEquals(6, prefs.getInt("takerSpaceGold", 0))
        assertTrue(refreshed)
    }

    @Test
    fun visit_withoutSuppliesStillMarksDelivered() {
        val prefs = Preferences(MapSettings())
        assertTrue(TakerSpaceChoiceSync.applyVisit(1537, "<html>empty</html>", prefs))
        assertTrue(prefs.getBoolean("_takerSpaceSuppliesDelivered", false))
        assertEquals(0, prefs.getInt("takerSpaceSpice", -1).coerceAtLeast(0))
    }
}
