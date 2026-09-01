package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP653Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bonerdagon_setsStep1AndClearsTotal() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptTotalEvilness", 999)
        val db = QuestDatabase(prefs)
        assertTrue(CryptManager.defeatBoss("Bonerdagon", db, prefs))
        assertEquals("step1", db.getProgress(Quest.CYRPT))
        assertEquals(0, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun zmombie_zerosAlcoveAndDecrementsTotal() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 13)
        prefs.setInt("cyrptTotalEvilness", 50)
        val db = QuestDatabase(prefs)
        assertTrue(CryptManager.defeatBoss("conjoined zmombie", db, prefs))
        assertEquals(0, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(37, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun lastCornerBoss_setsTotal999() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 13)
        prefs.setInt("cyrptTotalEvilness", 13)
        val db = QuestDatabase(prefs)
        assertTrue(CryptManager.defeatBoss("giant skeelton", db, prefs))
        assertEquals(0, prefs.getInt("cyrptNookEvilness"))
        assertEquals(999, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun unknownMonster_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(CryptManager.defeatBoss("screambat", db, prefs))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.CYRPT))
    }

    @Test
    fun applyCombatWin_wiresBonerdagon() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "Bonerdagon",
                won = true,
                preferences = prefs,
            ).advanced,
        )
        assertEquals("step1", db.getProgress(Quest.CYRPT))
    }

    @Test
    fun blankMonsterWin_doesNotDefeatBoss() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
            ).advanced,
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.CYRPT))
    }
}
