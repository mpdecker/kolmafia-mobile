package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestFightStartedSync
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.session.VoteMonsterManager

class GameRuntimeLibraryAshP649Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun checkCounter_startsVoteMonster() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "true")
        assertTrue(VoteMonsterManager.checkCounter(prefs, turnsPlayed = 12, isAllowed = { _, _ -> true }))
        assertTrue(TurnCounter.isCounting(prefs, "Vote Monster", 12))
        val entry = TurnCounter.findByLabel(prefs, "Vote Monster")
        assertEquals("absballot.gif", entry?.image)
        assertEquals(23, entry?.absoluteTurn)
    }

    @Test
    fun checkCounter_skipsWhenDisallowed() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "true")
        assertFalse(
            VoteMonsterManager.checkCounter(
                prefs,
                turnsPlayed = 12,
                isAllowed = { type, key ->
                    type != RestrictedItemType.ITEMS || key != "voter registration form"
                },
            ),
        )
        assertFalse(TurnCounter.isCounting(prefs, "Vote Monster", 12))
    }

    @Test
    fun checkCounter_skipsWhenTrackFalse() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "false")
        assertFalse(VoteMonsterManager.checkCounter(prefs, turnsPlayed = 12, isAllowed = { _, _ -> true }))
        assertFalse(TurnCounter.isCounting(prefs, "Vote Monster", 12))
    }

    @Test
    fun checkCounter_skipsWhenFreeCapReached() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "free")
        prefs.setInt("_voteFreeFights", 3)
        assertFalse(VoteMonsterManager.checkCounter(prefs, turnsPlayed = 12, isAllowed = { _, _ -> true }))
        assertFalse(TurnCounter.isCounting(prefs, "Vote Monster", 12))
    }

    @Test
    fun checkCounter_skipsWhenAlreadyCounting() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "true")
        TurnCounter.startCounting(prefs, 12, 11, "Vote Monster", "absballot.gif")
        assertFalse(VoteMonsterManager.checkCounter(prefs, turnsPlayed = 12, isAllowed = { _, _ -> true }))
    }

    @Test
    fun fightStarted_wiresRestartAfterVoteMonster() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "true")
        TurnCounter.startCounting(prefs, 0, 11, "Vote Monster", "vote.gif")
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "angry ghost",
                html = "",
                preferences = prefs,
                turnsPlayed = 12,
                isAllowed = { _, _ -> true },
            ),
        )
        val entry = TurnCounter.findByLabel(prefs, "Vote Monster")
        assertEquals("absballot.gif", entry?.image)
        assertEquals(23, entry?.absoluteTurn)
    }
}
