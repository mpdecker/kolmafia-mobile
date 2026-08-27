package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrystalBallChoiceSync

class GameRuntimeLibraryAshP760Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesPredictions() {
        val prefs = Preferences(MapSettings())
        prefs.setString(CrystalBallChoiceSync.PREDICTIONS_PREF, "10:The Haunted Bedroom:spooky mummy")
        assertTrue(
            CrystalBallChoiceSync.applyVisit(
                choiceId = 1462,
                html = "<ul><li> a spooky mummy in The Haunted Bedroom</li></ul>",
                preferences = prefs,
                currentRun = 99,
                findLocation = { if (it == "The Haunted Bedroom") it else null },
                findMonster = { if (it == "spooky mummy") it else null },
            ),
        )
        // same prediction preserves prior turn
        assertEquals(
            "10:The Haunted Bedroom:spooky mummy",
            prefs.getString(CrystalBallChoiceSync.PREDICTIONS_PREF, ""),
        )
    }

    @Test
    fun visit_newPredictionUsesCurrentRun() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CrystalBallChoiceSync.applyVisit(
                1462,
                "<li> an angry bugbear in The Heap</li>",
                prefs,
                currentRun = 42,
                findLocation = { if (it == "The Heap") it else null },
                findMonster = { if (it == "angry bugbear") it else null },
            ),
        )
        assertEquals(
            "42:The Heap:angry bugbear",
            prefs.getString(CrystalBallChoiceSync.PREDICTIONS_PREF, ""),
        )
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(
            CrystalBallChoiceSync.applyVisit(1451, "<li> a spooky mummy in The Haunted Bedroom</li>", Preferences(MapSettings())),
        )
    }
}
