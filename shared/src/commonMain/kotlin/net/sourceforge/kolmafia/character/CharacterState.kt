package net.sourceforge.kolmafia.character

import kotlin.math.max
import kotlin.math.sqrt

data class CharacterState(

    // ── Identity ──────────────────────────────────────────────────────────────
    val name: String = "",
    val playerId: Int = 0,
    val level: Int = 1,
    val characterClass: Int = 0,
    val zodiacSign: String = "",
    val challengePath: String = "",        // raw path string from API
    val ascensionNumber: Int = 0,
    val gender: Gender = Gender.UNKNOWN,
    val title: String = "",

    // ── HP / MP ──────────────────────────────────────────────────────────────
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val baseMaxHp: Int = 0,                // HP cap before equipment/effect buffs
    val currentMp: Int = 0,
    val maxMp: Int = 0,
    val baseMaxMp: Int = 0,                // MP cap before equipment/effect buffs

    // ── Stats — base, subpoints, buffed ──────────────────────────────────────
    val baseMusc: Int = 0,
    val muscSubpoints: Long = 0L,          // total subpoints (base² + since-last-gain)
    val baseMyst: Int = 0,
    val mystSubpoints: Long = 0L,
    val baseMoxie: Int = 0,
    val moxieSubpoints: Long = 0L,
    // Buffed = base + equipment + effects
    val buffedMusc: Int = 0,
    val buffedMyst: Int = 0,
    val buffedMoxie: Int = 0,

    // ── Currency ──────────────────────────────────────────────────────────────
    val meat: Int = 0,
    val storageMeat: Long = 0L,            // meat in Hagnk's storage
    val closetMeat: Long = 0L,             // meat in closet
    val sessionMeat: Long = 0L,            // net meat gained this session (runtime-only)

    // ── Turn / day counters ───────────────────────────────────────────────────
    val adventuresLeft: Int = 0,
    val turnsPlayed: Int = 0,              // all-time turns across all ascensions
    val currentRun: Int = 0,              // turns played *this ascension*
    val dayCount: Int = 0,                 // days into this ascension
    val globalDaycount: Int = 0,           // global KoL calendar day
    val rolloverTimestamp: Long = 0L,      // Unix epoch seconds of next rollover

    // ── Consumable tracking ───────────────────────────────────────────────────
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    val fullnessLimit: Int = 15,
    val inebrietyLimit: Int = 14,
    val spleenLimit: Int = 15,

    // ── PvP ──────────────────────────────────────────────────────────────────
    val pvpFightsLeft: Int = 0,
    val hippyStoneBroken: Boolean = false, // enables PvP

    // ── Ascension / run mode ─────────────────────────────────────────────────
    val roninLeft: Int = 0,
    val isHardcore: Boolean = false,
    val kingLiberated: Boolean = false,    // freed King Ralph
    val limitMode: String = "",            // "avatar", "spelunk", etc.

    // ── Class-specific combat resources ─────────────────────────────────────
    // Only non-zero for the relevant class/path; safe to ignore for others.
    val fury: Int = 0,                     // Seal Clubber (0–5)
    val soulsauce: Int = 0,                // Sauceror
    val discoMomentum: Int = 0,            // Disco Bandit
    val audience: Int = 0,                 // Accordion Thief (±30 or ±50)
    val absorbs: Int = 0,                  // Grey Goo path (0 to level+2)
    // Plumber path
    val thunder: Int = 0,                  // 0–100
    val rain: Int = 0,                     // 0–100
    val lightning: Int = 0,                // 0–100
    val currentPP: Int = 0,               // Plumber power points
    val maximumPP: Int = 0,
    // You, Robot path
    val youRobotEnergy: Int = 0,
    val youRobotScraps: Int = 0,
    // Wildfire path
    val wildfireWater: Int = 0,
    // Avatar of Boris
    val minstrelLevel: Int = 0,

    // ── Campground ────────────────────────────────────────────────────────────
    val telescopeUpgrades: Int = 0,        // 0–7; unlocks monster info per upgrade
    val hasBookshelf: Boolean = false,

    // ── Social / access flags ─────────────────────────────────────────────────
    val hasStore: Boolean = false,
    val hasDisplayCase: Boolean = false,
    val hasClan: Boolean = false,

    // ── Misc ──────────────────────────────────────────────────────────────────
    val mindControlLevel: Int = 0,         // adjusts monster level/difficulty
    val radSickness: Int = 0,              // Nuclear Autumn path
    val stillsAvailable: Int = -1,         // -1 = unknown; cocktailcrafting stills
    val arenaWins: Int = 0,

    // ── Current familiar ──────────────────────────────────────────────────────
    val familiarId: Int = 0,
    val familiarName: String = "",
    val familiarWeight: Int = 0,
    val familiarExp: Int = 0,
    val enthronedFamiliarId: Int = 0,      // Crown of Thrones familiar
    val enthronedFamiliarName: String = "",
    val bjornedFamiliarId: Int = 0,        // Buddy Bjorn familiar
    val bjornedFamiliarName: String = "",

    // ── Moon / KoL calendar ──────────────────────────────────────────────────
    val moonPhase: Int = 0,
    val moonSign: Int = 0,
    val moonDay: Int = 0,

    // ── Intrinsic effects ────────────────────────────────────────────────────
    val intrinsics: List<String> = emptyList(),

    // ── Equipment ────────────────────────────────────────────────────────────
    val equipment: Map<EquipmentSlot, String> = emptyMap(),

    // ── Session ──────────────────────────────────────────────────────────────
    val isLoggedIn: Boolean = false

) {
    // ── Computed: class identity ──────────────────────────────────────────────
    val characterClassEnum: CharacterClass get() = CharacterClass.fromId(characterClass)
    val className: String                  get() = characterClassEnum.displayName
    val mainStat: MainStat                 get() = characterClassEnum.mainStat

    // ── Computed: ascension path ──────────────────────────────────────────────
    val ascensionPath: AscensionPath get() = AscensionPath.fromApiString(challengePath)

    // ── Computed: challenge-path run mode ─────────────────────────────────────
    // Mirrors KoLCharacter.inFistcore() / inAxecore() from the desktop.
    val isFistcore: Boolean
        get() = !kingLiberated && ascensionPath == AscensionPath.SURPRISING_FIST
    val isAxecore: Boolean
        get() = ascensionPath == AscensionPath.AVATAR_OF_BORIS

    // ── Computed: main buffed stat ────────────────────────────────────────────
    val buffedMainStat: Int get() = when (mainStat) {
        MainStat.MUSCLE      -> buffedMusc
        MainStat.MYSTICALITY -> buffedMyst
        MainStat.MOXIE       -> buffedMoxie
    }

    // ── Computed: stat progression (till next point) ──────────────────────────
    val muscleTNP: Int     get() = tillNextPoint(muscSubpoints)
    val mystTNP: Int       get() = tillNextPoint(mystSubpoints)
    val moxieTNP: Int      get() = tillNextPoint(moxieSubpoints)
    val mainStatTNP: Int   get() = when (mainStat) {
        MainStat.MUSCLE      -> muscleTNP
        MainStat.MYSTICALITY -> mystTNP
        MainStat.MOXIE       -> moxieTNP
    }

    // ── Computed: consumable room & eligibility ───────────────────────────────
    val fullnessRemaining: Int   get() = fullnessLimit - fullness
    val inebrietyRemaining: Int  get() = inebrietyLimit - inebriety
    val spleenRemaining: Int     get() = spleenLimit - spleenUsed
    val isFallingDown: Boolean   get() = inebriety > inebrietyLimit
    val isOverdrunk: Boolean     get() = isFallingDown
    // Path-aware consumption checks (simplified; some paths handled client-side)
    val canEat: Boolean   get() = ascensionPath.canEat   && fullnessRemaining > 0
    val canDrink: Boolean get() = ascensionPath.canDrink && inebrietyRemaining > 0
    val canChew: Boolean  get() = ascensionPath.canChew  && spleenRemaining > 0

    // ── Computed: rollover / time ─────────────────────────────────────────────
    val secondsUntilRollover: Long
        get() = if (rolloverTimestamp > 0) rolloverTimestamp - System.currentTimeMillis() / 1000 else -1L
    val isNearRollover: Boolean get() = secondsUntilRollover in 0..300

    // ── Computed: HP / MP ratios ──────────────────────────────────────────────
    val hpRatio: Float   get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val mpRatio: Float   get() = if (maxMp > 0) currentMp.toFloat() / maxMp else 0f
    val isLowHp: Boolean get() = hpRatio < 0.2f
    val isLowMp: Boolean get() = mpRatio < 0.2f

    // ── Computed: restriction / mode flags ────────────────────────────────────
    val isInRonin: Boolean     get() = roninLeft > 0
    val isRestricted: Boolean  get() = isHardcore || isInRonin
    val isInLimitMode: Boolean get() = limitMode.isNotBlank()
    val isUnderStandard: Boolean get() = ascensionPath == AscensionPath.STANDARD

    // ── Computed: familiar ────────────────────────────────────────────────────
    val hasFamiliar: Boolean      get() = familiarId > 0
    val hasEnthroned: Boolean     get() = enthronedFamiliarId > 0
    val hasBjorned: Boolean       get() = bjornedFamiliarId > 0

    // ── Computed: fury / class resource limits ────────────────────────────────
    val furyLimit: Int get() = when (characterClassEnum) {
        CharacterClass.SEAL_CLUBBER -> 5
        else                        -> 0
    }
    val audienceLimit: Int get() = 30   // 50 with Pete jacket; simplified here
    // AT song slot limit. Base = 3 for AT class; 0 for all other classes.
    val atSongLimit: Int get() = if (characterClassEnum == CharacterClass.ACCORDION_THIEF) 3 else 0

    // ── Computed: campground access ───────────────────────────────────────────
    val canUseStore: Boolean        get() = hasStore
    val canUseDisplayCase: Boolean  get() = hasDisplayCase

    // ── Convenience: equipment ────────────────────────────────────────────────
    fun equippedItem(slot: EquipmentSlot): String? = equipment[slot]?.takeIf { it.isNotBlank() }
    fun isEquipped(slot: EquipmentSlot): Boolean   = equippedItem(slot) != null
    fun equippedItems(): List<Pair<EquipmentSlot, String>> =
        equipment.entries.filter { it.value.isNotBlank() }.map { it.toPair() }
    val isUnarmed: Boolean get() = !isEquipped(EquipmentSlot.WEAPON) && !isEquipped(EquipmentSlot.OFFHAND)

    // ── Private helpers ───────────────────────────────────────────────────────
    private fun tillNextPoint(subpoints: Long): Int {
        if (subpoints < 0L) return 0
        val base = sqrt(subpoints.toDouble()).toLong()
        val nextThreshold = (base + 1) * (base + 1)
        return max(0, (nextThreshold - subpoints).toInt())
    }
}
