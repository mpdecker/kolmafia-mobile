package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class BastilleBattalionSyncTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        BastilleBattalionSync.resetSessionForTest()
        prefs = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        BastilleBattalionSync.resetSessionForTest()
    }

    @Test
    fun syncPostChoice_parsesEnemyCastleOnGameStart() {
        val html = """
            According to your scanners, the nearest enemy castle is Humongous Craine, a sprawling chateau.
            (turn #1)
        """.trimIndent()
        BastilleBattalionSync.syncPostChoice(
            BastilleBattalionSync.CHOICE_RIG,
            decision = 5,
            html = html,
            prefs = prefs,
        )
        assertEquals("Humongous Craine", prefs.getString("_bastilleEnemyName"))
        assertEquals("bigcastle", prefs.getString("_bastilleEnemyCastle"))
        assertEquals(1, prefs.getInt("_bastilleGameTurn"))
        assertEquals(0, prefs.getInt("_bastilleCheese"))
    }

    @Test
    fun syncVisit_parsesTurnAndClearsChoicesOn1314() {
        prefs.setString("_bastilleChoice1", "old")
        val html = "(turn #4)"
        BastilleBattalionSync.syncVisit(
            BastilleBattalionSync.CHOICE_MASTER_OF_NONE,
            html,
            url = null,
            prefs = prefs,
        )
        assertEquals(4, prefs.getInt("_bastilleGameTurn"))
        assertEquals("", prefs.getString("_bastilleChoice1"))
    }

    @Test
    fun syncVisit_gainCheeseOnEnteringTrainingChoice() {
        val html = "You gain 50 cheese!"
        BastilleBattalionSync.syncVisit(
            BastilleBattalionSync.CHOICE_HELLO_TO_ARMS,
            html,
            url = null,
            prefs = prefs,
        )
        assertEquals(50, prefs.getInt("_bastilleCheese"))
        assertEquals(50, prefs.getInt("_bastilleLastCheese"))
    }

    @Test
    fun syncVisit_getChoicesOn1319() {
        val html = """
            <form><input type="hidden" name="whichchoice" value="1319">
            <input type="radio" name="option" value="1"> Raid the cave
            <input type="radio" name="option" value="2"> Grab the boulder
            <input type="radio" name="option" value="3"> Use the wishing well
            </form>
        """.trimIndent()
        BastilleBattalionSync.syncVisit(
            BastilleBattalionSync.CHOICE_CHEESE_SEEKING,
            html,
            url = null,
            prefs = prefs,
        )
        assertEquals("Raid the cave", prefs.getString("_bastilleChoice1"))
        assertEquals("Grab the boulder", prefs.getString("_bastilleChoice2"))
        assertEquals("Use the wishing well", prefs.getString("_bastilleChoice3"))
    }

    @Test
    fun syncPreChoice_setsLastEncounterFromChoiceLabel() {
        prefs.setString("_bastilleChoice2", "Levy the tax")
        BastilleBattalionSync.syncPreChoice(
            BastilleBattalionSync.CHOICE_HELLO_TO_ARMS,
            decision = 2,
            prefs = prefs,
        )
        assertEquals("Levy the tax", prefs.getString("_bastilleLastEncounter"))
    }

    @Test
    fun syncPostChoice_endBattleRecordsWin() {
        val html = """
            Military results:  Your attack strength is higher than their defense.<br />
            Castle results:  Your attack strength is higher than their defense.<br />
            Psychological results:  Your attack strength is lower than their defense .<br /><p>
            You have razed your foe!
            <input type="hidden" name="whichchoice" value="1314">
        """.trimIndent()
        BastilleBattalionSync.syncPostChoice(
            BastilleBattalionSync.CHOICE_CASTLE_VS_CASTLE,
            decision = 1,
            html = html,
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean("_bastilleLastBattleWon"))
        assertTrue(prefs.getString("_bastilleLastBattleResults").contains("MA>MD"))
    }

    @Test
    fun syncPostChoice_endBattleRecordsLoss() {
        val html = """
            Military results:  Your attack strength is lower than their defense .<br />
            Castle results:  Your attack strength is lower than their defense .<br />
            Psychological results:  Your attack strength is lower than their defense .<br /><p>
            Unfortunately, you have been razed.
            <input type="hidden" name="whichchoice" value="1316">
        """.trimIndent()
        BastilleBattalionSync.syncPostChoice(
            BastilleBattalionSync.CHOICE_CASTLE_VS_CASTLE,
            decision = 1,
            html = html,
            prefs = prefs,
        )
        assertFalse(prefs.getBoolean("_bastilleLastBattleWon"))
    }

    @Test
    fun syncPostChoice_logBoostsFromActiveEffects() {
        BastilleBattalionSync.syncPostChoice(
            BastilleBattalionSync.CHOICE_RIG,
            decision = 5,
            html = "(turn #1)",
            prefs = prefs,
            activeEffectNames = setOf("Shark Tooth Grin", "Enhanced Interrogation"),
        )
        assertEquals("MP", prefs.getString("_bastilleBoosts"))
    }

    @Test
    fun isBastilleChoice_covers1313Through1319() {
        assertTrue(BastilleBattalionSync.isBastilleChoice(1313))
        assertTrue(BastilleBattalionSync.isBastilleChoice(1319))
        assertFalse(BastilleBattalionSync.isBastilleChoice(1312))
    }
}
