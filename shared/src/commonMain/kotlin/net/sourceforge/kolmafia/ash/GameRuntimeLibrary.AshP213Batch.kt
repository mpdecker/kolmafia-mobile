package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/**
 * AshP213 — shoprows pref write-back + public is_coinmaster_skill ASH.
 */
internal fun GameRuntimeLibrary.registerAshP213Batch(scope: AshScope) {
    fun skillId(arg: AshValue): Int? = when (arg.type) {
        AshType.INT -> arg.toLong().toInt()
        AshType.SKILL -> {
            val name = arg.toString()
            gameDatabase?.skill(name)?.id
                ?: SkillDefinitionDatabase.getByName(name)?.id
                ?: skillManager?.state?.value?.skills?.find { it.name.equals(name, ignoreCase = true) }?.id
        }
        else -> null
    }

    regFn(scope, "is_coinmaster_skill", AshType.BOOLEAN, listOf("sk" to AshType.SKILL)) { _, args ->
        val id = skillId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(
            CoinmasterDatabase.containsBuySkill(
                skillId = id,
                validate = false,
                state = craftCharacterState(),
                prefs = preferences,
            ),
        )
    }

    regFn(scope, "is_coinmaster_skill", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        AshValue.of(
            CoinmasterDatabase.containsBuySkill(
                skillId = id,
                validate = false,
                state = craftCharacterState(),
                prefs = preferences,
            ),
        )
    }

    regFn(scope, "is_coinmaster_skill", AshType.BOOLEAN, listOf("id" to AshType.INT, "validate" to AshType.BOOLEAN)) { _, args ->
        val id = args[0].toLong().toInt()
        val validate = args[1].toBoolean()
        AshValue.of(
            CoinmasterDatabase.containsBuySkill(
                skillId = id,
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
