package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestCombatWinExtrasSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.ZeppelinRonSync

class GameRuntimeLibraryAshP666Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun protectorSpectre_finishesWorship() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.WORSHIP, "step3")
        assertTrue(QuestCombatWinExtrasSync.apply("Protector Spectre", db, prefs))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.WORSHIP))
    }

    @Test
    fun wuTang_setsLastDefeated() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(QuestCombatWinExtrasSync.apply("Wu Tang the Betrayer", db, prefs, ascensionNumber = 7))
        assertEquals(7, prefs.getInt("lastWuTangDefeated"))
    }

    @Test
    fun zeppelinCabin_setsProgressAndRonStep4() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ZeppelinRonSync.applyCabinProgress(
                "red butler",
                "the inevitable confrontation with Ron Copperhead awaits",
                db,
                prefs,
            ),
        )
        assertEquals(6, prefs.getInt("zeppelinProgress"))
        assertEquals("step4", db.getProgress(Quest.RON))
    }

    @Test
    fun zeppelinCabin_firstPhraseSetsProgress1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            ZeppelinRonSync.applyCabinProgress(
                "man with the red buttons",
                "you do get a slightly better sense of the ship",
                db,
                prefs,
            ),
        )
        assertEquals(1, prefs.getInt("zeppelinProgress"))
    }

    @Test
    fun questFightRules_wiresSpectreWin() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.WORSHIP, "step2")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "Protector Spectre", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.WORSHIP))
    }
}
