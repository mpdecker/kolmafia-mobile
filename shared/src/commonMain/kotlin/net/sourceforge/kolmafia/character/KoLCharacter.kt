package net.sourceforge.kolmafia.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KoLCharacter {
    private val _state = MutableStateFlow(CharacterState())
    val state: StateFlow<CharacterState> = _state.asStateFlow()

    fun updateFromApiResponse(response: CharacterApiResponse) {
        _state.value = CharacterState(
            name = response.name,
            playerId = response.playerid.toIntOrNull() ?: 0,
            level = response.level.toIntOrNull() ?: 1,
            characterClass = response.classId.toIntOrNull() ?: 0,
            currentHp = response.hp.toIntOrNull() ?: 0,
            maxHp = response.hpmax.toIntOrNull() ?: 0,
            currentMp = response.mp.toIntOrNull() ?: 0,
            maxMp = response.mpmax.toIntOrNull() ?: 0,
            meat = response.meat.toIntOrNull() ?: 0,
            adventuresLeft = response.adventures.toIntOrNull() ?: 0,
            fullness = response.fullness.toIntOrNull() ?: 0,
            inebriety = response.drunk.toIntOrNull() ?: 0,
            spleenUsed = response.spleen.toIntOrNull() ?: 0,
            isLoggedIn = true
        )
    }

    fun reset() {
        _state.value = CharacterState()
    }
}
