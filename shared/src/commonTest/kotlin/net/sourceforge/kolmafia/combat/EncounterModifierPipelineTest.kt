package net.sourceforge.kolmafia.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.familiar.FamiliarIds

class EncounterModifierPipelineTest {

    private fun ctx(
        familiarId: Int = 0,
        path: AscensionPath = AscensionPath.NONE,
    ) = EncounterModifierPipeline.EncounterModifierContext(familiarId, path)

    @Test
    fun mask_disguisesDelimit_stripsNameAndAddsModifier() {
        val modifiers = mutableListOf<String>()
        val result = EncounterModifierPipeline.applyPostOcrs(
            "Naughty Sorceress wearing a Boss Bat mask",
            modifiers,
            ctx(path = AscensionPath.DISGUISES_DELIMIT),
        )
        assertEquals("Naughty Sorceress", result)
        assertEquals(listOf("Boss Bat mask"), modifiers)
    }

    @Test
    fun intergnat_bacon_stripsSuffixAndAddsModifier() {
        val modifiers = mutableListOf<String>()
        val result = EncounterModifierPipeline.applyPostOcrs(
            "giant skeletor WITH BACON!!!",
            modifiers,
            ctx(familiarId = FamiliarIds.INTERGNAT),
        )
        assertEquals("giant skeletor", result)
        assertEquals(listOf("bacon"), modifiers)
    }

    @Test
    fun nuclearAutumn_mutant_stripsPrefixAndAddsModifier() {
        val modifiers = mutableListOf<String>()
        val result = EncounterModifierPipeline.applyPostOcrs(
            "mutant Knob Goblin",
            modifiers,
            ctx(path = AscensionPath.NUCLEAR_AUTUMN),
        )
        assertEquals("Knob Goblin", result)
        assertEquals(listOf("mutant"), modifiers)
    }

    @Test
    fun dinosaurs_stripsTypeAndModifier() {
        val modifiers = mutableListOf<String>()
        val result = EncounterModifierPipeline.applyPostOcrs(
            "cold-blooded chicken huge mosquito",
            modifiers,
            ctx(path = AscensionPath.DINOSAURS),
        )
        assertEquals("huge mosquito", result.trim())
        assertEquals(listOf("chicken", "cold-blooded"), modifiers)
    }

    @Test
    fun hatTrick_parsesMultipleHats() {
        val modifiers = mutableListOf<String>()
        val result = EncounterModifierPipeline.applyPostOcrs(
            "black adder wearing a terrycloth turban and a tinfoil hat",
            modifiers,
            ctx(path = AscensionPath.HAT_TRICK),
        )
        assertEquals("black adder", result)
        assertEquals(listOf("terrycloth turban", "tinfoil hat"), modifiers)
    }

    @Test
    fun noPathGate_leavesNameUnchanged() {
        val modifiers = mutableListOf<String>()
        val input = "Naughty Sorceress wearing a Boss Bat mask"
        val result = EncounterModifierPipeline.applyPostOcrs(input, modifiers, ctx())
        assertEquals(input, result)
        assertEquals(emptyList(), modifiers)
    }
}
