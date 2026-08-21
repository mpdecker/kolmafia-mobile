package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GenieChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP812Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesWishesLeft() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            GenieChoiceSync.applyVisit(
                choiceId = 1267,
                html = "You have 2 wishes left",
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_genieWishesUsed", 0))
    }

    @Test
    fun post_incrementsOnAcquire() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            GenieChoiceSync.apply(
                choiceId = 1267,
                html = "You acquire an item: foo",
                preferences = prefs,
                choiceUrl = "choice.php?wish=for+a+pony",
            ),
        )
        assertEquals(1, prefs.getInt("_genieWishesUsed", 0))
    }

    @Test
    fun questChoiceRules_wires1267() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1267,
                responseText = "You gain 1000 Meat",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "choice.php?wish=I+was+rich",
            ),
        )
        assertEquals(1, prefs.getInt("_genieWishesUsed", 0))
    }
}
