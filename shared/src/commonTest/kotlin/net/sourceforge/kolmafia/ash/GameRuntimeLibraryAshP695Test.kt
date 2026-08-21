package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FightItemPrefSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.session.TurnCounter

class GameRuntimeLibraryAshP695Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun emptyEye_setsPrefAndCounter() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FightItemPrefSync.apply(
                html = "You hold Zombo's eye out toward your opponent, whose gaze is transfixed by it.",
                monster = "spooky mummy",
                preferences = prefs,
                combatItemId = FightItemPrefSync.EMPTY_EYE,
                currentRun = 10,
            ),
        )
        assertEquals(11, prefs.getInt("_lastZomboEye"))
        assertTrue(
            TurnCounter.load(prefs).any {
                it.parsedLabel().equals(FightItemPrefSync.ZOMBO_EYE_COUNTER, ignoreCase = true)
            },
        )
    }

    @Test
    fun questFightRules_wiresEmptyEye() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "spooky mummy",
                won = false,
                preferences = prefs,
                responseText =
                    "You hold Zombo's eye out toward your opponent, whose gaze is transfixed by it.",
                combatItemId = FightItemPrefSync.EMPTY_EYE,
                currentRun = 4,
            ).advanced,
        )
        assertEquals(5, prefs.getInt("_lastZomboEye"))
    }
}
