package net.sourceforge.kolmafia.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KoLCharacter {
    private val _state = MutableStateFlow(CharacterState())
    val state: StateFlow<CharacterState> = _state.asStateFlow()

    fun updateFromApiResponse(response: CharacterApiResponse) {
        _state.value = CharacterState(
            // Identity
            name = response.name,
            playerId = response.playerid.toIntOrNull() ?: 0,
            level = response.level.toIntOrNull() ?: 1,
            characterClass = response.classId.toIntOrNull() ?: 0,
            zodiacSign = response.sign,
            challengePath = response.path,
            ascensionNumber = response.ascensions.toIntOrNull() ?: 0,

            // HP / MP
            currentHp = response.hp.toIntOrNull() ?: 0,
            maxHp = response.hpmax.toIntOrNull() ?: 0,
            currentMp = response.mp.toIntOrNull() ?: 0,
            maxMp = response.mpmax.toIntOrNull() ?: 0,

            // Base stats
            baseMusc = response.mus.toIntOrNull() ?: 0,
            muscSubpoints = response.musexp.toIntOrNull() ?: 0,
            baseMyst = response.mys.toIntOrNull() ?: 0,
            mystSubpoints = response.mysexp.toIntOrNull() ?: 0,
            baseMoxie = response.mox.toIntOrNull() ?: 0,
            moxieSubpoints = response.moxexp.toIntOrNull() ?: 0,

            // Buffed stats — fall back to base if buffed values absent
            buffedMusc = response.buffedmus.toIntOrNull()
                ?: response.mus.toIntOrNull() ?: 0,
            buffedMyst = response.buffedmys.toIntOrNull()
                ?: response.mys.toIntOrNull() ?: 0,
            buffedMoxie = response.buffedmox.toIntOrNull()
                ?: response.mox.toIntOrNull() ?: 0,

            // Currency / turns
            meat = response.meat.toIntOrNull() ?: 0,
            adventuresLeft = response.adventures.toIntOrNull() ?: 0,
            turnsPlayed = response.turnsplayed.toIntOrNull() ?: 0,
            dayCount = response.daycount.toIntOrNull() ?: 0,

            // Consumables
            fullness = response.fullness.toIntOrNull() ?: 0,
            inebriety = response.drunk.toIntOrNull() ?: 0,
            spleenUsed = response.spleen.toIntOrNull() ?: 0,
            fullnessLimit = response.stomachsize.toIntOrNull() ?: 15,
            inebrietyLimit = response.liversize.toIntOrNull() ?: 14,
            spleenLimit = response.spleensize.toIntOrNull() ?: 15,

            // PvP
            pvpFightsLeft = response.pvpfights.toIntOrNull() ?: 0,

            // Ascension / mode
            roninLeft = response.roninleft.toIntOrNull() ?: 0,
            isHardcore = response.hardcore == "1",
            limitMode = response.limitmode,

            // Familiar
            familiarId = response.familiar.toIntOrNull() ?: 0,
            familiarName = response.familiarname,
            familiarWeight = response.familiarweight.toIntOrNull() ?: 0,
            familiarExp = response.familiarexp.toIntOrNull() ?: 0,

            // Moon
            moonPhase = response.moonphase.toIntOrNull() ?: 0,
            moonSign = response.moonsign.toIntOrNull() ?: 0,
            moonDay = response.moonday.toIntOrNull() ?: 0,

            // Equipment
            equipment = buildEquipmentMap(response),

            isLoggedIn = true
        )
    }

    // Merge partial updates without overwriting fields not present in the payload.
    // Useful when only HP/MP changes (e.g. after combat) rather than a full refresh.
    fun updateHpMp(currentHp: Int, maxHp: Int, currentMp: Int, maxMp: Int) {
        _state.value = _state.value.copy(
            currentHp = currentHp, maxHp = maxHp,
            currentMp = currentMp, maxMp = maxMp
        )
    }

    fun updateMeat(meat: Int) {
        _state.value = _state.value.copy(meat = meat)
    }

    fun updateAdventuresLeft(adventures: Int) {
        _state.value = _state.value.copy(adventuresLeft = adventures)
    }

    fun updateConsumables(fullness: Int, inebriety: Int, spleenUsed: Int) {
        _state.value = _state.value.copy(
            fullness = fullness, inebriety = inebriety, spleenUsed = spleenUsed
        )
    }

    fun updateFamiliar(id: Int, name: String, weight: Int, exp: Int) {
        _state.value = _state.value.copy(
            familiarId = id, familiarName = name,
            familiarWeight = weight, familiarExp = exp
        )
    }

    fun updateEquipment(slot: EquipmentSlot, itemName: String) {
        val updated = _state.value.equipment.toMutableMap()
        if (itemName.isBlank()) updated.remove(slot) else updated[slot] = itemName
        _state.value = _state.value.copy(equipment = updated)
    }

    fun setIntrinsics(names: List<String>) {
        _state.value = _state.value.copy(intrinsics = names)
    }

    fun reset() {
        _state.value = CharacterState()
    }

    private fun buildEquipmentMap(r: CharacterApiResponse): Map<EquipmentSlot, String> {
        val map = mutableMapOf<EquipmentSlot, String>()
        if (r.hat.isNotBlank())           map[EquipmentSlot.HAT]       = r.hat
        if (r.weapon.isNotBlank())        map[EquipmentSlot.WEAPON]    = r.weapon
        if (r.offhand.isNotBlank())       map[EquipmentSlot.OFFHAND]   = r.offhand
        if (r.shirt.isNotBlank())         map[EquipmentSlot.SHIRT]     = r.shirt
        if (r.pants.isNotBlank())         map[EquipmentSlot.PANTS]     = r.pants
        if (r.acc1.isNotBlank())          map[EquipmentSlot.ACC1]      = r.acc1
        if (r.acc2.isNotBlank())          map[EquipmentSlot.ACC2]      = r.acc2
        if (r.acc3.isNotBlank())          map[EquipmentSlot.ACC3]      = r.acc3
        if (r.familiarequip.isNotBlank()) map[EquipmentSlot.FAMILIAR]  = r.familiarequip
        if (r.container.isNotBlank())     map[EquipmentSlot.CONTAINER] = r.container
        return map
    }
}
