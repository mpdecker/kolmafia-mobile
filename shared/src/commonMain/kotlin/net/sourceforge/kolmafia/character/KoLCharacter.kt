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
            gender = Gender.fromApiString(response.gender),
            title = response.title,

            // HP / MP
            currentHp = response.hp.toIntOrNull() ?: 0,
            maxHp = response.hpmax.toIntOrNull() ?: 0,
            baseMaxHp = response.basehpmax.toIntOrNull() ?: response.hpmax.toIntOrNull() ?: 0,
            currentMp = response.mp.toIntOrNull() ?: 0,
            maxMp = response.mpmax.toIntOrNull() ?: 0,
            baseMaxMp = response.basempmax.toIntOrNull() ?: response.mpmax.toIntOrNull() ?: 0,

            // Base stats & subpoints
            baseMusc = response.mus.toIntOrNull() ?: 0,
            muscSubpoints = response.musexp.toLongOrNull() ?: 0L,
            baseMyst = response.mys.toIntOrNull() ?: 0,
            mystSubpoints = response.mysexp.toLongOrNull() ?: 0L,
            baseMoxie = response.mox.toIntOrNull() ?: 0,
            moxieSubpoints = response.moxexp.toLongOrNull() ?: 0L,

            // Buffed stats — fall back to base if API omits buffed values
            buffedMusc = response.buffedmus.toIntOrNull()
                ?: response.mus.toIntOrNull() ?: 0,
            buffedMyst = response.buffedmys.toIntOrNull()
                ?: response.mys.toIntOrNull() ?: 0,
            buffedMoxie = response.buffedmox.toIntOrNull()
                ?: response.mox.toIntOrNull() ?: 0,

            // Currency
            meat = response.meat.toIntOrNull() ?: 0,
            storageMeat = response.storagemeat.toLongOrNull() ?: 0L,

            // Turns / days
            adventuresLeft = response.adventures.toIntOrNull() ?: 0,
            turnsPlayed = response.turnsplayed.toIntOrNull() ?: 0,
            currentRun = response.currentrun.toIntOrNull() ?: 0,
            dayCount = response.daycount.toIntOrNull() ?: 0,
            rolloverTimestamp = response.rollover.toLongOrNull() ?: 0L,

            // Consumables
            fullness = response.fullness.toIntOrNull() ?: 0,
            inebriety = response.drunk.toIntOrNull() ?: 0,
            spleenUsed = response.spleen.toIntOrNull() ?: 0,
            fullnessLimit = response.stomachsize.toIntOrNull() ?: 15,
            inebrietyLimit = response.liversize.toIntOrNull() ?: 14,
            spleenLimit = response.spleensize.toIntOrNull() ?: 15,

            // PvP
            pvpFightsLeft = response.pvpfights.toIntOrNull() ?: 0,
            hippyStoneBroken = response.hippystone == "1",

            // Ascension / mode
            roninLeft = response.roninleft.toIntOrNull() ?: 0,
            isHardcore = response.hardcore == "1",
            kingLiberated = response.kingliberated == "1",
            limitMode = response.limitmode,

            // Class-specific resources
            fury = response.fury.toIntOrNull() ?: 0,
            soulsauce = response.soulsauce.toIntOrNull() ?: 0,
            thunder = response.thunder.toIntOrNull() ?: 0,
            rain = response.rain.toIntOrNull() ?: 0,
            lightning = response.lightning.toIntOrNull() ?: 0,
            currentPP = response.pp.toIntOrNull() ?: 0,
            maximumPP = response.ppmax.toIntOrNull() ?: 0,
            youRobotEnergy = response.robonenergy.toIntOrNull() ?: 0,
            youRobotScraps = response.robonscraps.toIntOrNull() ?: 0,
            wildfireWater = response.wildfirewater.toIntOrNull() ?: 0,
            minstrelLevel = response.minstrel.toIntOrNull() ?: 0,

            // Social / access
            hasStore = response.hasstore == "1",
            hasDisplayCase = response.hasdisplaycase == "1",
            hasClan = response.hasclan == "1",

            // Campground
            telescopeUpgrades = response.telescopelevel.toIntOrNull() ?: 0,

            // Misc
            mindControlLevel = response.mcd.toIntOrNull() ?: 0,
            radSickness = response.radsickness.toIntOrNull() ?: 0,
            stillsAvailable = response.stills.toIntOrNull() ?: -1,
            arenaWins = response.arenawins.toIntOrNull() ?: 0,

            // Familiar
            familiarId = response.familiar.toIntOrNull() ?: 0,
            familiarName = response.familiarname,
            familiarWeight = response.familiarweight.toIntOrNull() ?: 0,
            familiarExp = response.familiarexp.toIntOrNull() ?: 0,
            enthronedFamiliarId = response.enthroned.toIntOrNull() ?: 0,
            enthronedFamiliarName = response.enthronedname,
            bjornedFamiliarId = response.bjorned.toIntOrNull() ?: 0,
            bjornedFamiliarName = response.bjornedname,

            // Moon
            moonPhase = response.moonphase.toIntOrNull() ?: 0,
            moonSign = response.moonsign.toIntOrNull() ?: 0,
            moonDay = response.moonday.toIntOrNull() ?: 0,

            // Equipment
            equipment = buildEquipmentMap(response),

            isLoggedIn = true
        )
    }

    // ── Partial update helpers — avoid full API round-trips for frequent changes ──

    fun updateHpMp(currentHp: Int, maxHp: Int, currentMp: Int, maxMp: Int) {
        _state.value = _state.value.copy(
            currentHp = currentHp, maxHp = maxHp,
            currentMp = currentMp, maxMp = maxMp
        )
    }

    fun updateMeat(meat: Int, storageMeat: Long = _state.value.storageMeat) {
        _state.value = _state.value.copy(meat = meat, storageMeat = storageMeat)
    }

    fun addSessionMeat(delta: Long) {
        _state.value = _state.value.copy(sessionMeat = _state.value.sessionMeat + delta)
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

    fun updateClassResource(fury: Int? = null, soulsauce: Int? = null,
                            discoMomentum: Int? = null, audience: Int? = null,
                            absorbs: Int? = null) {
        _state.value = _state.value.copy(
            fury = fury ?: _state.value.fury,
            soulsauce = soulsauce ?: _state.value.soulsauce,
            discoMomentum = discoMomentum ?: _state.value.discoMomentum,
            audience = audience ?: _state.value.audience,
            absorbs = absorbs ?: _state.value.absorbs
        )
    }

    fun updatePlumberResources(thunder: Int, rain: Int, lightning: Int,
                               currentPP: Int = _state.value.currentPP,
                               maximumPP: Int = _state.value.maximumPP) {
        _state.value = _state.value.copy(
            thunder = thunder, rain = rain, lightning = lightning,
            currentPP = currentPP, maximumPP = maximumPP
        )
    }

    fun setCampground(telescopeUpgrades: Int = _state.value.telescopeUpgrades,
                      hasBookshelf: Boolean = _state.value.hasBookshelf) {
        _state.value = _state.value.copy(
            telescopeUpgrades = telescopeUpgrades,
            hasBookshelf = hasBookshelf
        )
    }

    fun setMindControlLevel(level: Int) {
        _state.value = _state.value.copy(mindControlLevel = level)
    }

    fun setCurrentRun(run: Int) {
        _state.value = _state.value.copy(currentRun = run)
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
