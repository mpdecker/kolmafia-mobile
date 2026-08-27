package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AutumnatonChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP767Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesUpgrades() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img src="autumnaton/leftarm1.png">
            <img src="autumnaton/base.png">
            <img src="autumnaton/leftleg0.png">
            <img src="autumnaton/cowcatcher.png">
        """.trimIndent()
        assertTrue(AutumnatonChoiceSync.applyVisit(1483, html, prefs))
        assertEquals("cowcatcher,leftarm1", prefs.getString("autumnatonUpgrades", ""))
    }

    @Test
    fun post_upgradeAttaches() {
        val prefs = Preferences(MapSettings())
        prefs.setString("autumnatonUpgrades", "cowcatcher")
        assertTrue(
            AutumnatonChoiceSync.apply(
                choiceId = 1483,
                decision = 1,
                html = "You attach the enhanced left arm",
                preferences = prefs,
            ),
        )
        assertEquals("cowcatcher,leftarm1", prefs.getString("autumnatonUpgrades", ""))
    }

    @Test
    fun post_questSendsAndConsumes() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(
            AutumnatonChoiceSync.apply(
                choiceId = 1483,
                decision = 2,
                html = "Good luck, little buddy",
                preferences = prefs,
                choiceUrl = "heythereprogrammer=221",
                turnsPlayed = 100,
                consumeItem = { id, qty -> if (id == 10954) consumed += qty },
                adventureNameForSnarfblat = { if (it == 221) "The Spooky Forest" else null },
            ),
        )
        assertEquals(1, prefs.getInt("_autumnatonQuests", 0))
        assertEquals("The Spooky Forest", prefs.getString("autumnatonQuestLocation", ""))
        assertEquals(111, prefs.getInt("autumnatonQuestTurn", 0))
        assertEquals(1, consumed)
    }

    @Test
    fun plaque_visitAndPost() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            AutumnatonChoiceSync.applyVisit(
                1484,
                "The plaque currently reads: <b>Old Name</b>",
                prefs,
            ),
        )
        assertEquals("Old Name", prefs.getString("speakeasyName", ""))
        assertTrue(
            AutumnatonChoiceSync.apply(
                choiceId = 1484,
                decision = 1,
                html = "All right, you're the boss.",
                preferences = prefs,
                choiceUrl = "name=New+Name",
            ),
        )
        assertEquals("New Name", prefs.getString("speakeasyName", ""))
    }

    @Test
    fun questChoiceRules_wires1483() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1483,
                responseText = "You attach the dual exhaust",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getString("autumnatonUpgrades", "").contains("dualexhaust"))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(AutumnatonChoiceSync.applyVisit(1467, "", Preferences(MapSettings())))
    }
}
