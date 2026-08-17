package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PirateNcSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP591Test {

    @Test
    fun arrrboretum_incrementsSaplings() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("_saplingsPlanted", 2)
        assertTrue(
            PirateNcSync.applyFromAdventure("174", "You plant another sapling.", db, prefs),
        )
        assertEquals(3, prefs.getInt("_saplingsPlanted", 0))
    }

    @Test
    fun arrrboretum_treePlantNc_skips() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            PirateNcSync.applyFromAdventure(
                "174",
                "Plant a Tree, Plant a Tree!",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("_saplingsPlanted", 0))
    }

    @Test
    fun poopDeck_padlock_finishesPirate() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PirateNcSync.applyFromAdventure(
                "159",
                "unlocks a padlock on a trap door",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PIRATE))
    }
}
