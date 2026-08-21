package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP633Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun funGuy_alwaysIncrementsKills() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("funGuyMansionKills", 2)
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("Fun-Guy Playmate", "You win the fight", db, prefs))
        assertEquals(3, prefs.getInt("funGuyMansionKills"))
        assertEquals(0, prefs.getInt("brodenBacteria", 0))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.BRODEN_BACTERIA))
    }

    @Test
    fun funGuy_hotTubIncrementsBacteria() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("brodenBacteria", 8)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Fun-Guy Playmate",
                "You fill the hot tub with some more bacteria",
                db,
                prefs,
            ),
        )
        assertEquals(1, prefs.getInt("funGuyMansionKills"))
        assertEquals(9, prefs.getInt("brodenBacteria"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.BRODEN_BACTERIA))
    }

    @Test
    fun funGuy_bacteriaFinishesAtTen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("brodenBacteria", 9)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Fun-Guy Playmate",
                "You fill the hot tub with some more bacteria",
                db,
                prefs,
            ),
        )
        assertEquals(10, prefs.getInt("brodenBacteria"))
        assertEquals("step1", db.getProgress(Quest.BRODEN_BACTERIA))
    }
}
