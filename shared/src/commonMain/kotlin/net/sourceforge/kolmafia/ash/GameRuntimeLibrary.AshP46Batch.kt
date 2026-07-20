package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP46 — will_usually_miss / will_usually_dodge / buffed_hit_stat / current_hit_stat.
 * Desktop-lite: last monster + ML-adjusted atk/def; mid-combat delevel modifiers = 0.
 */
internal fun GameRuntimeLibrary.registerAshP46Batch(scope: AshScope) {
    fun currentMl(): Int =
        CombatAdjustment.monsterLevelAdjustment(
            buildCurrentModifiers(),
            character?.state?.value,
            lastLocationName(),
        )

    fun lastMonster() =
        resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    fun weaponName(): String? =
        character?.state?.value?.equippedItem(EquipmentSlot.WEAPON)

    fun hitStat(): Int =
        CombatAdjustment.buffedHitStat(
            character = character?.state?.value,
            modifiers = buildCurrentModifiers(),
            weaponName = weaponName(),
        )

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
                expressionContext = buildMonsterExpressionContext(),
                reduceEnemyDefensePercent = CombatAdjustment.reduceEnemyDefensePercent(mods),
            ),
        )
    }

    regFn(scope, "buffed_hit_stat", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(hitStat().toLong())
    }

    regFn(scope, "current_hit_stat", AshType.STAT, emptyList()) { _, _ ->
        AshValue(AshType.STAT, CombatAdjustment.currentHitStatName(weaponName()))
    }
}
