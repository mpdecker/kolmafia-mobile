package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/**
 * ASH-P138 behavioral batch — NPC shop sync + coinmaster validate v2.
 */
internal fun GameRuntimeLibrary.registerAshP138Batch(scope: AshScope) {
    regFn(scope, "is_coinmaster_item", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        AshValue.of(
            CoinmasterDatabase.containsBuyItem(
                id,
                validate = false,
                state = craftCharacterState(),
                prefs = preferences,
                accessibleCount = { itemId -> craftAccessibleCount(itemId) },
            ),
        )
    }

    regFn(scope, "is_coinmaster_item", AshType.BOOLEAN, listOf("id" to AshType.INT, "validate" to AshType.BOOLEAN)) { _, args ->
        val id = args[0].toLong().toInt()
        val validate = args[1].toBoolean()
        AshValue.of(
            CoinmasterDatabase.containsBuyItem(
                id,
                validate = validate,
                state = craftCharacterState(),
                prefs = preferences,
                accessibleCount = { itemId -> craftAccessibleCount(itemId) },
                hasSkill = { skillId -> craftSkills().any { it.id == skillId } },
                hasEffect = { effectId -> hasActiveEffect(effectId) },
            ),
        )
    }
}
