package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MeatsmithChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP680Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun meatsmith_startsAndUnlocksStore() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            MeatsmithChoiceSync.apply(
                MeatsmithChoiceSync.HELPING_MAKE_ENDS_MEAT,
                "The skeleton store is right next door",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MEATSMITH))
        assertTrue(prefs.getBoolean("skeletonStoreAvailable"))
    }

    @Test
    fun meatsmith_checkFinishesAndConsumes() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        MeatsmithChoiceSync.apply(
            MeatsmithChoiceSync.HELPING_MAKE_ENDS_MEAT,
            "He excitedly takes the check",
            db,
            prefs,
            consumeItem = { id, qty -> consumed.add(id to qty) },
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MEATSMITH))
        assertTrue(consumed.contains(MeatsmithChoiceSync.MEATSMITH_CHECK to 1))
    }

    @Test
    fun skeletonKey_snapsOff() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            MeatsmithChoiceSync.apply(
                MeatsmithChoiceSync.TEMPORARILY_OUT_OF_SKELETONS,
                "it snaps off in the lock",
                db,
                prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(MeatsmithChoiceSync.SKELETON_KEY to 1))
    }

    @Test
    fun questChoiceRules_wires1059() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = MeatsmithChoiceSync.HELPING_MAKE_ENDS_MEAT,
                responseText = "I'll be here if you need me.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MEATSMITH))
    }
}
