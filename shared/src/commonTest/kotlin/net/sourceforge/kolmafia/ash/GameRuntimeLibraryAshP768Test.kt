package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TrainsetChoiceSync

class GameRuntimeLibraryAshP768Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_setsWorkshedAndConfiguration() {
        val prefs = Preferences(MapSettings())
        val html = """
            <br>Your train is about to pass station 3.<
            data-slot="0" class="trainslot dragtospot" style="position: absolute; left: 0px; top: 0px; height: 80px; width: 80px;"><div data-id="1"
            data-slot="1" class="trainslot dragtospot" style="position: absolute; left: 0px; top: 0px; height: 80px; width: 80px;"><div data-id="0"
            data-slot="2" class="trainslot dragtospot" style="position: absolute; left: 0px; top: 0px; height: 80px; width: 80px;"><div data-id="6"
        """.trimIndent()
        assertTrue(TrainsetChoiceSync.applyVisit(1485, html, prefs))
        assertEquals(
            TrainsetChoiceSync.MODEL_TRAIN_SET_ID,
            prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, -1),
        )
        assertEquals(2, prefs.getInt("trainsetPosition", 0) % 8)
        assertTrue(prefs.getString("trainsetConfiguration", "").startsWith("meat_mine,empty,logging_mill"))
    }

    @Test
    fun visit_reconfiguredStamp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("trainsetPosition", 16)
        assertTrue(
            TrainsetChoiceSync.applyVisit(
                1485,
                """>Train set reconfigured.</span>
                data-slot="0" class="trainslot dragtospot" style="position: absolute; left: 0px; top: 0px; height: 80px; width: 80px;"><div data-id="0"
                """.trimIndent(),
                prefs,
            ),
        )
        assertEquals(16, prefs.getInt("lastTrainsetConfiguration", 0))
    }
}
