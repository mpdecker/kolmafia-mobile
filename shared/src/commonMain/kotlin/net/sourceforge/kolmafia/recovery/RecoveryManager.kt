package net.sourceforge.kolmafia.recovery

import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.RestoreType
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.LightsOutManager
import net.sourceforge.kolmafia.session.VoteMonsterManager
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

/**
 * Callbacks for desktop [RecoveryManager.runBetweenBattleChecks] ordering
 * (Phases 1911–1925). Wired by [net.sourceforge.kolmafia.adventure.AdventureManager].
 */
data class BetweenBattleContext(
    val isScriptCheck: Boolean = true,
    val isMoodCheck: Boolean = true,
    val isHealthCheck: Boolean = true,
    val isManaCheck: Boolean = true,
    val isRecoveryPossible: () -> Boolean = { true },
    val executeBetweenBattleScript: () -> Unit = {},
    val executeMood: suspend () -> Unit = {},
    val recoverHpStep: suspend () -> Boolean = { false },
    val burnExtraMana: suspend () -> Unit = {},
    val recoverMpStep: suspend () -> Boolean = { false },
    val currentHp: () -> Int = { Int.MAX_VALUE },
    val maxHp: () -> Int = { Int.MAX_VALUE },
    val edFightInProgress: () -> Boolean = { false },
    val turnsPlayed: () -> Int = { 0 },
)

sealed class BetweenBattleResult {
    data object Ok : BetweenBattleResult()
    data object Skipped : BetweenBattleResult()
    data object AbortedZeroHp : BetweenBattleResult()
}

class RecoveryManager(
    private val inventoryManager: InventoryManager,
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    @Volatile
    var isRecoveryActive: Boolean = false

    companion object {
        private const val MAX_BETWEEN_BATTLE_HEAL_ITERS = 10
        private const val MAX_CHECKPOINT_ITERATIONS = 25

        fun needsHpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_HP, true)) return false
            if (state.maxHp <= 0) return false
            val targetPct = prefs.getInt(Preferences.HP_RECOVERY_TARGET_PCT, 50)
            val ratioPct = state.currentHp * 100 / state.maxHp
            return ratioPct < targetPct
        }

        fun needsMpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_MP, false)) return false
            if (state.maxMp <= 0) return false
            val targetPct = prefs.getInt(Preferences.MP_RECOVERY_TARGET_PCT, 50)
            val ratioPct = state.currentMp * 100 / state.maxMp
            return ratioPct < targetPct
        }

        fun hpAboveStopThreshold(state: CharacterState, prefs: Preferences): Boolean {
            if (state.maxHp <= 0) return true
            val stopPct = prefs.getInt(Preferences.HP_RECOVERY_STOP_PCT, 90)
            return state.currentHp * 100 / state.maxHp >= stopPct
        }

        fun mpAboveStopThreshold(state: CharacterState, prefs: Preferences): Boolean {
            if (state.maxMp <= 0) return true
            val stopPct = prefs.getInt(Preferences.MP_RECOVERY_STOP_PCT, 90)
            return state.currentMp * 100 / state.maxMp >= stopPct
        }

        internal fun hpRecoveryTarget(amount: Int, state: CharacterState, prefs: Preferences): Int {
            if (state.maxHp <= 0) return 0
            return if (amount > 0) {
                minOf(state.maxHp, amount)
            } else {
                val stopPct = prefs.getInt(Preferences.HP_RECOVERY_STOP_PCT, 90)
                (state.maxHp * stopPct + 99) / 100
            }
        }

        internal fun mpRecoveryTarget(amount: Int, state: CharacterState, prefs: Preferences): Int {
            if (state.maxMp <= 0) return 0
            return if (amount > 0) {
                minOf(state.maxMp, amount)
            } else {
                val stopPct = prefs.getInt(Preferences.MP_RECOVERY_STOP_PCT, 90)
                (state.maxMp * stopPct + 99) / 100
            }
        }

        internal fun isFullRestore(restoreData: RestoreData): Boolean =
            restoreData.hpMaxExpr.contains("[") || restoreData.mpMaxExpr.contains("[")

        internal fun pickHpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.quantity > 0 }
            }

        internal fun pickMpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.quantity > 0 }
            }

        internal fun pickHpSkill(
            restores: List<RestoreData>,
            skills: List<SkillData>,
            currentMp: Int,
        ): SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }

        internal fun pickMpSkill(
            restores: List<RestoreData>,
            skills: List<SkillData>,
            currentMp: Int,
        ): SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }
    }

    /**
     * Desktop [RecoveryManager.runBetweenBattleChecks] — ordered pre-adventure pipeline.
     * Re-entrancy: nested calls while [isRecoveryActive] are skipped.
     */
    suspend fun runBetweenBattleChecks(
        isFullCheck: Boolean = true,
        ctx: BetweenBattleContext = BetweenBattleContext(),
    ): BetweenBattleResult =
        // Desktop overload: (isFull, isFull, true, isFull)
        runBetweenBattleChecks(
            isScriptCheck = isFullCheck,
            isMoodCheck = isFullCheck,
            isHealthCheck = true,
            isManaCheck = isFullCheck,
            ctx = ctx,
        )

    suspend fun runBetweenBattleChecks(
        isScriptCheck: Boolean,
        isMoodCheck: Boolean,
        isHealthCheck: Boolean,
        isManaCheck: Boolean,
        ctx: BetweenBattleContext,
    ): BetweenBattleResult {
        if (isRecoveryActive || !ctx.isRecoveryPossible()) {
            return BetweenBattleResult.Skipped
        }
        if (CharpaneValhallaSync.inValhalla) {
            return BetweenBattleResult.Skipped
        }

        isRecoveryActive = true
        try {
            if (isScriptCheck) {
                ctx.executeBetweenBattleScript()
            }
            if (isMoodCheck) {
                ctx.executeMood()
            }
            if (isHealthCheck) {
                var continueHp = true
                var hpIter = 0
                while (continueHp && hpIter < MAX_BETWEEN_BATTLE_HEAL_ITERS) {
                    continueHp = ctx.recoverHpStep()
                    hpIter++
                }
            }
            if (isMoodCheck) {
                ctx.burnExtraMana()
            }
            if (isManaCheck) {
                var continueMp = true
                var mpIter = 0
                while (continueMp && mpIter < MAX_BETWEEN_BATTLE_HEAL_ITERS) {
                    continueMp = ctx.recoverMpStep()
                    mpIter++
                }
            }

            // Uninitialized character (maxHp==0) is not a death abort
            if (ctx.currentHp() == 0 && ctx.maxHp() > 0 && !ctx.edFightInProgress()) {
                return BetweenBattleResult.AbortedZeroHp
            }

            val turns = ctx.turnsPlayed()
            LightsOutManager.checkCounter(preferences, turns)
            VoteMonsterManager.checkCounter(preferences, turns)
            return BetweenBattleResult.Ok
        } finally {
            isRecoveryActive = false
        }
    }

    suspend fun recoverIfNeeded(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
        force: Boolean = false,
    ): Boolean {
        var recovered = false
        if (preferences.getBoolean(Preferences.AUTO_RECOVER_HP, true) &&
            (force || needsHpRecovery(charState, preferences))) {
            recovered = recoverHp(charState, invState, skillState) || recovered
        }
        if (preferences.getBoolean(Preferences.AUTO_RECOVER_MP, false) &&
            (force || needsMpRecovery(charState, preferences))) {
            recovered = recoverMp(charState, invState, skillState) || recovered
        }
        return recovered
    }

    /** Single HP restore action for between-battle / ASH recover loops. */
    suspend fun recoverHpOnce(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean = recoverHp(charState, invState, skillState)

    /** Single MP restore action for between-battle / ASH recover loops. */
    suspend fun recoverMpOnce(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean = recoverMp(charState, invState, skillState)

    suspend fun recoverHpToMax(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
        targetHp: Int,
    ): Boolean {
        if (charState.currentHp >= targetHp) return false
        return recoverHp(charState, invState, skillState)
    }

    suspend fun checkpointedRecoverHp(
        amount: Int,
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
        refreshStates: suspend () -> Triple<CharacterState, InventoryState, SkillState>,
    ): Boolean {
        if (CharpaneValhallaSync.inValhalla) return false
        isRecoveryActive = true
        var state = charState
        try {
            var inventory = invState
            var skills = skillState
            val target = hpRecoveryTarget(amount, state, preferences)
            if (state.currentHp >= target) return true

            repeat(MAX_CHECKPOINT_ITERATIONS) {
                val before = state.currentHp
                if (!recoverHp(state, inventory, skills)) return false
                val refreshed = refreshStates()
                state = refreshed.first
                inventory = refreshed.second
                skills = refreshed.third
                if (state.currentHp <= before) return false
                if (state.currentHp >= target) return true
            }
            return state.currentHp >= target
        } finally {
            isRecoveryActive = false
            VoteMonsterManager.checkCounter(preferences, state.turnsPlayed)
        }
    }

    suspend fun checkpointedRecoverMp(
        amount: Int,
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
        refreshStates: suspend () -> Triple<CharacterState, InventoryState, SkillState>,
    ): Boolean {
        if (CharpaneValhallaSync.inValhalla) return false
        isRecoveryActive = true
        var state = charState
        try {
            var inventory = invState
            var skills = skillState
            val target = mpRecoveryTarget(amount, state, preferences)
            if (state.currentMp >= target) return true

            repeat(MAX_CHECKPOINT_ITERATIONS) {
                val before = state.currentMp
                if (!recoverMp(state, inventory, skills)) return false
                val refreshed = refreshStates()
                state = refreshed.first
                inventory = refreshed.second
                skills = refreshed.third
                if (state.currentMp <= before) return false
                if (state.currentMp >= target) return true
            }
            return state.currentMp >= target
        } finally {
            isRecoveryActive = false
            VoteMonsterManager.checkCounter(preferences, state.turnsPlayed)
        }
    }

    private suspend fun recoverHp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickHpItem(RestoreDatabase.hpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickHpSkill(RestoreDatabase.hpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }

    private suspend fun recoverMp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickMpItem(RestoreDatabase.mpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickMpSkill(RestoreDatabase.mpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }
}
