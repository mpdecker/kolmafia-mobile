package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PirateRealmSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.SeaCombatSync

class GameRuntimeLibraryAshP667Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun glassJack_unlocksSpyglassAndStep16() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(PirateRealmSync.applyNamedBossWin("Glass Jack Hummel", db, prefs))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedSpyglass"))
        assertEquals("step16", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun giantCrab_unlocksCrabsicleAndStep6() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(PirateRealmSync.applyNamedBossWin("giant giant crab", db, prefs))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedCrabsicle"))
        assertEquals("step6", db.getProgress(Quest.PIRATEREALM))
    }

    @Test
    fun plasticPirate_capsAtFifty() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("pirateRealmPlasticPiratesDefeated", 49)
        assertTrue(PirateRealmSync.applyNamedBossWin("plastic pirate", db, prefs))
        assertEquals(50, prefs.getInt("pirateRealmPlasticPiratesDefeated"))
        PirateRealmSync.applyNamedBossWin("plastic pirate", db, prefs)
        assertEquals(50, prefs.getInt("pirateRealmPlasticPiratesDefeated"))
    }

    @Test
    fun momSeaMonkee_countsEquippedBonus() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaCombatSync.apply(
                "Peanut",
                db,
                prefs,
                hasItemEquipped = { id -> id == SeaCombatSync.SHARK_JUMPER },
            ),
        )
        assertEquals(2, prefs.getInt("momSeaMonkeeProgress"))
    }

    @Test
    fun nauticalSeaceress_finishesFinal() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step12")
        assertTrue(SeaCombatSync.apply("Nautical Seaceress", db, prefs))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.FINAL))
    }

    @Test
    fun questFightRules_wiresInvader() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db, "the invader", won = true, preferences = prefs,
            ).advanced,
        )
        assertTrue(prefs.getBoolean("spaceInvaderDefeated"))
    }
}
