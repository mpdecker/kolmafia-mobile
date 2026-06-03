package net.sourceforge.kolmafia.character

data class CharacterState(
    // ── Identity ─────────────────────────────────────────────────────────────
    val name: String = "",
    val playerId: Int = 0,
    val level: Int = 1,
    val characterClass: Int = 0,
    val zodiacSign: String = "",
    val challengePath: String = "",
    val ascensionNumber: Int = 0,

    // ── HP / MP ──────────────────────────────────────────────────────────────
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val currentMp: Int = 0,
    val maxMp: Int = 0,

    // ── Base stats (unmodified by equipment/effects) ──────────────────────────
    val baseMusc: Int = 0,
    val muscSubpoints: Int = 0,
    val baseMyst: Int = 0,
    val mystSubpoints: Int = 0,
    val baseMoxie: Int = 0,
    val moxieSubpoints: Int = 0,

    // ── Buffed stats (equipment + effect modifiers applied) ───────────────────
    val buffedMusc: Int = 0,
    val buffedMyst: Int = 0,
    val buffedMoxie: Int = 0,

    // ── Currency / turn counters ──────────────────────────────────────────────
    val meat: Int = 0,
    val adventuresLeft: Int = 0,
    val turnsPlayed: Int = 0,
    val dayCount: Int = 0,

    // ── Consumable tracking ───────────────────────────────────────────────────
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    val fullnessLimit: Int = 15,
    val inebrietyLimit: Int = 14,
    val spleenLimit: Int = 15,

    // ── PvP ──────────────────────────────────────────────────────────────────
    val pvpFightsLeft: Int = 0,

    // ── Ascension / run mode ─────────────────────────────────────────────────
    val roninLeft: Int = 0,
    val isHardcore: Boolean = false,
    val limitMode: String = "",     // "avatar", "spelunk", etc.; empty = normal

    // ── Current familiar ──────────────────────────────────────────────────────
    // These mirror FamiliarManager's activeFamiliar for quick access.
    val familiarId: Int = 0,
    val familiarName: String = "",
    val familiarWeight: Int = 0,    // base weight before equipment/buffs
    val familiarExp: Int = 0,

    // ── Moon / calendar ──────────────────────────────────────────────────────
    val moonPhase: Int = 0,         // 0-7 lunar phase
    val moonSign: Int = 0,          // 1-13 (0 = Bad Moon)
    val moonDay: Int = 0,           // day of the KoL calendar month (1-37)

    // ── Intrinsic effects ────────────────────────────────────────────────────
    // Permanent / always-on effects from permed skills or path bonuses.
    val intrinsics: List<String> = emptyList(),

    // ── Equipment (slot → item name; missing = unequipped) ───────────────────
    val equipment: Map<EquipmentSlot, String> = emptyMap(),

    // ── Session ──────────────────────────────────────────────────────────────
    val isLoggedIn: Boolean = false
) {
    // ── Computed: class identity ──────────────────────────────────────────────
    val characterClassEnum: CharacterClass get() = CharacterClass.fromId(characterClass)
    val className: String get() = characterClassEnum.displayName
    val mainStat: MainStat get() = characterClassEnum.mainStat

    // ── Computed: main combat stat (buffed) ───────────────────────────────────
    val buffedMainStat: Int get() = when (mainStat) {
        MainStat.MUSCLE      -> buffedMusc
        MainStat.MYSTICALITY -> buffedMyst
        MainStat.MOXIE       -> buffedMoxie
    }

    // ── Computed: consumable room ─────────────────────────────────────────────
    val fullnessRemaining: Int   get() = fullnessLimit - fullness
    val inebrietyRemaining: Int  get() = inebrietyLimit - inebriety
    val spleenRemaining: Int     get() = spleenLimit - spleenUsed

    // ── Computed: restriction flags ───────────────────────────────────────────
    val isInRonin: Boolean       get() = roninLeft > 0
    val isRestricted: Boolean    get() = isHardcore || isInRonin
    val isInLimitMode: Boolean   get() = limitMode.isNotBlank()

    // ── Computed: HP / MP ratios (0.0–1.0) ───────────────────────────────────
    val hpRatio: Float   get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val mpRatio: Float   get() = if (maxMp > 0) currentMp.toFloat() / maxMp else 0f
    val isLowHp: Boolean get() = hpRatio < 0.2f
    val isLowMp: Boolean get() = mpRatio < 0.2f

    // ── Computed: familiar ────────────────────────────────────────────────────
    val hasFamiliar: Boolean get() = familiarId > 0

    // ── Convenience: equipment accessors ─────────────────────────────────────
    fun equippedItem(slot: EquipmentSlot): String? = equipment[slot]?.takeIf { it.isNotBlank() }
    fun isEquipped(slot: EquipmentSlot): Boolean  = equippedItem(slot) != null
    fun equippedItems(): List<Pair<EquipmentSlot, String>> =
        equipment.entries.filter { it.value.isNotBlank() }.map { it.toPair() }
}
