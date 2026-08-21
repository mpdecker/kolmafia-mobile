package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DailyDungeonChoiceSync
import net.sourceforge.kolmafia.quest.DailyDungeonCombatSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightStartedSync

class GameRuntimeLibraryAshP669Test {

    @Test
    fun revision_phase671() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun finalReward_marksDungeonDone() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DailyDungeonChoiceSync.apply(
                choiceId = DailyDungeonChoiceSync.FINAL_REWARD,
                html = "You claim your rightful reward.",
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("dailyDungeonDone"))
        assertEquals(15, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun firstChest_skipAddsThree() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DailyDungeonCombatSync.PREF, 2)
        DailyDungeonChoiceSync.apply(
            choiceId = DailyDungeonChoiceSync.FIRST_CHEST,
            html = "chest",
            decision = 2,
            preferences = prefs,
        )
        assertEquals(5, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun door_consumesSkeletonKeyAndIncrements() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DailyDungeonCombatSync.PREF, 8)
        val consumed = mutableListOf<Pair<Int, Int>>()
        DailyDungeonChoiceSync.apply(
            choiceId = DailyDungeonChoiceSync.I_WANNA_BE_A_DOOR,
            html = "the key breaks off in the lock",
            decision = 1,
            preferences = prefs,
            consumeItem = { id, qty -> consumed.add(id to qty) },
        )
        assertEquals(9, prefs.getInt(DailyDungeonCombatSync.PREF))
        assertTrue(consumed.contains(DailyDungeonChoiceSync.SKELETON_KEY to 1))
    }

    @Test
    fun trap_skipsIncrementOnDecision3() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DailyDungeonCombatSync.PREF, 4)
        DailyDungeonChoiceSync.apply(
            choiceId = DailyDungeonChoiceSync.ALMOST_CERTAINLY_A_TRAP,
            html = "trap",
            decision = 3,
            preferences = prefs,
        )
        assertEquals(4, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun fightStart_parsesChamberNumber() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DailyDungeonCombatSync.applyFightStart(
                "You enter chamber <b>#7</b> of the Daily Dungeon.",
                prefs,
            ),
        )
        assertEquals(6, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun questChoiceRules_wiresChoice689() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = DailyDungeonChoiceSync.FINAL_REWARD,
                responseText = "You claim your rightful reward from the chest.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("dailyDungeonDone"))
    }

    @Test
    fun questFightStarted_skipsChamberOnCombatAction() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DailyDungeonCombatSync.PREF, 3)
        QuestFightStartedSync.apply(
            monster = "some monster",
            html = "chamber <b>#9</b>",
            preferences = prefs,
            turnsPlayed = 1,
            allowUnequippedConsume = false,
        )
        assertEquals(3, prefs.getInt(DailyDungeonCombatSync.PREF))
    }
}
