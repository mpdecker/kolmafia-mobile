package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Group E tests — DynamicChoiceSpoilers polish (Behavioral Deepen XXXIII).
 */
class DynamicChoiceSpoilersGroupETest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        DynamicChoiceSpoilers.preferences = prefs
        DynamicChoiceSpoilers.questDatabase = QuestDatabase(prefs)
        DynamicChoiceSpoilers.itemCount = { 0 }
        DynamicChoiceSpoilers.hasEquipped = { false }
        DynamicChoiceSpoilers.hasEffect = { false }
        DynamicChoiceSpoilers.characterClass = { CharacterClass.SEAL_CLUBBER }
        DynamicChoiceSpoilers.elementalResistanceLevels = { 0 }
        DynamicChoiceSpoilers.elementalResistanceByLevel = { 0.0 }
        DynamicChoiceSpoilers.buffedMuscle = { 0 }
        DynamicChoiceSpoilers.buffedMyst = { 0 }
        DynamicChoiceSpoilers.buffedMoxie = { 0 }
        DynamicChoiceSpoilers.currentHP = { 100L }
        DynamicChoiceSpoilers.currentMP = { 50L }
        DynamicChoiceSpoilers.inebriety = { 0 }
        DynamicChoiceSpoilers.initiativeAdjustment = { 0.0 }
        DynamicChoiceSpoilers.itemDropPercent = { 0.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 0.0 }
        DynamicChoiceSpoilers.currentBonusDamage = { 0 }
        DynamicChoiceSpoilers.currentPrismaticDamage = { 0 }
        DynamicChoiceSpoilers.estimatedPoolSkill = { 0 }
        DynamicChoiceSpoilers.hasSkillId = { false }
        DynamicChoiceSpoilers.isWearingOutfit = { false }
        DynamicChoiceSpoilers.lastEncounter = { "" }
        DynamicChoiceSpoilers.numericModifier = { 0.0 }
        // Reset exclusion providers
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 0.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 0.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 0.0 }
        DynamicChoiceSpoilers.clancyLuteItemDrop = { 0.0 }
        DynamicChoiceSpoilers.eggmanItemDrop = { 0.0 }
        DynamicChoiceSpoilers.edCatServantItemDrop = { 0.0 }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // E1 — Overlook Lodge choice 606 familiar-exclusion
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun overlookLodge_noExclusion_showsRawItemDrop() {
        DynamicChoiceSpoilers.itemDropPercent = { 80.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 10.0 }
        DynamicChoiceSpoilers.elementalResistanceLevels = { if (it == "stench") 5 else 0 }
        DynamicChoiceSpoilers.initiativeAdjustment = { 50.0 }

        val spoilers = ChoiceAdventures.choiceSpoilers(606)
        assertEquals("Lost in the Great Overlook Lodge", spoilers?.name)
        // Option at index 1 (decision=2) is the item drop line
        val opt = ChoiceAdventures.findOption(spoilers?.options.orEmpty(), 2)
        assertTrue(opt?.name?.contains("have 90%") == true, "expected 80+10=90%, got: ${opt?.name}")
    }

    @Test
    fun overlookLodge_familiarExclusion_subtractsFamiliarBonus() {
        DynamicChoiceSpoilers.itemDropPercent = { 80.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 10.0 }
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 20.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 5.0 }

        val spoilers = ChoiceAdventures.choiceSpoilers(606)
        val opt = ChoiceAdventures.findOption(spoilers?.options.orEmpty(), 2)
        // 80 + 10 - 20 - 5 = 65
        assertTrue(opt?.name?.contains("have 65%") == true, "expected 65%, got: ${opt?.name}")
    }

    @Test
    fun overlookLodge_throneAndBjornExclusion() {
        DynamicChoiceSpoilers.itemDropPercent = { 100.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 0.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 15.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 10.0 }

        val spoilers = ChoiceAdventures.choiceSpoilers(606)
        val opt = ChoiceAdventures.findOption(spoilers?.options.orEmpty(), 2)
        // 100 + 0 - 15 - 10 = 75
        assertTrue(opt?.name?.contains("have 75%") == true, "expected 75%, got: ${opt?.name}")
    }

    @Test
    fun overlookLodge_combinedExclusion() {
        DynamicChoiceSpoilers.itemDropPercent = { 120.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 15.0 }
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 30.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 5.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 10.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 8.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 7.0 }

        val spoilers = ChoiceAdventures.choiceSpoilers(606)
        val opt = ChoiceAdventures.findOption(spoilers?.options.orEmpty(), 2)
        // 120 + 15 - (30 + 5 + 10 + 8 + 7) = 75
        assertTrue(opt?.name?.contains("have 75%") == true, "expected 75%, got: ${opt?.name}")
    }

    @Test
    fun overlookLodge_noApproxHedge() {
        DynamicChoiceSpoilers.itemDropPercent = { 50.0 }
        DynamicChoiceSpoilers.foodDropPercent = { 0.0 }

        val spoilers = ChoiceAdventures.choiceSpoilers(606)
        val opt = ChoiceAdventures.findOption(spoilers?.options.orEmpty(), 2)
        assertTrue(opt?.name?.contains("approx") != true, "should not contain approx hedge")
        assertTrue(opt?.name?.contains("excludes familiar") != true, "should not say excludes familiar")
    }

    @Test
    fun overlookExclusionBonus_computesCorrectly() {
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 20.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 5.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 10.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 8.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 7.0 }
        DynamicChoiceSpoilers.clancyLuteItemDrop = { 0.0 }
        DynamicChoiceSpoilers.eggmanItemDrop = { 0.0 }
        DynamicChoiceSpoilers.edCatServantItemDrop = { 0.0 }

        val bonus = DynamicChoiceSpoilers.overlookItemDropExclusionBonus()
        assertEquals(50.0, bonus, 0.001)
    }

    @Test
    fun overlookExclusionBonus_zeroWhenNoProviders() {
        assertEquals(0.0, DynamicChoiceSpoilers.overlookItemDropExclusionBonus(), 0.001)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // E2 — Tomb choice 1049 class-answer spoilers
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun tombDecision_sealClubber_matchesBoredom() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.SEAL_CLUBBER }
        val choices = mapOf(1 to "Me. Duh.", 2 to "Boredom.", 3 to "Music.")
        assertEquals(2, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_turtleTamer_matchesFriendship() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.TURTLE_TAMER }
        val choices = mapOf(1 to "Music.", 2 to "Power.", 3 to "Friendship.")
        assertEquals(3, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_pastamancer_matchesBindingPastaThralls() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.PASTAMANCER }
        val choices = mapOf(1 to "Binding pasta thralls.", 2 to "Boredom.", 3 to "Music.")
        assertEquals(1, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_sauceror_matchesPower() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.SAUCEROR }
        val choices = mapOf(1 to "Music.", 2 to "Power.", 3 to "Friendship.")
        assertEquals(2, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_discoBandit_matchesMeDuh() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.DISCO_BANDIT }
        val choices = mapOf(1 to "Boredom.", 2 to "Music.", 3 to "Me. Duh.")
        assertEquals(3, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_accordionThief_matchesMusic() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.ACCORDION_THIEF }
        val choices = mapOf(1 to "Music.", 2 to "Boredom.", 3 to "Power.")
        assertEquals(1, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_unknownClass_returnsZero() {
        DynamicChoiceSpoilers.characterClass = { null }
        val choices = mapOf(1 to "Music.", 2 to "Boredom.", 3 to "Power.")
        assertEquals(0, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombDecision_noMatch_returnsZero() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.SEAL_CLUBBER }
        val choices = mapOf(1 to "Music.", 2 to "Power.", 3 to "Friendship.")
        assertEquals(0, DynamicChoiceSpoilers.tombDecision(choices))
    }

    @Test
    fun tombSpoilers_singleOption_returnsEmpty() {
        DynamicChoiceSpoilers.lastResponseText = {
            """<input type="hidden" name="whichchoice" value="1049">
               <input type="submit" name="option" value="1">Continue</input>"""
        }
        val spoilers = ChoiceAdventures.choiceSpoilers(1049)
        assertEquals("Tomb of the Unknown Your Class Here", spoilers?.name)
        assertTrue(spoilers?.options?.isEmpty() == true)
    }

    @Test
    fun tombSpoilers_multipleOptions_marksRightAndWrong() {
        DynamicChoiceSpoilers.characterClass = { CharacterClass.PASTAMANCER }
        DynamicChoiceSpoilers.lastResponseText = {
            """<input type="hidden" name="whichchoice" value="1049">
               <input type="submit" name="option" value="1">Music.</input>
               <input type="submit" name="option" value="2">Binding pasta thralls.</input>
               <input type="submit" name="option" value="3">Power.</input>"""
        }
        val spoilers = ChoiceAdventures.choiceSpoilers(1049)
        assertEquals("Tomb of the Unknown Your Class Here", spoilers?.name)
        val opts = spoilers?.options ?: emptyList()
        assertEquals(3, opts.size)
        assertEquals("wrong answer", opts[0]?.name)
        assertEquals("right answer", opts[1]?.name)
        assertEquals("wrong answer", opts[2]?.name)
    }

    @Test
    fun tombSpoilers_unknownClass_returnsEmptyOptions() {
        DynamicChoiceSpoilers.characterClass = { null }
        DynamicChoiceSpoilers.lastResponseText = {
            """<input type="hidden" name="whichchoice" value="1049">
               <input type="submit" name="option" value="1">Music.</input>
               <input type="submit" name="option" value="2">Boredom.</input>
               <input type="submit" name="option" value="3">Power.</input>"""
        }
        val spoilers = ChoiceAdventures.choiceSpoilers(1049)
        assertTrue(spoilers?.options?.isEmpty() == true)
    }
}
