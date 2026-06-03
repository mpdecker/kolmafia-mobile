package net.sourceforge.kolmafia.character

data class CharacterState(
    // Identity
    val name: String = "",
    val playerId: Int = 0,
    val level: Int = 1,
    val characterClass: Int = 0,
    val zodiacSign: String = "",
    val challengePath: String = "",

    // HP / MP
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val currentMp: Int = 0,
    val maxMp: Int = 0,

    // Base stats (unmodified)
    val baseMusc: Int = 0,
    val muscSubpoints: Int = 0,
    val baseMyst: Int = 0,
    val mystSubpoints: Int = 0,
    val baseMoxie: Int = 0,
    val moxieSubpoints: Int = 0,

    // Buffed stats (modified by equipment/effects — what you actually fight with)
    val buffedMusc: Int = 0,
    val buffedMyst: Int = 0,
    val buffedMoxie: Int = 0,

    // Currency / turns
    val meat: Int = 0,
    val adventuresLeft: Int = 0,
    val turnsPlayed: Int = 0,
    val dayCount: Int = 0,

    // Consumable tracking
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    // Caps vary by sign/path; defaults are standard KoL values
    val fullnessLimit: Int = 15,
    val inebrietyLimit: Int = 14,
    val spleenLimit: Int = 15,

    // PvP
    val pvpFightsLeft: Int = 0,

    // Ascension / mode
    val roninLeft: Int = 0,
    val isHardcore: Boolean = false,
    val ascensionNumber: Int = 0,
    val limitMode: String = "",  // "avatar", "spelunk", etc.; empty = normal

    // Equipment (slot → item name; empty string = unequipped)
    val equipment: Map<EquipmentSlot, String> = emptyMap(),

    // Session state
    val isLoggedIn: Boolean = false
) {
    val fullnessRemaining get() = fullnessLimit - fullness
    val inebrietyRemaining get() = inebrietyLimit - inebriety
    val spleenRemaining get() = spleenLimit - spleenUsed
    val isInRonin get() = roninLeft > 0
    val isRestricted get() = isHardcore || isInRonin

    fun equippedItem(slot: EquipmentSlot): String? = equipment[slot]?.takeIf { it.isNotBlank() }
}
