package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility

/**
 * ASH-P120 behavioral batch — consumption eligibility and capacity limits.
 */
internal fun GameRuntimeLibrary.registerAshP120Batch(scope: AshScope) {
    fun state() = character?.state?.value ?: CharacterState()
    fun skills() = skillManager?.state?.value?.skills ?: emptyList()

    regFn(scope, "can_eat", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ConsumptionEligibility.canEat(state(), skills()))
    }

    regFn(scope, "can_drink", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ConsumptionEligibility.canDrink(state(), skills()))
    }

    regFn(scope, "fullness_limit", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(
            ConsumptionEligibility.stomachCapacity(state(), skills(), buildCurrentModifiers()).toLong(),
        )
    }

    regFn(scope, "inebriety_limit", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(
            ConsumptionEligibility.liverCapacity(state(), skills(), buildCurrentModifiers()).toLong(),
        )
    }

    regFn(scope, "spleen_limit", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(ConsumptionEligibility.spleenCapacity(state()).toLong())
    }
}
