package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync

class GameRuntimeLibraryAshP627Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun emptyBox_finishesFoodQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DINSEY_REFRESHMENTS,
                "You realize that the box of refreshments is empty",
                db,
                prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.WORK_WITH_FOOD))
        assertEquals(30, prefs.getInt("dinseyTouristsFed"))
    }

    @Test
    fun emptySnacksAlias_alsoFinishes() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DINSEY_REFRESHMENTS,
                "The box of snacks is empty.",
                db,
                prefs,
            ),
        )
        assertEquals(30, prefs.getInt("dinseyTouristsFed"))
    }

    @Test
    fun handOut_countsOpponentPlusFrontTourist() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DINSEY_REFRESHMENTS,
                "You hand out snacks to your opponent and the tourist in front of them.",
                db,
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt("dinseyTouristsFed"))
    }

    @Test
    fun handOut_countsCrowd() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyTouristsFed", 10)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DINSEY_REFRESHMENTS,
                "You hand out snacks to your opponent and the 4 tourists in front of them.",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("dinseyTouristsFed"))
    }

    @Test
    fun handOut_capsAtThirty() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyTouristsFed", 28)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemUsedSync.apply(
                QuestItemUsedSync.DINSEY_REFRESHMENTS,
                "You hand out snacks to your opponent and the 10 tourists in front.",
                db,
                prefs,
            ),
        )
        assertEquals(30, prefs.getInt("dinseyTouristsFed"))
    }
}
