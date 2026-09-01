package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.session.VoteMonsterManager

class GameRuntimeLibraryAshP650Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun voteMonsterNow_trueOnCycleWhenNotFoughtThisTurn() {
        assertTrue(VoteMonsterManager.voteMonsterNow(turnsPlayed = 1, lastVoteMonsterTurn = 0))
        assertTrue(VoteMonsterManager.voteMonsterNow(turnsPlayed = 12, lastVoteMonsterTurn = 1))
    }

    @Test
    fun voteMonsterNow_falseWhenAlreadyFoughtThisTurn() {
        assertFalse(VoteMonsterManager.voteMonsterNow(turnsPlayed = 12, lastVoteMonsterTurn = 12))
    }

    @Test
    fun voteMonsterNow_falseOffCycle() {
        assertFalse(VoteMonsterManager.voteMonsterNow(turnsPlayed = 13, lastVoteMonsterTurn = 0))
        assertFalse(VoteMonsterManager.voteMonsterNow(turnsPlayed = 11, lastVoteMonsterTurn = 0))
    }

    @Test
    fun voteMonsterNow_readsLastTurnPref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastVoteMonsterTurn", 1)
        assertTrue(VoteMonsterManager.voteMonsterNow(12, prefs))
        prefs.setInt("lastVoteMonsterTurn", 12)
        assertFalse(VoteMonsterManager.voteMonsterNow(12, prefs))
    }

    @Test
    fun loginStyleCheckCounter_startsWhenTracking() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "true")
        assertTrue(VoteMonsterManager.checkCounter(prefs, turnsPlayed = 5, isAllowed = { _, _ -> true }))
        assertTrue(TurnCounter.isCounting(prefs, "Vote Monster", 5))
    }
}
