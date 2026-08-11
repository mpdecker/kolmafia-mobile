package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.effect.CharpaneEffectsSync
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest

/** Desktop [ApiRequest.updateStatus] routing for mobile status/effects refresh (Phase 408). */
object CharacterStatusRefresh {

    fun needsCharpaneFallback(state: CharacterState): Boolean =
        LimitModeGates.requiresCharPane(state.limitMode) ||
            state.inNoobcore ||
            state.inPokefam ||
            state.inDisguise ||
            CharpaneStatusSync.hasTransfunctionerEquipped(state)

    suspend fun refresh(
        characterRequest: CharacterRequest?,
        character: KoLCharacter,
        effectManager: EffectManager? = null,
        preferences: Preferences? = null,
    ): Boolean {
        val state = character.state.value
        return if (needsCharpaneFallback(state)) {
            refreshFromCharpane(characterRequest, character, effectManager, preferences)
        } else {
            refreshFromApi(characterRequest, character, effectManager)
        }
    }

    private suspend fun refreshFromApi(
        characterRequest: CharacterRequest?,
        character: KoLCharacter,
        effectManager: EffectManager?,
    ): Boolean {
        val req = characterRequest ?: return false
        val statusOk = req.fetchCharacterState()
            .onSuccess { character.updateFromApiResponse(it) }
            .isSuccess
        effectManager?.fetchEffects()
        return statusOk
    }

    private suspend fun refreshFromCharpane(
        characterRequest: CharacterRequest?,
        character: KoLCharacter,
        effectManager: EffectManager?,
        preferences: Preferences?,
    ): Boolean {
        val req = characterRequest ?: return false
        val html = req.fetchCharpaneHtml().getOrNull() ?: return false
        CharpaneStatusSync.apply(character, html, preferences)
        effectManager?.applyEffectsFromCharpane(html)
        return true
    }
}
