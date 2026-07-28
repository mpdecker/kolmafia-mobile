package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.skill.SkillState

/**
 * ASH-P118 behavioral batch — status refresh and targeted HP/MP recovery.
 */
internal fun GameRuntimeLibrary.registerAshP118Batch(scope: AshScope) {
    regFn(scope, "refresh_status", AshType.BOOLEAN, emptyList()) { _, _ ->
        runBlocking {
            val ok = characterRequest?.fetchCharacterState()
                ?.onSuccess { character?.updateFromApiResponse(it) }
                ?.isSuccess == true
            AshValue.of(ok)
        }
    }

    regFn(scope, "restore_hp", AshType.BOOLEAN, listOf("amount" to AshType.INT)) { _, args ->
        val rm = recoveryManager ?: return@regFn AshValue.FALSE
        val amount = args[0].toLong().toInt()
        runBlocking {
            val charState = character?.state?.value ?: CharacterState()
            val invState = inventoryManager?.state?.value ?: InventoryState()
            val skillState = skillManager?.state?.value ?: SkillState()
            val ok = rm.checkpointedRecoverHp(amount, charState, invState, skillState) {
                refreshCharacterStates()
            }
            AshValue.of(ok)
        }
    }

    regFn(scope, "restore_mp", AshType.BOOLEAN, listOf("amount" to AshType.INT)) { _, args ->
        val rm = recoveryManager ?: return@regFn AshValue.FALSE
        val amount = args[0].toLong().toInt()
        runBlocking {
            val charState = character?.state?.value ?: CharacterState()
            val invState = inventoryManager?.state?.value ?: InventoryState()
            val skillState = skillManager?.state?.value ?: SkillState()
            val ok = rm.checkpointedRecoverMp(amount, charState, invState, skillState) {
                refreshCharacterStates()
            }
            AshValue.of(ok)
        }
    }
}

/** api.php status refresh (desktop ApiRequest.updateStatus scope). */
internal suspend fun GameRuntimeLibrary.refreshCharacterStates(): Triple<CharacterState, InventoryState, SkillState> {
    characterRequest?.fetchCharacterState()?.onSuccess { character?.updateFromApiResponse(it) }
    return Triple(
        character?.state?.value ?: CharacterState(),
        inventoryManager?.state?.value ?: InventoryState(),
        skillManager?.state?.value ?: SkillState(),
    )
}
