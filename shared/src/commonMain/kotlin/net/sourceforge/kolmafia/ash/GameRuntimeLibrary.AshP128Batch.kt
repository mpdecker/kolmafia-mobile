package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.ConsumptionEligibility

/**
 * ASH-P128 behavioral batch — consumption expand guards + craft permission depth hooks.
 */
internal fun GameRuntimeLibrary.registerAshP128Batch(scope: AshScope) {
    fun state() = craftCharacterState()
    fun skills() = craftSkills()

    regFn(scope, "can_expand_stomach", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ConsumptionEligibility.canExpandStomach(state(), skills()))
    }

    regFn(scope, "can_expand_liver", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ConsumptionEligibility.canExpandLiver(state(), skills()))
    }

    regFn(scope, "is_craft_permitted", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        AshValue.of(isCraftPermitted(itemId))
    }
}
