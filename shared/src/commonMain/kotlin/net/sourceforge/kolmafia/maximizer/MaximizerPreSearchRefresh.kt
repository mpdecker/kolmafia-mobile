package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterStatusRefresh
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop `ApiRequest.updateStatus()` parity at maximize/speculate entry (Phase 409). */
object MaximizerPreSearchRefresh {

    suspend fun refresh(
        inventoryManager: InventoryManager,
        effectManager: EffectManager?,
        character: KoLCharacter,
        characterRequest: CharacterRequest?,
        preferences: Preferences?,
        skillManager: SkillManager? = null,
        familiarManager: FamiliarManager? = null,
    ) {
        skillManager?.fetchSkills()
        if (characterRequest != null) {
            CharacterStatusRefresh.refreshWithQuantumPreflight(
                characterRequest = characterRequest,
                character = character,
                effectManager = effectManager,
                preferences = preferences,
                familiarManager = familiarManager,
            )
        } else {
            inventoryManager.refreshCharacterStatus(effectManager, familiarManager)
        }
    }
}
