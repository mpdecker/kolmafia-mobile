package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync

class GameRuntimeLibraryAshP626Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun clippers_incrementWithoutFinishing() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("fingernailsClipped", 4)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.FINGERNAIL_CLIPPERS,
                "You find a little sliver of something fingernail-like",
                db,
                prefs,
            ),
        )
        assertEquals(5, prefs.getInt("fingernailsClipped"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.CLIPPER))
    }

    @Test
    fun clippers_finishAtTwentyThree() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("fingernailsClipped", 22)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.FINGERNAIL_CLIPPERS,
                "little sliver of something fingernail-like",
                db,
                prefs,
            ),
        )
        assertEquals(23, prefs.getInt("fingernailsClipped"))
        assertEquals("step1", db.getProgress(Quest.CLIPPER))
    }

    @Test
    fun clippers_withoutSliverIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.FINGERNAIL_CLIPPERS,
                "Nothing happens.",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("fingernailsClipped", 0))
    }
}
