package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestFightStartedSync
import net.sourceforge.kolmafia.session.TurnCounter

class GameRuntimeLibraryAshP625Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun gng_discardsEquippedGingerservo() {
        val cleared = mutableListOf<EquipmentSlot>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "GNG-3-R",
                html = "",
                preferences = Preferences(MapSettings()),
                turnsPlayed = 10,
                equipment = mapOf(EquipmentSlot.ACC1 to "gingerservo"),
                itemName = { if (it == QuestFightStartedSync.GINGERSERVO) "gingerservo" else "" },
                clearSlot = { cleared += it },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(EquipmentSlot.ACC1), cleared)
        assertEquals(listOf(QuestFightStartedSync.GINGERSERVO to 1), consumed)
    }

    @Test
    fun gng_consumesUnequippedWhenAllowed() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "GNG-3-R",
                html = "",
                preferences = Preferences(MapSettings()),
                turnsPlayed = 10,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(QuestFightStartedSync.GINGERSERVO to 1), consumed)
    }

    @Test
    fun gng_skipsUnequippedConsumeOnCombatAction() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "GNG-3-R",
                html = "",
                preferences = Preferences(MapSettings()),
                turnsPlayed = 10,
                consumeItem = { id, qty -> consumed += id to qty },
                allowUnequippedConsume = !QuestFightStartedSync.isCombatActionUrl(
                    "fight.php?action=attack",
                ),
            ),
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun snojo_countsSnowmanParts() {
        val prefs = Preferences(MapSettings())
        val html = """
            otherimages/combatsnowman/head.gif
            otherimages/combatsnowman/torso.gif
            otherimages/combatsnowman/arm.gif
            otherimages/combatsnowman/leg.gif
            otherimages/combatsnowman/scarf.gif
        """.trimIndent()
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "X-32-F Combat Training Snowman",
                html = html,
                preferences = prefs,
                turnsPlayed = 0,
            ),
        )
        assertEquals(3, prefs.getInt("_snojoParts"))
    }

    @Test
    fun voteMonster_incrementsAndStopsCounter() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trackVoteMonster", "false")
        TurnCounter.startCounting(prefs, 0, 11, "Vote Monster", "vote.gif")
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "angry ghost",
                html = "",
                preferences = prefs,
                turnsPlayed = 42,
            ),
        )
        assertEquals(1, prefs.getInt("_voteFreeFights"))
        assertEquals(42, prefs.getInt("lastVoteMonsterTurn"))
        assertEquals("angry ghost", prefs.getString("_voteMonster"))
        assertFalse(prefs.getString(TurnCounter.PREF_KEY, "").contains("Vote Monster"))
    }

    @Test
    fun voteMonster_capsAtThreeAndIsIdempotentSameTurn() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_voteFreeFights", 3)
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "slime blob",
                html = "",
                preferences = prefs,
                turnsPlayed = 9,
            ),
        )
        assertEquals(3, prefs.getInt("_voteFreeFights"))
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "slime blob",
                html = "",
                preferences = prefs,
                turnsPlayed = 9,
            ),
        )
        assertEquals(3, prefs.getInt("_voteFreeFights"))
    }
}
