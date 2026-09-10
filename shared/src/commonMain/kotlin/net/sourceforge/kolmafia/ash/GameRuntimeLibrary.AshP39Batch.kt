package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierStats
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.primaryAttackElement
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP39 — combat adjustment ASH library.
 * Mirrors desktop [RuntimeLibrary] expected_damage / monster_level_adjustment / elemental_resistance
 * and related DA/DR/mana/weight wrappers.
 *
 * Phase 6431–6440: FightRequest-style expected_damage (tracker attackModifier + Hero of the
 * Half-Shell shield defense) and elemental_resistance last-monster tracker overlay.
 *
 * Phase 6611–6630 (XLVI Track A): RandomModifierStats (OCRS) attack/element overlay +
 * primaryAttackElement fallback + residual FightRequest absorb/res edges.
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

    fun effectiveMonster(raw: MonsterDefinition?): MonsterDefinition? {
        if (raw == null) return null
        return RandomModifierStats.apply(raw, raw.randomModifiers, buildMonsterExpressionContext())
    }

    fun attackElementOf(monster: MonsterDefinition?): String {
        if (monster == null) return ""
        return monster.attackElement.ifBlank { primaryAttackElement(monster.attackElements) }
    }

    fun lastEffectiveMonster(): MonsterDefinition? {
        val raw = MonsterStatusTracker.getLastMonster()
            ?: resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "").orEmpty())
        return effectiveMonster(raw)
    }

    regFn(scope, "elemental_resistance", AshType.FLOAT, emptyList()) { _, _ ->
        val monster = lastEffectiveMonster()
        AshValue.of(
            CombatAdjustment.elementalResistancePercent(
                buildCurrentModifiers(),
                attackElementOf(monster),
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
        val monster = effectiveMonster(resolveMonsterDefinition(args[0].toString()))
        AshValue.of(
            CombatAdjustment.elementalResistancePercent(
                buildCurrentModifiers(),
                attackElementOf(monster),
                character?.state?.value,
            ),
        )
    }

    fun hasHeroOfTheHalfShell(): Boolean =
        skillManager?.state?.value?.skills?.any {
            it.id == HERO_OF_THE_HALF_SHELL_SKILL_ID ||
                it.name.equals("Hero of the Half-Shell", ignoreCase = true)
        } == true

    fun usingShield(): Boolean = equipmentManager?.usingShield() == true

    regFn(scope, "expected_damage", AshType.INT, emptyList()) { _, _ ->
        val monster = lastEffectiveMonster()
        val mods = buildCurrentModifiers()
        val state = character?.state?.value
        val ml = CombatAdjustment.monsterLevelAdjustment(mods, state, lastLocationName())
        AshValue.of(
            CombatAdjustment.expectedDamage(
                monster,
                state,
                mods,
                attackModifier = MonsterStatusTracker.getMonsterAttackModifier(),
                ml = ml,
                expressionContext = buildMonsterExpressionContext(),
                usingShield = usingShield(),
                hasHeroOfTheHalfShell = hasHeroOfTheHalfShell(),
            ).toLong(),
        )
    }

    regFn(scope, "expected_damage", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val mods = buildCurrentModifiers()
        val state = character?.state?.value
        val ml = CombatAdjustment.monsterLevelAdjustment(mods, state, lastLocationName())
        AshValue.of(
            CombatAdjustment.expectedDamage(
                effectiveMonster(resolveMonsterDefinition(args[0].toString())),
                state,
                mods,
                attackModifier = 0,
                ml = ml,
                expressionContext = buildMonsterExpressionContext(),
                usingShield = usingShield(),
                hasHeroOfTheHalfShell = hasHeroOfTheHalfShell(),
            ).toLong(),
        )
    }
}

/** Desktop [SkillPool.HERO_OF_THE_HALF_SHELL]. */
internal const val HERO_OF_THE_HALF_SHELL_SKILL_ID = 2020
