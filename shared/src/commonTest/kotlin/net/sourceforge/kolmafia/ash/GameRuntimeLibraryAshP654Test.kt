package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemRules
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP654Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun acquireEvilometer_startsQuestAndSeedsCorners() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(CryptManager.acquireEvilometer(db, prefs))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CYRPT))
        assertEquals(200, prefs.getInt("cyrptTotalEvilness"))
        assertEquals(50, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(50, prefs.getInt("cyrptCrannyEvilness"))
        assertEquals(50, prefs.getInt("cyrptNicheEvilness"))
        assertEquals(50, prefs.getInt("cyrptNookEvilness"))
    }

    @Test
    fun applyAcquireFromHtml_requiresAcquireAndName() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            CryptManager.applyAcquireFromHtml("You look at an Evilometer", db, prefs),
        )
        assertTrue(
            CryptManager.applyAcquireFromHtml(
                "You acquire an item: <b>Evilometer</b>",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CYRPT))
    }

    @Test
    fun applyItemsGained_wiresNameAndId() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemRules.applyItemsGained(
                itemsGained = listOf("Evilometer"),
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(200, prefs.getInt("cyrptTotalEvilness"))
        val prefs2 = Preferences(MapSettings())
        val db2 = QuestDatabase(prefs2)
        assertTrue(
            QuestItemRules.applyItemsGained(
                itemsGained = emptyList(),
                questDatabase = db2,
                preferences = prefs2,
                itemIdsGained = listOf(CryptManager.EVILOMETER),
            ),
        )
        assertEquals(50, prefs2.getInt("cyrptAlcoveEvilness"))
    }
}
