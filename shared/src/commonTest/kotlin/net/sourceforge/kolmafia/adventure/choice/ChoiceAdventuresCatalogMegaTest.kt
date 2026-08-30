package net.sourceforge.kolmafia.adventure.choice

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.GoalManager

class ChoiceAdventuresCatalogMegaTest {

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
    }

    @Test
    fun catalogCounts_matchDesktopConfigurablePlusSpoilers() {
        assertEquals(454, ChoiceAdventures.configurableCount)
        assertEquals(66, ChoiceAdventures.spoilerCount)
    }

    @Test
    fun fingerLickinDeath_spoilersAndZone() {
        val entry = ChoiceAdventures.adventure(4)
        assertNotNull(entry)
        assertEquals("Beach", entry.zone)
        assertEquals("South of the Border", entry.name)
        assertEquals("choiceAdventure4", entry.property)
        val spoilers = ChoiceAdventures.choiceSpoilers(4)
        assertNotNull(spoilers)
        assertEquals("try for poultrygeist", spoilers.options[1].name)
        assertEquals(listOf("poultrygeist"), spoilers.options[1].itemNames)
        assertEquals("skip adventure", spoilers.options[2].name)
    }

    @Test
    fun palindomeDenimAxe_isSpoilerNotConfigurable() {
        assertEquals(null, ChoiceAdventures.adventure(2))
        val spoiler = ChoiceAdventures.spoiler(2)
        assertNotNull(spoiler)
        assertEquals("Palindome", spoiler.name)
        assertEquals("denim axe", ChoiceAdventures.findOption(spoiler.options, 1)?.name)
    }

    @Test
    fun parseChoicesWithSpoilers_appendsCatalogText() {
        val html = """
            <form><input type="hidden" name="whichchoice" value="4">
            <input type="hidden" name="option" value="1">Take the chicken</form>
            <form><input type="hidden" name="option" value="2">Order the poultrygeist</form>
            <form><input type="hidden" name="option" value="3">Walk away</form>
        """.trimIndent()
        val parsed = ChoiceUtilities.parseChoicesWithSpoilers(html)
        assertTrue(parsed[2]!!.contains("poultrygeist"), parsed.toString())
        assertTrue(parsed[3]!!.contains("skip adventure"), parsed.toString())
    }

    @Test
    fun pickGoalChoice_itemGoalOverridesPreference() {
        val picked = ChoiceAdventures.pickGoalChoice(
            choice = 4,
            decision = 3,
            hasItemGoal = { it.equals("poultrygeist", ignoreCase = true) },
            hasItem = { false },
        )
        assertEquals(2, picked)
    }

    @Test
    fun pickGoalChoice_manualControlStaysZero() {
        assertEquals(
            0,
            ChoiceAdventures.pickGoalChoice(4, 0, hasItemGoal = { true }, hasItem = { false }),
        )
    }

    @Test
    fun pickGoalChoice_completeTheOutfitPicksMissingPiece() {
        val picked = ChoiceAdventures.pickGoalChoice(
            choice = 14,
            decision = 4,
            hasItemGoal = { false },
            hasItem = { name -> name.contains("veil", ignoreCase = true) },
        )
        assertEquals(2, picked)
        val spoiler = ChoiceAdventures.choiceSpoiler(14, 4)
        assertEquals("complete the outfit", spoiler?.name)
    }

    @Test
    fun choiceDescription_usesCatalog() {
        assertEquals("small meat boost", ChoiceAdventures.choiceDescription(4, 1))
        assertEquals("unknown", ChoiceAdventures.choiceDescription(99999, 1, ""))
    }

    @Test
    fun choiceCost_economistOfScalesSpendsDullScales() {
        val cost = ChoiceCost.getCost(310, 1)
        assertNotNull(cost)
        assertEquals(ChoiceCost.Kind.ITEM, cost.kind)
        assertEquals(ItemPool.DULL_FISH_SCALE, cost.itemId)
        assertEquals(-10, cost.amount)
        assertFalse(ChoiceCost.payCost(310, 4, inventory = null, character = null))
    }

    @Test
    fun adventuresUsed_barrelCrankCostsATurn() {
        assertEquals(1, ChoiceAdventuresUsed.adventuresForChoice(1099, 1))
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(1099, 3))
    }

    @Test
    fun pickGoalChoice_viaGoalManagerName() {
        val goals = GoalManager()
        goals.addItemGoalByName("poultrygeist")
        assertEquals(2, ChoiceAdventures.pickGoalChoice(4, 1, goals, inventory = null))
    }

    @Test
    fun sealClubberSpoiler_className() {
        val footprints = ChoiceAdventures.choiceSpoilers(27)
        assertNotNull(footprints)
        assertEquals("Seal Clubber", footprints.options[0].name)
        assertEquals("Turtle Tamer", footprints.options[1].name)
    }
}
