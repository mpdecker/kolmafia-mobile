package net.sourceforge.kolmafia.adventure.choice

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.DynamicChoiceSpoilers
import net.sourceforge.kolmafia.session.WumpusManager

class ChoiceAdventuresphase7510Test {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        WumpusManager.reset()
        DynamicChoiceSpoilers.preferences = Preferences(MapSettings())
        DynamicChoiceSpoilers.ascensions = { 3 }
        DynamicChoiceSpoilers.characterClass = { null }
        DynamicChoiceSpoilers.itemCount = { 0 }
        DynamicChoiceSpoilers.hasEquipped = { false }
        DynamicChoiceSpoilers.inebriety = { 0 }
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        WumpusManager.reset()
        DynamicChoiceSpoilers.preferences = null
        DynamicChoiceSpoilers.ascensions = { 0 }
        DynamicChoiceSpoilers.characterClass = { null }
        DynamicChoiceSpoilers.itemCount = { 0 }
        DynamicChoiceSpoilers.hasEquipped = { false }
        DynamicChoiceSpoilers.inebriety = { 0 }
    }

    @Test
    fun choiceSpoilers_routesDynamicBeforeCatalogAndNullsSolverChoices() {
        val rock = ChoiceAdventures.choiceSpoilers(5)
        assertEquals("Heart of Very, Very Dark Darkness", rock?.name)
        assertEquals(ChoiceAdventures.SKIP_ADVENTURE, rock?.options?.get(1))
        assertNull(ChoiceAdventures.choiceSpoilers(535))
        assertNull(ChoiceAdventures.choiceSpoilers(536))
        assertNull(ChoiceAdventures.choiceSpoilers(546))
        assertNull(ChoiceAdventures.choiceSpoilers(594))
    }

    @Test
    fun choiceSpoiler_105_reportsDefeatedOrCallCountAfterAscensionReset() {
        val prefs = DynamicChoiceSpoilers.preferences!!
        prefs.setInt("lastGuyMadeOfBeesReset", 0)
        prefs.setInt("guyMadeOfBeesCount", 9)
        prefs.setBoolean("guyMadeOfBeesDefeated", true)
        val defeated = ChoiceAdventures.choiceSpoiler(105, 3)
        assertEquals("guy made of bees: called 0 times", defeated?.name)
        prefs.setBoolean("guyMadeOfBeesDefeated", true)
        assertEquals(
            "guy made of bees: defeated",
            ChoiceAdventures.choiceSpoiler(105, 3)?.name,
        )
        KoLCharacter.ensureUpdatedGuyMadeOfBees(prefs, 3)
        assertEquals(3, prefs.getInt("lastGuyMadeOfBeesReset", -1))
    }

    @Test
    fun parseChoicesWithSpoilers_requiresHandlingChoiceAndLastChoice() {
        val html = """
            <form><input type="hidden" name="whichchoice" value="4">
            <input type="hidden" name="option" value="3">Walk away</form>
        """.trimIndent()
        val idle = ChoiceUtilities.parseChoicesWithSpoilers(html)
        assertEquals("Walk away", idle[3])
        ChoiceCombatAshState.noteChoiceVisit(4, html)
        val live = ChoiceUtilities.parseChoicesWithSpoilers(html)
        assertTrue(live[3]!!.contains("skip adventure"), live.toString())
    }

    @Test
    fun optionsFor_emptyCatalogFallsThroughToDynamicItemGoals() {
        assertTrue(ChoiceAdventures.adventure(184)!!.options.isEmpty())
        val picked = ChoiceAdventures.pickGoalChoice(
            choice = 184,
            decision = 1,
            hasItemGoal = { it.equals("shot of rotgut", ignoreCase = true) },
            hasItem = { false },
        )
        assertEquals(2, picked)
        assertEquals(
            listOf("Mistified"),
            ChoiceAdventures.adventure(546)!!.options.take(1).map { it.name },
        )
        assertEquals(
            "E.M.U. rocket thrusters",
            ChoiceAdventures.adventure(535)!!.options.first().name,
        )
    }

    @Test
    fun wumpusDynamicChoiceOptions_emptyVsSixSlot() {
        val empty = WumpusManager.dynamicChoiceOptions()
        assertEquals(3, empty.size)
        assertEquals("", empty[0]?.name)
        assertEquals("", empty[1]?.name)
        assertNull(empty[2])
        WumpusManager.visitChoice(
            """
            <b>>The Acrid Chamber</b>
            Enter the breezy chamber
            Enter the creepy chamber
            Enter the dripping chamber
            """.trimIndent(),
        )
        val live = WumpusManager.dynamicChoiceOptions()
        assertEquals(6, live.size)
        assertEquals(live[0]?.name, live[3]?.name)
    }

    @Test
    fun skipAdventureConstant_andDynamicChoiceOptionsApi() {
        assertEquals("skip adventure", ChoiceAdventures.SKIP_ADVENTURE.name)
        val options = DynamicChoiceSpoilers.dynamicChoiceOptions(7)
        assertEquals(ChoiceAdventures.SKIP_ADVENTURE.name, options[1]?.name)
    }
}
