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

class GameRuntimeLibraryAshP661Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bossBat_setsStep4IfBetter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, "step3")
        assertTrue(QuestCombatWinExtrasSync.apply("Boss Bat", db, prefs))
        assertEquals("step4", db.getProgress(Quest.BAT))
    }

    @Test
    fun goblinKing_finishesQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GOBLIN, "started")
        assertTrue(QuestCombatWinExtrasSync.apply("Knob Goblin King", db, prefs))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.GOBLIN))
    }

    @Test
    fun groarAndYeti_advanceTrapper() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TRAPPER, "step3")
        assertTrue(QuestCombatWinExtrasSync.apply("panicking Knott Yeti", db, prefs))
        assertEquals("step4", db.getProgress(Quest.TRAPPER))
        assertTrue(QuestCombatWinExtrasSync.apply("Groar", db, prefs))
        assertEquals("step5", db.getProgress(Quest.TRAPPER))
    }

    @Test
    fun hulkingBridgeTroll_resetsChasmProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("chasmBridgeProgress", 18)
        val db = QuestDatabase(prefs)
        assertTrue(QuestCombatWinExtrasSync.apply("hulking bridge troll", db, prefs))
        assertEquals(0, prefs.getInt("chasmBridgeProgress"))
    }

    @Test
    fun rufusShadowBoss_setsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.RUFUS, QuestDatabase.STARTED)
        assertTrue(QuestCombatWinExtrasSync.apply("shadow spire", db, prefs))
        assertEquals("step1", db.getProgress(Quest.RUFUS))
    }

    @Test
    fun telegramStage_includesCowpoke() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "drunk cowpoke", won = true, preferences = prefs,
            ).advanced,
        )
        assertEquals(1, prefs.getInt("lttQuestStageCount", 0))
    }

    @Test
    fun burnouts_opiumGrenadeAddsThree() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(QuestFightRules.BURNOUTS_DEFEATED_PREF, 10)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.CITADEL, "step3")
        QuestFightRules.applyCombat(
            db,
            "pair of burnouts",
            won = true,
            preferences = prefs,
            responseText = "You throw the opium grenade.",
        )
        assertEquals(13, prefs.getInt(QuestFightRules.BURNOUTS_DEFEATED_PREF))
        assertEquals("step3", db.getProgress(Quest.CITADEL))
    }

    @Test
    fun partyFair_incrementsFreeTurnsWithoutQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        QuestFightRules.applyCombat(db, "biker", won = true, preferences = prefs)
        assertEquals(1, prefs.getInt("_neverendingPartyFreeTurns"))
        repeat(12) {
            QuestFightRules.applyCombat(db, "jock", won = true, preferences = prefs)
        }
        assertEquals(10, prefs.getInt("_neverendingPartyFreeTurns"))
    }

    @Test
    fun applyCombatWin_wiresBossBat() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, "step2")
        assertTrue(
            QuestFightRules.applyCombat(db, "Boss Bat", won = true, preferences = prefs).advanced,
        )
        assertEquals("step4", db.getProgress(Quest.BAT))
    }
}
