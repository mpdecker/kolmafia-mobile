package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DinseyCombatSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP644Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun frontOfLine_setsRollercoasterNext() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("442", "You made it to the front of the line", db, prefs),
        )
        assertTrue(prefs.getBoolean("dinseyRollercoasterNext"))
    }

    @Test
    fun withoutLine_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(DinseyCombatSync.apply("442", "You win the fight", db, prefs))
        assertFalse(prefs.getBoolean("dinseyRollercoasterNext", false))
    }

    @Test
    fun applyCombatWin_wiresBarfMountain() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "442",
                responseText = "You made it to the front of the line",
            ).advanced,
        )
        assertTrue(prefs.getBoolean("dinseyRollercoasterNext"))
    }

    @Test
    fun loss_doesNotSet() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = false,
                preferences = prefs,
                adventureId = "442",
                responseText = "You made it to the front of the line",
            ).advanced,
        )
        assertFalse(prefs.getBoolean("dinseyRollercoasterNext", false))
    }
}
