package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerCarriedFamiliarsTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun canCarry_falseWhenThroneLineIsNone() {
        ModifierDatabase.injectForTest("Throne", "Miniature Donkey", "none")
        assertFalse(FamiliarCarryRules.canCarry("Miniature Donkey"))
    }

    @Test
    fun canCarry_trueWhenThroneLineAbsent() {
        assertTrue(FamiliarCarryRules.canCarry("Miniature Donkey"))
    }

    @Test
    fun canCarry_trueWhenThroneHasModifiers() {
        ModifierDatabase.injectForTest("Throne", "Miniature Donkey", "Meat Drop: +5")
        assertTrue(FamiliarCarryRules.canCarry("Miniature Donkey"))
    }

    @Test
    fun discoverCarryFamiliars_excludesActiveFamiliarAndSortsByScore() {
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Mysticality: +5")
        ModifierDatabase.injectForTest("Familiar", "Mosquito", "Mysticality: +1")
        val context = MaximizerCarriedFamiliars.DiscoveryContext(
            familiarState = FamiliarState(
                activeFamiliar = FamiliarData(1, "Mosquito", "Mosquito", 5, 0, 0),
                ownedFamiliars = listOf(
                    FamiliarData(1, "Mosquito", "Mosquito", 5, 0, 0),
                    FamiliarData(2, "Donkey", "Miniature Donkey", 5, 0, 0),
                ),
            ),
            charState = CharacterState(familiarName = "Mosquito"),
            preferences = null,
            scoreFamiliar = { race ->
                when (race.lowercase()) {
                    "miniature donkey" -> 5.0
                    "mosquito" -> 1.0
                    else -> 0.0
                }
            },
        )
        val discovered = MaximizerCarriedFamiliars.discoverCarryFamiliars(context)
        assertEquals(listOf("Miniature Donkey"), discovered)
    }

    @Test
    fun discoverCarryFamiliars_excludesUnusableBeeRaceOnBeecore() {
        ModifierDatabase.injectForTest("Familiar", "Barrrnacle", "Mysticality: +5")
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Mysticality: +1")
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
        val context = MaximizerCarriedFamiliars.DiscoveryContext(
            familiarState = FamiliarState(
                ownedFamiliars = listOf(
                    FamiliarData(8, "Barn", "Barrrnacle", 5, 0, 0),
                    FamiliarData(1, "Donkey", "Miniature Donkey", 5, 0, 0),
                ),
            ),
            charState = character.state.value,
            preferences = null,
            scoreFamiliar = { 1.0 },
        )
        val discovered = MaximizerCarriedFamiliars.discoverCarryFamiliars(context)
        assertEquals(listOf("Miniature Donkey"), discovered)
    }

    @Test
    fun needsEnthroneDiscovery_falseOnSneakyPete() {
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(path = "Avatar of Sneaky Pete"))
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.set(
            MaximizerSlot.HAT,
            listOf(
                MaximizerRankedItem(
                    4614,
                    MaximizerManager.CROWN_OF_THRONES,
                    1.0,
                    MaximizerCheckedItem(4614, MaximizerManager.CROWN_OF_THRONES, initial = 1),
                ),
            ),
        )
        assertFalse(
            MaximizerCarriedFamiliars.needsEnthroneDiscovery(
                MaximizeSpec(primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MYS),
                character.state.value,
                buckets,
                emptyMap(),
            ),
        )
    }

    @Test
    fun needsEnthroneDiscovery_falseWhenExplicitGoalsPresent() {
        val spec = MaximizeSpec(
            primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MYS,
            enthronedFamiliars = listOf("Mosquito"),
        )
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.set(
            MaximizerSlot.HAT,
            listOf(
                MaximizerRankedItem(
                    4614,
                    MaximizerManager.CROWN_OF_THRONES,
                    1.0,
                    MaximizerCheckedItem(4614, MaximizerManager.CROWN_OF_THRONES, initial = 1),
                ),
            ),
        )
        assertFalse(
            MaximizerCarriedFamiliars.needsEnthroneDiscovery(
                spec,
                CharacterState(),
                buckets,
                emptyMap(),
            ),
        )
    }

    @Test
    fun needsEnthroneDiscovery_trueWhenCrownCandidateAndNoGoals() {
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.set(
            MaximizerSlot.HAT,
            listOf(
                MaximizerRankedItem(
                    4614,
                    MaximizerManager.CROWN_OF_THRONES,
                    1.0,
                    MaximizerCheckedItem(4614, MaximizerManager.CROWN_OF_THRONES, initial = 1),
                ),
            ),
        )
        assertTrue(
            MaximizerCarriedFamiliars.needsEnthroneDiscovery(
                MaximizeSpec(primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MYS),
                CharacterState(),
                buckets,
                emptyMap(),
            ),
        )
    }
}
