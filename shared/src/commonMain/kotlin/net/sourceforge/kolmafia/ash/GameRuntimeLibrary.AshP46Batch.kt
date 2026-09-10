package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierStats
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP46 — will_usually_miss / will_usually_dodge / buffed_hit_stat / current_hit_stat.
 *
 * Phase 6431–6440: desktop [MonsterStatusTracker] mid-combat atk/def modifiers +
 * [EquipmentManager.getAdjustedHitStat] buffed-hit parity (saber / knife / accordion).
 *
 * Phase 6611–6630 (XLVI Track A): RandomModifierStats (OCRS) atk/def overlay +
 * ATTACKS_CANT_MISS miss short-circuit via [CombatAdjustment.willUsuallyMiss].
 */
internal fun GameRuntimeLibrary.registerAshP46Batch(scope: AshScope) {
    fun currentMl(): Int =
        CombatAdjustment.monsterLevelAdjustment(
            buildCurrentModifiers(),
            character?.state?.value,
            lastLocationName(),
        )

    fun effectiveMonster(raw: MonsterDefinition?): MonsterDefinition? {
        if (raw == null) return null
        return RandomModifierStats.apply(raw, raw.randomModifiers, buildMonsterExpressionContext())
    }

    fun lastMonster() =
        effectiveMonster(
            MonsterStatusTracker.getLastMonster()
                ?: resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""),
        )

    fun weaponName(): String? =
        character?.state?.value?.equippedItem(EquipmentSlot.WEAPON)

    fun hitStat(): Int {
        val em = equipmentManager
        if (em != null) {
            // Prefer desktop EquipmentManager hit-stat stack when wired.
            return em.getAdjustedHitStat()
        }
        return CombatAdjustment.buffedHitStat(
            character = character?.state?.value,
            modifiers = buildCurrentModifiers(),
            weaponName = weaponName(),
        )
    }

    fun buffedMoxie(): Int {
        val mods = buildCurrentModifiers()
        val fromMods = mods.buffedMoxie()
        if (fromMods != 0) return fromMods
        return character?.state?.value?.buffedMoxie ?: 0
    }

    regFn(scope, "will_usually_dodge", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(
            CombatAdjustment.willUsuallyDodge(
                monster = lastMonster(),
                buffedMoxie = buffedMoxie(),
                ml = currentMl(),
                offenseModifier = MonsterStatusTracker.getMonsterAttackModifier(),
                expressionContext = buildMonsterExpressionContext(),
            ),
        )
    }

    regFn(scope, "will_usually_miss", AshType.BOOLEAN, emptyList()) { _, _ ->
        val mods = buildCurrentModifiers()
        AshValue.of(
            CombatAdjustment.willUsuallyMiss(
                monster = lastMonster(),
                hitStat = hitStat(),
                ml = currentMl(),
                defenseModifier = MonsterStatusTracker.getMonsterDefenseModifier(),
                expressionContext = buildMonsterExpressionContext(),
                reduceEnemyDefensePercent = CombatAdjustment.reduceEnemyDefensePercent(mods),
            ),
        )
    }

    regFn(scope, "buffed_hit_stat", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(hitStat().toLong())
    }

    regFn(scope, "current_hit_stat", AshType.STAT, emptyList()) { _, _ ->
        val em = equipmentManager
        val name = if (em != null) {
            em.getHitStatType()
        } else {
            CombatAdjustment.currentHitStatName(weaponName())
        }
        AshValue(AshType.STAT, name)
    }
}
