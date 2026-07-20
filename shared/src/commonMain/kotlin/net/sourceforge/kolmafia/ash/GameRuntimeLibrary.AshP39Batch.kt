package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP39 — combat adjustment ASH library.
 * Mirrors desktop [RuntimeLibrary] expected_damage / monster_level_adjustment / elemental_resistance
 * and related DA/DR/mana/weight wrappers.
 */
internal fun GameRuntimeLibrary.registerAshP39Batch(scope: AshScope) {
    regFn(scope, "monster_level_adjustment", AshType.INT, emptyList()) { _, _ ->
        val mods = buildCurrentModifiers()
        AshValue.of(
            CombatAdjustment.monsterLevelAdjustment(
                mods,
                character?.state?.value,
                lastLocationName(),
            ).toLong(),
        )
    }

    regFn(scope, "weight_adjustment", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.weightAdjustment(buildCurrentModifiers()).toLong())
    }

    regFn(scope, "mana_cost_modifier", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.manaCostModifier(buildCurrentModifiers(), combat = false).toLong())
    }

    regFn(scope, "combat_mana_cost_modifier", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.manaCostModifier(buildCurrentModifiers(), combat = true).toLong())
    }

    regFn(scope, "raw_damage_absorption", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(buildCurrentModifiers().values.get(DoubleModifier.DAMAGE_ABSORPTION).toLong())
    }

    regFn(scope, "damage_absorption_percent", AshType.FLOAT, emptyList()) { _, _ ->
        val raw = buildCurrentModifiers().values.get(DoubleModifier.DAMAGE_ABSORPTION).toInt()
        AshValue.of(CombatAdjustment.damageAbsorptionPercent(raw))
    }

    regFn(scope, "damage_reduction", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(buildCurrentModifiers().values.get(DoubleModifier.DAMAGE_REDUCTION).toLong())
    }

    regFn(scope, "combat_rate_modifier", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(
            CombatAdjustment.combatRateModifier(buildCurrentModifiers(), lastLocationName()),
        )
    }

    regFn(scope, "elemental_resistance", AshType.FLOAT, emptyList()) { _, _ ->
        val monsterName = preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""
        val monster = resolveMonsterDefinition(monsterName)
        AshValue.of(
            CombatAdjustment.elementalResistancePercent(
                buildCurrentModifiers(),
                monster?.attackElement.orEmpty(),
                character?.state?.value,
            ),
        )
    }

    regFn(scope, "elemental_resistance", AshType.FLOAT, listOf("element" to AshType.ELEMENT)) { _, args ->
        AshValue.of(
            CombatAdjustment.elementalResistancePercent(
                buildCurrentModifiers(),
                args[0].toString(),
                character?.state?.value,
            ),
        )
    }

    regFn(scope, "elemental_resistance", AshType.FLOAT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monster = resolveMonsterDefinition(args[0].toString())
        AshValue.of(
            CombatAdjustment.elementalResistancePercent(
                buildCurrentModifiers(),
                monster?.attackElement.orEmpty(),
                character?.state?.value,
            ),
        )
    }

    regFn(scope, "expected_damage", AshType.INT, emptyList()) { _, _ ->
        val monsterName = preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""
        val mods = buildCurrentModifiers()
        val state = character?.state?.value
        val ml = CombatAdjustment.monsterLevelAdjustment(mods, state, lastLocationName())
        AshValue.of(
            CombatAdjustment.expectedDamage(
                resolveMonsterDefinition(monsterName),
                state,
                mods,
                ml = ml,
                expressionContext = buildMonsterExpressionContext(),
            ).toLong(),
        )
    }

    regFn(scope, "expected_damage", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val mods = buildCurrentModifiers()
        val state = character?.state?.value
        val ml = CombatAdjustment.monsterLevelAdjustment(mods, state, lastLocationName())
        AshValue.of(
            CombatAdjustment.expectedDamage(
                resolveMonsterDefinition(args[0].toString()),
                state,
                mods,
                ml = ml,
                expressionContext = buildMonsterExpressionContext(),
            ).toLong(),
        )
    }
}
