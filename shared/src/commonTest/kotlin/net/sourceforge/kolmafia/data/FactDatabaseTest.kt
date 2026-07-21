package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class FactDatabaseTest {

    @Test
    fun load_parsesBookOfFacts() = runBlocking {
        ItemDatabase.load()
        EffectDatabase.load()
        FactDatabase.load()
        assertTrue(FactDatabase.loadedFactCount >= 80)
    }

    @Test
    fun calculateSeed_mosquitoDefaultClass() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(1341, FactDatabase.calculateSeed(CharacterClass.UNKNOWN, AscensionPath.NONE, mosquito))
    }

    @Test
    fun calculateSeed_mosquitoSealClubber() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(
            1762,
            FactDatabase.calculateSeed(CharacterClass.SEAL_CLUBBER, AscensionPath.NONE, mosquito),
        )
    }

    @Test
    fun getFact_mosquitoNonePoolWithoutCharacter() = runBlocking {
        loadAll()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val fact = FactDatabase.getFact(
            mosquito,
            CharacterClass.UNKNOWN,
            AscensionPath.NONE,
            stateful = true,
            ExpressionContext.EMPTY,
        )
        assertEquals("effect", fact.type.toString())
        assertEquals("Disabled Olfactory Processing (10)", fact.display)
        val again = FactDatabase.getFact(
            mosquito,
            CharacterClass.UNKNOWN,
            AscensionPath.NONE,
            stateful = true,
            ExpressionContext.EMPTY,
        )
        assertEquals(fact.display, again.display)
    }

    @Test
    fun getFact_mosquitoBugPoolWithSealClubber() = runBlocking {
        loadAll()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val fact = FactDatabase.getFact(
            mosquito,
            CharacterClass.SEAL_CLUBBER,
            AscensionPath.NONE,
            stateful = true,
            ExpressionContext.EMPTY,
        )
        assertEquals("effect", fact.type.toString())
        assertEquals("Industrial Strength Starch (15)", fact.display)
    }

    @Test
    fun getFact_penguinMeatBase() = runBlocking {
        loadAll()
        val monster = MonsterDatabase.getByName("mob penguin") ?: return@runBlocking
        val fact = FactDatabase.getFact(
            monster,
            CharacterClass.UNKNOWN,
            AscensionPath.NONE,
            stateful = true,
            ExpressionContext.EMPTY,
        )
        if (fact.type.toString() == "meat") {
            assertTrue(fact.display.endsWith(" Meat"))
        }
    }

    @Test
    fun getFact_underTheSeaFishySwap() = runBlocking {
        loadAll()
        val monster = MonsterDatabase.all().first { it.phylum == "fish" }
        var foundFishy = false
        repeat(50) { classId ->
            val fact = FactDatabase.getFact(
                monster,
                CharacterClass.fromId(classId),
                AscensionPath.UNDER_THE_SEA,
                stateful = true,
                ExpressionContext.EMPTY,
            )
            if (fact.display == "Fishy Fortification (10)") {
                foundFishy = true
            }
        }
        assertTrue(foundFishy)
    }

    fun getFact_unknownMonsterEmpty() {
        assertEquals("", FactDatabase.factString(null, CharacterClass.UNKNOWN, AscensionPath.NONE, null))
        assertEquals("", FactDatabase.factTypeString(null, CharacterClass.UNKNOWN, AscensionPath.NONE, null))
    }

    private suspend fun loadAll() {
        ItemDatabase.load()
        EffectDatabase.load()
        MonsterDatabase.load()
        FactDatabase.load()
    }
}
