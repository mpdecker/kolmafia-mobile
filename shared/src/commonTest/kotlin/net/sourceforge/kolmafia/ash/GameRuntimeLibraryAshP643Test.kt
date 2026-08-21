package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DinseyCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP643Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun gorePounds_incrementsWithoutFinishing() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("goreCollected", 40)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("416", "You collect 20 pounds of the gore", db, prefs),
        )
        assertEquals(60, prefs.getInt("goreCollected"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GORE))
    }

    @Test
    fun gorePounds_finishesAt100() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("goreCollected", 90)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("416", "You collect 15 pounds of gore", db, prefs),
        )
        assertEquals(105, prefs.getInt("goreCollected"))
        assertEquals("step2", db.getProgress(Quest.GORE))
    }

    @Test
    fun goreSloshes_setsStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "416",
                "The gore sloshes around nauseatingly in your bucket",
                db,
                prefs,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.GORE))
        assertEquals(0, prefs.getInt("goreCollected", 0))
    }

    @Test
    fun applyCombatWin_wiresGore() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "416",
                responseText = "You collect 5 pounds of the gore",
            ).advanced,
        )
        assertEquals(5, prefs.getInt("goreCollected"))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            DinseyCombatSync.apply("417", "You collect 20 pounds of the gore", db, prefs),
        )
        assertEquals(0, prefs.getInt("goreCollected", 0))
    }
}
