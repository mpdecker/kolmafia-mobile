package net.sourceforge.kolmafia.ash

/**
 * AshP40 — drop / XP / initiative ASH wrappers.
 * Mirrors desktop [RuntimeLibrary] initiative_modifier / experience_bonus /
 * meat_drop_modifier / item_drop_modifier using [KoLCharacter] formulas.
 */
internal fun GameRuntimeLibrary.registerAshP40Batch(scope: AshScope) {
    regFn(scope, "initiative_modifier", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.initiativeModifier(buildCurrentModifiers()))
    }

    regFn(scope, "experience_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(
            CombatAdjustment.experienceBonus(
                buildCurrentModifiers(),
                character?.state?.value,
            ),
        )
    }

    regFn(scope, "meat_drop_modifier", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.meatDropModifier(buildCurrentModifiers()))
    }

    regFn(scope, "item_drop_modifier", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.itemDropModifier(buildCurrentModifiers()))
    }
}
