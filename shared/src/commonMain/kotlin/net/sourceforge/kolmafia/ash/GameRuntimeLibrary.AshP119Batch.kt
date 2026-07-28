package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.skill.SkillState

/**
 * ASH-P119 behavioral batch — mood_execute + zodiac sign booleans.
 */
internal fun GameRuntimeLibrary.registerAshP119Batch(scope: AshScope) {
    regFn(scope, "mood_execute", AshType.VOID, listOf("multiplicity" to AshType.INT)) { _, _ ->
        if (recoveryManager?.isRecoveryActive == true || moodManager?.isExecuting() == true) {
            return@regFn AshValue.VOID
        }
        val mood = moodManager ?: return@regFn AshValue.VOID
        runBlocking {
            mood.checkpointedExecute(
                effectState = effectManager?.state?.value ?: EffectState(),
                skillState = skillManager?.state?.value ?: SkillState(),
                charState = character?.state?.value ?: CharacterState(),
                character = character,
                equipmentRequest = equipmentRequest,
                gameDatabase = gameDatabase,
            )
        }
        AshValue.VOID
    }

    regFn(scope, "in_muscle_sign", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign ?: ""
        AshValue.of(ZodiacSign.find(sign)?.isMuscle == true)
    }

    regFn(scope, "in_mysticality_sign", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign ?: ""
        AshValue.of(ZodiacSign.find(sign)?.isMysticality == true)
    }

    regFn(scope, "in_moxie_sign", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign ?: ""
        AshValue.of(ZodiacSign.find(sign)?.isMoxie == true)
    }

    regFn(scope, "in_bad_moon", AshType.BOOLEAN, emptyList()) { _, _ ->
        val sign = character?.state?.value?.zodiacSign ?: ""
        AshValue.of(ZodiacSign.find(sign)?.isBadMoon == true)
    }
}
