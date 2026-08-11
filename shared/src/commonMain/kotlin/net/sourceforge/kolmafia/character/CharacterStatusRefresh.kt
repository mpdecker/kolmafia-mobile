package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest

/** Desktop [ApiRequest.updateStatus] routing for mobile status/effects refresh (Phase 408). */
object CharacterStatusRefresh {

    /** Quantum Terrarium qterrarium.php pre-fetch before status refresh (Phase 413). */
    suspend fun refreshWithQuantumPreflight(
        characterRequest: CharacterRequest?,
        character: KoLCharacter,
        effectManager: EffectManager? = null,
        preferences: Preferences? = null,
        familiarManager: FamiliarManager? = null,
    ): Boolean {
        val state = character.state.value
        if (state.inQuantum && characterRequest != null) {
            val parseResult = QuantumTerrariumRequest.refresh(
                characterRequest.client,
                character,
                preferences,
            )
            if (parseResult.needsStatusRefresh) {
                refreshFromApi(characterRequest, character, effectManager)
            }
        }
        return refresh(characterRequest, character, effectManager, preferences, familiarManager)
    }

    fun needsCharpaneFallback(state: CharacterState): Boolean =
        CharpaneValhallaSync.inValhalla ||
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
        familiarManager: FamiliarManager? = null,
    ): Boolean {
        val state = character.state.value
        return if (CharpaneValhallaSync.inValhalla || needsCharpaneFallback(state)) {
            refreshFromCharpane(characterRequest, character, effectManager, preferences, familiarManager)
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
        familiarManager: FamiliarManager?,
    ): Boolean {
        val req = characterRequest ?: return false
        val html = req.fetchCharpaneHtml().getOrNull() ?: return false
        val state = character.state.value
        if (CharpaneValhallaSync.isValhallaHtml(html, state.limitMode)) {
            CharpaneValhallaSync.apply(character, html, preferences, effectManager)
        } else {
            CharpaneValhallaSync.reset()
            CharpaneStatusSync.apply(character, html, preferences)
            if (state.inPokefam) {
                CharpanePokefamSync.apply(character, html, familiarManager)
            }
            effectManager?.applyEffectsFromCharpane(html)
        }
        return true
    }
}
