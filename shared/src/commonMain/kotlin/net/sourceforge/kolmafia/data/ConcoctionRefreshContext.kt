package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.skill.SkillData

/** Item-count snapshot for ConcoctionDatabase.refreshConcoctionsNow initial counts. */
data class ConcoctionRefreshContext(
    val itemCount: (String) -> Int = { 0 },
    val availableCountById: (Int) -> Int = { 0 },
    val isPermitted: (ConcoctionData) -> Boolean = { true },
    val limitPools: ConcoctionLimitPools? = null,
    val limitPoolsFactory: (() -> ConcoctionLimitPools?)? = null,
    val inGLover: Boolean = false,
    val canUseNpcStores: Boolean = false,
    val npcPrice: (String) -> Int = { NpcStoreDatabase.npcPrice(it) },
    val knollAvailable: Boolean = false,
    val inZombiecore: Boolean = false,
    val canUseCoinmasters: Boolean = false,
    val coinmasterAcquirable: (Int) -> Int = { 0 },
    val storageCountById: (Int) -> Int = { 0 },
    val considerPulls: Boolean = false,
    val characterLevel: Int = 1,
    val characterState: CharacterState? = null,
    val preferences: Preferences? = null,
) {
    fun resolvedLimitPools(): ConcoctionLimitPools? = limitPoolsFactory?.invoke() ?: limitPools

    fun withQueuedCredits(credits: Map<Int, Int>): ConcoctionRefreshContext {
        if (credits.isEmpty()) return this
        return copy(
            itemCount = { name ->
                val itemId = ItemDatabase.getByName(name)?.id
                val base = itemCount(name)
                if (itemId == null) base else (base + (credits[itemId] ?: 0)).coerceAtLeast(0)
            },
            availableCountById = { id ->
                (availableCountById(id) + (credits[id] ?: 0)).coerceAtLeast(0)
            },
        )
    }

    companion object {
        val EMPTY = ConcoctionRefreshContext()

        fun fromAggregatedCounts(counts: Map<Int, Int>): ConcoctionRefreshContext {
            return ConcoctionRefreshContext(
                itemCount = itemCount@{ name ->
                    val itemId = ItemDatabase.getByName(name)?.id ?: return@itemCount 0
                    counts[itemId] ?: 0
                },
                availableCountById = { id -> counts[id] ?: 0 },
            )
        }

        fun fromInventoryCounts(counts: Map<Int, Int>): ConcoctionRefreshContext =
            fromAggregatedCounts(counts)

        fun fromIngredientSources(sources: ConcoctionIngredientSources): ConcoctionRefreshContext =
            fromAggregatedCounts(ConcoctionAvailableIngredients.aggregate(sources)).copy(
                storageCountById = { id -> sources.storage[id] ?: 0 },
            )

        /** Desktop refresh pass 1 — coinmaster affordableCount when gates pass. */
        fun resolveCoinmasterAcquirable(
            itemId: Int,
            state: CharacterState,
            prefs: Preferences?,
            accessibleCount: (Int) -> Int,
            hasSkill: (Int) -> Boolean = { false },
            hasEffect: (Int) -> Boolean = { false },
        ): Int {
            val (master, row) = CoinmasterDatabase.findBuyRowForItem(itemId) ?: return 0
            if (!CoinmasterAccessibility.isAccessible(master, state, prefs, accessibleCount, hasEffect)) {
                return 0
            }
            if (!CoinmasterPurchaseAccessibility.canPurchaseItem(
                    master,
                    itemId,
                    state,
                    prefs,
                    accessibleCount,
                    hasSkill,
                )
            ) {
                return 0
            }
            return CoinmasterPurchaseProbe.affordableCount(row, state, accessibleCount)
        }

        /** Live login refresh — aggregated counts + ConcoctionPermitted + cachePermitted v2 limit pools. */
        fun fromLiveSession(
            aggregatedCounts: Map<Int, Int>,
            state: CharacterState,
            skills: List<SkillData> = emptyList(),
            prefs: Preferences? = null,
            effects: List<EffectData> = emptyList(),
            accessibleCount: (Int) -> Int = { id -> aggregatedCounts[id] ?: 0 },
            familiarUsable: (Int) -> Boolean = { false },
            ownedFamiliar: (String) -> Boolean = { false },
            storageCounts: Map<Int, Int> = emptyMap(),
        ): ConcoctionRefreshContext {
            val hasSkill = { id: Int -> skills.any { it.id == id } }
            val hasEffect = { id: Int -> effects.any { it.id == id } }
            val canUseCoinmasters = prefs?.getBoolean("autoSatisfyWithCoinmasters", false) == true &&
                !LimitModeGates.limitCoinmasters(state.limitMode)
            val considerPulls = state.isInRonin && !state.isHardcore
            if (considerPulls && ConcoctionDatabase.getPullsBudgeted() == 0 &&
                ConcoctionDatabase.getPullsRemaining() >= 0
            ) {
                ConcoctionDatabase.setPullsBudgeted(ConcoctionDatabase.getPullsRemaining())
            }
            return ConcoctionRefreshContext(
                itemCount = itemCount@{ name ->
                    val itemId = ItemDatabase.getByName(name)?.id ?: return@itemCount 0
                    aggregatedCounts[itemId] ?: 0
                },
                availableCountById = accessibleCount,
                isPermitted = { concoction ->
                    ConcoctionPermitted.isPermittedMethod(
                        concoction,
                        state,
                        skills,
                        accessibleCount = accessibleCount,
                        prefs = prefs,
                        familiarUsable = familiarUsable,
                        limitMode = state.limitMode,
                    )
                },
                limitPoolsFactory = {
                    ConcoctionLimitPools.fromLiveSession(
                        state = state,
                        skills = skills,
                        prefs = prefs,
                        effects = effects,
                        itemCount = accessibleCount,
                        ownedFamiliar = ownedFamiliar,
                    )
                },
                inGLover = state.inGLover,
                canUseNpcStores = prefs?.getBoolean("autoSatisfyWithNPCs", false) == true &&
                    !LimitModeGates.limitNPCStores(state.limitMode),
                knollAvailable = state.knollAvailable,
                inZombiecore = state.inZombiecore,
                canUseCoinmasters = canUseCoinmasters,
                coinmasterAcquirable = { itemId ->
                    resolveCoinmasterAcquirable(
                        itemId,
                        state,
                        prefs,
                        accessibleCount,
                        hasSkill,
                        hasEffect,
                    )
                },
                storageCountById = { id -> storageCounts[id] ?: 0 },
                considerPulls = considerPulls,
                characterLevel = state.level,
                characterState = state,
                preferences = prefs,
            )
        }
    }
}
