package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SwampQuestSync

class GameRuntimeLibraryAshP596Test {

    @Test
    fun marty_startsSwamp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SwampQuestSync.applyFromCanadia(
                url = "place.php?whichplace=canadia&action=lc_marty",
                html = "All right, Marty, I'll see what I can do",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SWAMP))
    }

    @Test
    fun marais_unlocksAreas() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SwampQuestSync.applyFromMarais(
                url = "place.php?whichplace=marais",
                html = """
                    The Edge of the Swamp
                    The Dark and Spooky Swamp
                    The Wildlife Sanctuarrrrrgh
                    The Corpse Bog
                """.trimIndent(),
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SWAMP))
        assertTrue(prefs.getBoolean("maraisDarkUnlock", false))
        assertTrue(prefs.getBoolean("maraisWildlifeUnlock", false))
        assertTrue(prefs.getBoolean("maraisCorpseUnlock", false))
    }
}
