package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MonkeyPawChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.MonkeyPawRequest

class GameRuntimeLibraryAshP789Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesFingers() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MonkeyPawChoiceSync.applyVisit(
                1501,
                "It has 3 fingers held up expectantly.",
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt(MonkeyPawRequest.WISHES_USED_PREF, -1))
    }

    @Test
    fun post_incrementsOnWishGranted() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(MonkeyPawRequest.WISHES_USED_PREF, 1)
        assertTrue(
            MonkeyPawChoiceSync.apply(
                1501,
                "Wish granted. Something happened.",
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt(MonkeyPawRequest.WISHES_USED_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1501() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1501,
                responseText = "Wish granted.",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(MonkeyPawRequest.WISHES_USED_PREF, 0))
    }
}
