package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.inventory.ItemRestriction
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses concoctions.txt from the bundled compose resources.
// Format (tab-separated): result  method  ingredient1  [ingredient2  ...]
// result and ingredients may carry a quantity suffix like "item name (3)".
// method is a comma-separated list of creation method strings.
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ConcoctionDatabase {

    private val _byResult = mutableMapOf<String, ConcoctionData>()
    private val _byIngredient = mutableMapOf<String, MutableList<ConcoctionData>>()
    private var loaded = false
    private var pullsRemaining: Int = -1
    private var pullsBudgeted: Int = 0
    private var refreshNeeded = false
    private var refreshLevel = 0
    private var recalculateAdventureRange = false
    private val runtimeByResult = mutableMapOf<String, ConcoctionRuntimeState>()
    private var lastRefreshContext: ConcoctionRefreshContext = ConcoctionRefreshContext.EMPTY
    private var speakeasyConcoctionsRegistered = false
    private var hotDogConcoctionsRegistered = false

    val byResult: Map<String, ConcoctionData> get() = _byResult
    val byIngredient: Map<String, List<ConcoctionData>> get() = _byIngredient

    fun getPullsRemaining(): Int = pullsRemaining

    fun getPullsBudgeted(): Int = pullsBudgeted

    fun getQueuedFullness(): Int = ConcoctionQueueBudget.queuedFullness

    fun getQueuedInebriety(): Int = ConcoctionQueueBudget.queuedInebriety

    fun getQueuedSpleenHit(): Int = ConcoctionQueueBudget.queuedSpleenHit

    fun setPullsRemaining(pulls: Int) {
        pullsRemaining = pulls
        if (pullsRemaining >= 0 && pullsBudgeted == 0) {
            setPullsBudgeted(pullsRemaining)
        } else if (pullsRemaining >= 0 && pullsRemaining < pullsBudgeted) {
            setPullsBudgeted(pullsRemaining)
        }
    }

    fun setPullsBudgeted(value: Int) {
        pullsBudgeted = value.coerceAtLeast(ConcoctionQueueBudget.pullsUsed)
        if (pullsRemaining >= 0) {
            pullsBudgeted = pullsBudgeted.coerceAtMost(pullsRemaining)
        }
    }

    fun pullableCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.pullable ?: 0

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/concoctions.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getByResult(name: String): ConcoctionData? = _byResult[name.lowercase()]
    fun getByIngredient(name: String): List<ConcoctionData> =
        _byIngredient[name.lowercase()] ?: emptyList()
    fun all(): Collection<ConcoctionData> = _byResult.values

    fun getEffectName(resultName: String): String? = getByResult(resultName)?.effectName

    fun getRuntime(resultName: String): ConcoctionRuntimeState? =
        runtimeByResult[resultName.lowercase()]

    fun initialCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.initial ?: 0

    fun creatableCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.creatable ?: 0

    fun totalCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.total ?: 0

    /** Desktop Concoction.getAvailable — post-refresh visible quantity (includes pullable). */
    fun availableCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.visibleTotal ?: 0

    fun freeTotalCount(resultName: String): Int =
        runtimeByResult[resultName.lowercase()]?.freeTotal ?: 0

    /** Desktop ConcoctionDatabase.getCreatables — post-refresh creatable snapshot list. */
    fun getCreatables(): List<ConcoctionCreatableEntry> = ConcoctionCreatableRegistry.entries()

    /** Desktop Concoction.setEffectName — derive from item EFFECT modifier after TCRS apply. */
    fun setEffectName(itemId: Int, itemName: String) {
        val concoction = getByResult(itemName) ?: return
        val effectName = ModifierDatabase.getStringModifier(itemName, StringModifier.EFFECT)
            .ifBlank { null }
        _byResult[concoction.result.lowercase()] = concoction.copy(effectName = effectName)
    }

    /** Re-derive effect names from current (bundled) item modifiers after TCRS reset. */
    fun resetEffectNames() {
        for ((key, concoction) in _byResult.toList()) {
            val effectName = ModifierDatabase.getStringModifier(concoction.result, StringModifier.EFFECT)
                .ifBlank { null }
            _byResult[key] = concoction.copy(effectName = effectName)
        }
    }

    /** Desktop ConcoctionDatabase.markRecalculateAdventureRange — variable consumables changed adv yields. */
    fun markRecalculateAdventureRange() {
        recalculateAdventureRange = true
    }

    /** Desktop ConcoctionDatabase.refreshConcoctions — effect names + initial counts; full creatable cache deferred. */
    fun refreshConcoctions(
        force: Boolean = true,
        context: ConcoctionRefreshContext = ConcoctionRefreshContext.EMPTY,
    ) {
        if (force) {
            refreshNeeded = true
        }
        if (!refreshNeeded) {
            return
        }
        if (refreshLevel > 0) {
            return
        }
        refreshConcoctionsNow(context)
    }

    /** Live login hook — refresh from aggregated counts and rebuild average-adventure cache. */
    fun refreshConcoctionsFromAggregated(counts: Map<Int, Int>) {
        refreshConcoctionsFromAggregated(ConcoctionRefreshContext.fromAggregatedCounts(counts))
    }

    /** Live login hook — full refresh context (counts + cachePermitted v1). */
    fun refreshConcoctionsFromAggregated(context: ConcoctionRefreshContext) {
        refreshConcoctionsNow(context)
        ConsumableDatabase.calculateAllAverageAdventures()
    }

    /** Backward-compatible alias for inventory-only refresh. */
    fun refreshConcoctionsFromInventory(counts: Map<Int, Int>) =
        refreshConcoctionsFromAggregated(counts)

    /** Desktop ConcoctionDatabase.refreshConcoctionsNow — initial counts + adventures context + cache rebuild. */
    fun refreshConcoctionsNow(context: ConcoctionRefreshContext = ConcoctionRefreshContext.EMPTY) {
        lastRefreshContext = context
        refreshNeeded = false
        rebuildRuntimeState(context)
        resetEffectNames()
        ConsumableDatabase.setAdventuresNeededContextForLive(
            ConcoctionAdventuresContext(
                initialCount = { name -> initialCount(name) },
            ),
        )
        if (recalculateAdventureRange) {
            ConsumableDatabase.calculateAllAverageAdventures()
            recalculateAdventureRange = false
        }
    }

    private fun rebuildRuntimeState(context: ConcoctionRefreshContext) {
        registerLoungeSpeakeasyConcoctions()
        registerLoungeHotDogConcoctions()
        val preservedQueued = runtimeByResult.mapValues { (_, state) ->
            state.queued to state.queuedPulls
        }
        val preservedWasPossible = runtimeByResult.mapValues { (_, state) -> state.wasPossible }
        runtimeByResult.clear()
        for (concoction in _byResult.values) {
            val initial = context.itemCount(concoction.result)
            val key = concoction.result.lowercase()
            val preserved = preservedQueued[key]
            if (isSpeakeasyConcoction(concoction)) {
                val cost = SpeakeasyDatabase.nameToCost(concoction.result).coerceAtLeast(0)
                runtimeByResult[key] = ConcoctionRuntimeState(
                    initial = 0,
                    creatable = 0,
                    total = 0,
                    visibleTotal = 0,
                    price = cost,
                    skipCalculate = true,
                    queued = preserved?.first ?: 0,
                    queuedPulls = preserved?.second ?: 0,
                    wasPossible = preservedWasPossible[key] ?: false,
                )
                continue
            }
            if (isHotDogConcoction(concoction)) {
                runtimeByResult[key] = ConcoctionRuntimeState(
                    initial = 0,
                    creatable = 0,
                    total = 0,
                    visibleTotal = 0,
                    price = 0,
                    skipCalculate = true,
                    queued = preserved?.first ?: 0,
                    queuedPulls = preserved?.second ?: 0,
                    wasPossible = preservedWasPossible[key] ?: false,
                )
                continue
            }
            if (isCoinmasterPurchaseOnly(concoction)) {
                val itemId = ItemDatabase.getByName(concoction.result)?.id
                val acquirable = if (itemId != null && context.canUseCoinmasters) {
                    context.coinmasterAcquirable(itemId)
                } else {
                    0
                }
                runtimeByResult[key] = ConcoctionRuntimeState(
                    initial = initial,
                    creatable = acquirable,
                    total = initial + acquirable,
                    visibleTotal = initial + acquirable,
                    price = 0,
                    skipCalculate = true,
                    queued = preserved?.first ?: 0,
                    queuedPulls = preserved?.second ?: 0,
                    wasPossible = preservedWasPossible[key] ?: false,
                )
            } else {
                val price = resolveRefreshPrice(concoction, context)
                runtimeByResult[key] = ConcoctionRuntimeState(
                    initial = initial,
                    creatable = 0,
                    total = initial,
                    visibleTotal = initial,
                    price = price,
                    queued = preserved?.first ?: 0,
                    queuedPulls = preserved?.second ?: 0,
                    wasPossible = preservedWasPossible[key] ?: false,
                )
            }
        }

        val creatableContext = ConcoctionCreatableContext(
            initialCount = { name -> initialCount(name) },
            isPermitted = context.isPermitted,
            limitPools = context.resolvedLimitPools(),
            inGLover = context.inGLover,
            priceFor = { concoction -> runtimeByResult[concoction.result.lowercase()]?.price ?: 0 },
            knollAvailable = context.knollAvailable,
            inZombiecore = context.inZombiecore,
            coinmasterAcquirable = context.coinmasterAcquirable,
            availableCountById = context.availableCountById,
            ingredientPriceFor = ConcoctionInterchangeableIngredients::defaultPriceFor,
        )
        for (key in _byResult.keys.sorted()) {
            val concoction = _byResult[key] ?: continue
            val runtime = runtimeByResult[key] ?: continue
            if (runtime.skipCalculate) continue
            val total = calculateCreatableTotal(concoction, creatableContext)
            val creatable = adjustCreatableForMeatPrice(total, runtime.initial, concoction, creatableContext)
            val freeTotal = calculateCreatableFreeTotal(concoction, creatableContext)
            val preserved = preservedQueued[key]
            runtimeByResult[key] = runtime.copy(
                creatable = creatable,
                total = total,
                visibleTotal = total,
                freeTotal = freeTotal,
                queued = preserved?.first ?: runtime.queued,
                queuedPulls = preserved?.second ?: runtime.queuedPulls,
                wasPossible = preservedWasPossible[key] ?: runtime.wasPossible,
            )
        }
        applyPullablePass(context)
        applySpeakeasyRefreshTail(context, preservedQueued, preservedWasPossible)
        applyHotDogRefreshTail(context, preservedQueued, preservedWasPossible)
        ConcoctionCreatableRegistry.updateFromRefresh()
    }

    /** Desktop Concoction.resetCalculations speakeasy branch — meat/daily-limit initial counts. */
    private fun applySpeakeasyRefreshTail(
        context: ConcoctionRefreshContext,
        preservedQueued: Map<String, Pair<Int, Int>>,
        preservedWasPossible: Map<String, Boolean>,
    ) {
        val meat = context.characterState?.meat ?: 0
        val drunk = context.preferences?.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF)
            ?: DefaultsDatabase.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF)
        val drinkableNumber = (3 - drunk).coerceAtLeast(0)

        for (entry in SpeakeasyDatabase.entries) {
            val key = entry.name.lowercase()
            val preserved = preservedQueued[key]
            val cost = entry.cost
            val initial = if (!SpeakeasyAvailability.isAvailable(entry.name) || cost <= 0) {
                0
            } else {
                minOf(meat / cost, drinkableNumber).coerceAtLeast(0)
            }
            runtimeByResult[key] = ConcoctionRuntimeState(
                initial = initial,
                creatable = initial,
                total = initial,
                visibleTotal = initial,
                freeTotal = initial,
                price = cost,
                skipCalculate = true,
                queued = preserved?.first ?: 0,
                queuedPulls = preserved?.second ?: 0,
                wasPossible = preservedWasPossible[key] ?: false,
            )
        }
    }

    /** Desktop Concoction.resetCalculations hot dog special branch — availability + fancy daily limit. */
    private fun applyHotDogRefreshTail(
        context: ConcoctionRefreshContext,
        preservedQueued: Map<String, Pair<Int, Int>>,
        preservedWasPossible: Map<String, Boolean>,
    ) {
        val fancyEaten = context.preferences?.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false)
            ?: DefaultsDatabase.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF)

        for (entry in HotDogDatabase.entries) {
            val key = entry.name.lowercase()
            val preserved = preservedQueued[key]
            val available = HotDogAvailability.isAvailable(entry.name) &&
                !(HotDogDatabase.isFancyHotDog(entry.name) && fancyEaten)
            val initial = if (available) 1 else 0
            runtimeByResult[key] = ConcoctionRuntimeState(
                initial = initial,
                creatable = 0,
                total = initial,
                visibleTotal = initial,
                freeTotal = initial,
                price = 0,
                skipCalculate = true,
                queued = preserved?.first ?: 0,
                queuedPulls = preserved?.second ?: 0,
                wasPossible = preservedWasPossible[key] ?: false,
            )
        }
    }

    /** Idempotent virtual HOT_DOG concoctions for clan lounge dogs not in concoctions.txt. */
    private fun registerLoungeHotDogConcoctions() {
        if (hotDogConcoctionsRegistered) return
        for (entry in HotDogDatabase.entries) {
            val key = entry.name.lowercase()
            if (key in _byResult) continue
            _byResult[key] = ConcoctionData(
                result = entry.name,
                resultQuantity = 1,
                methods = setOf("HOT_DOG"),
                ingredients = emptyList(),
            )
        }
        hotDogConcoctionsRegistered = true
    }

    private fun isHotDogConcoction(concoction: ConcoctionData): Boolean =
        "HOT_DOG" in concoction.methods

    /** Idempotent virtual SPEAKEASY concoctions for clan lounge drinks not in concoctions.txt. */
    private fun registerLoungeSpeakeasyConcoctions() {
        if (speakeasyConcoctionsRegistered) return
        for (entry in SpeakeasyDatabase.entries) {
            val key = entry.name.lowercase()
            if (key in _byResult) continue
            _byResult[key] = ConcoctionData(
                result = entry.name,
                resultQuantity = 1,
                methods = setOf("SPEAKEASY"),
                ingredients = emptyList(),
            )
        }
        speakeasyConcoctionsRegistered = true
    }

    private fun isSpeakeasyConcoction(concoction: ConcoctionData): Boolean =
        "SPEAKEASY" in concoction.methods

    /** Desktop refresh calculate2 tail — storage pull budgeting for ronin concoctions. */
    private fun applyPullablePass(context: ConcoctionRefreshContext) {
        if (!context.considerPulls) return
        val pullsAvailable = getPullsBudgeted() - ConcoctionQueueBudget.pullsUsed
        if (pullsAvailable <= 0) return

        for (key in _byResult.keys.sorted()) {
            val concoction = _byResult[key] ?: continue
            val runtime = runtimeByResult[key] ?: continue
            if (runtime.skipCalculate || runtime.price > 0) continue

            val itemId = ItemDatabase.getByName(concoction.result)?.id ?: continue
            if (itemId <= 0) continue
            if (!meetsLevelRequirement(concoction.result, context.characterLevel)) continue

            val state = context.characterState
            if (state != null &&
                !ItemRestriction.isAllowed(itemId, concoction.result, state, gameDatabase = null)
            ) {
                continue
            }

            val storageCount = context.storageCountById(itemId)
            val pullable = minOf(
                storageCount - runtime.queuedPulls,
                pullsAvailable,
            ).coerceAtLeast(0)
            if (pullable <= 0) continue

            val newTotal = runtime.total + pullable
            runtimeByResult[key] = runtime.copy(
                pullable = pullable,
                total = newTotal,
                visibleTotal = newTotal,
            )
        }
    }

    private fun meetsLevelRequirement(resultName: String, characterLevel: Int): Boolean {
        val levelReq = ConsumableDatabase.getLevelReqByName(resultName) ?: return true
        return characterLevel >= levelReq
    }

    /** Re-run refresh using the last [ConcoctionRefreshContext] (desktop post-queue-drain refresh). */
    fun refreshConcoctionsNowFromLastContext() {
        refreshConcoctionsNow(lastRefreshContext)
    }

    /** Re-run calculate2 after craft queue push/pop using the last refresh context. */
    internal fun refreshAfterQueueMutation() {
        val context = lastRefreshContext.withQueuedCredits(ConcoctionQueuedIngredients.creditForRefresh())
        if (context != ConcoctionRefreshContext.EMPTY || runtimeByResult.isNotEmpty()) {
            rebuildRuntimeState(context)
        }
    }

    /** Re-run refresh after clan lounge speakeasy/hot-dog availability or daily-limit mutation. */
    internal fun refreshAfterLoungeMutation(preferences: Preferences? = null) {
        val context = preferences?.let { lastRefreshContext.copy(preferences = it) } ?: lastRefreshContext
        if (context != ConcoctionRefreshContext.EMPTY || runtimeByResult.isNotEmpty()) {
            rebuildRuntimeState(context)
        } else {
            refreshConcoctionsNow(context)
        }
    }

    internal fun setRuntimeForTest(key: String, state: ConcoctionRuntimeState) {
        runtimeByResult[key.lowercase()] = state
    }

    internal fun updateRuntimeWasPossible(resultName: String, wasPossible: Boolean) {
        val key = resultName.lowercase()
        val current = runtimeByResult[key] ?: return
        runtimeByResult[key] = current.copy(wasPossible = wasPossible)
    }

    /** Desktop purchaseRequest != null — COINMASTER-primary concoctions only. */
    private fun isCoinmasterPurchaseOnly(concoction: ConcoctionData): Boolean {
        val method = ConcoctionCreationCost.primaryMethod(concoction.methods) ?: return false
        return method == "COINMASTER"
    }

    /** Desktop refresh pass 1 — NPC price + calculateBasicItems buyables. */
    private fun resolveRefreshPrice(concoction: ConcoctionData, context: ConcoctionRefreshContext): Int {
        val itemId = ItemDatabase.getByName(concoction.result)?.id
        if (itemId != null) {
            ConcoctionBuyables.buyablePrice(itemId)?.let { return it }
        }
        if (!context.canUseNpcStores) return 0
        if (itemId == ConcoctionBuyables.FLAT_DOUGH) return 0
        val price = context.npcPrice(concoction.result)
        if (price <= 0) return 0
        if (itemId != null && !NpcStoreDatabase.containsItem(itemId, validate = false)) return 0
        return price
    }

    /** Test hook — inject a concoction without loading from disk. */
    internal fun injectForTest(concoction: ConcoctionData) {
        _byResult[concoction.result.lowercase()] = concoction
        for (ingredient in concoction.ingredients) {
            _byIngredient
                .getOrPut(ingredient.name.lowercase()) { mutableListOf() }
                .add(concoction)
        }
        loaded = true
    }

    /** Test hook — reset singleton state. */
    internal fun resetForTest() {
        _byResult.clear()
        _byIngredient.clear()
        runtimeByResult.clear()
        loaded = false
        pullsRemaining = -1
        pullsBudgeted = 0
        ConcoctionQueueBudget.resetForTest()
        ConcoctionCraftQueue.resetForTest()
        ConcoctionCreatableRegistry.resetForTest()
        lastRefreshContext = ConcoctionRefreshContext.EMPTY
        speakeasyConcoctionsRegistered = false
        hotDogConcoctionsRegistered = false
        resetRefreshStateForTest()
    }

    internal fun resetRefreshStateForTest() {
        refreshNeeded = false
        refreshLevel = 0
        recalculateAdventureRange = false
        runtimeByResult.clear()
    }

    internal fun recalculateAdventureRangeForTest(): Boolean = recalculateAdventureRange
    fun cooking(): List<ConcoctionData> = _byResult.values.filter { it.isCooking }
    fun mixing(): List<ConcoctionData> = _byResult.values.filter { it.isMixing }
    fun smithing(): List<ConcoctionData> = _byResult.values.filter { it.isSmithing }

    // Parses a field that may carry an optional quantity suffix: "item name (3)" → Pair("item name", 3)
    // Fields without a parenthesised integer suffix have quantity 1.
    private fun parseNameWithQuantity(raw: String): Pair<String, Int> {
        val parenIdx = raw.lastIndexOf('(')
        if (parenIdx >= 0) {
            val inside = raw.substring(parenIdx + 1).trimEnd(')')
            val qty = inside.toIntOrNull()
            if (qty != null) {
                val name = raw.substring(0, parenIdx).trim()
                return Pair(name, qty)
            }
        }
        return Pair(raw.trim(), 1)
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val (resultName, resultQty) = parseNameWithQuantity(parts[0])
            val methods = parts[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            val ingredients = (2 until parts.size).mapNotNull { idx ->
                val raw2 = parts[idx].trim()
                if (raw2.isEmpty()) null
                else {
                    val (ingName, ingQty) = parseNameWithQuantity(raw2)
                    ConcoctionIngredient(ingName, ingQty)
                }
            }

            val concoction = ConcoctionData(
                result = resultName,
                resultQuantity = resultQty,
                methods = methods,
                ingredients = ingredients,
            )

            _byResult[resultName.lowercase()] = concoction
            for (ingredient in ingredients) {
                _byIngredient
                    .getOrPut(ingredient.name.lowercase()) { mutableListOf() }
                    .add(concoction)
            }
        }
    }
}
