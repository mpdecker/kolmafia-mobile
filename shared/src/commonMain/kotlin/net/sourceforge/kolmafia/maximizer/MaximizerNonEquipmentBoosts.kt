package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectDefinitionProxy
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.PullableItems
import net.sourceforge.kolmafia.item.RetrieveItemSimulator
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.mood.EffectGainGate
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop Maximizer non-equipment boost pass (Phase 386–388: horsery/boombox/mcd/effects + noobcore absorb). */
object MaximizerNonEquipmentBoosts {

    enum class NonEquipmentBaseline {
        PLAN_OVERLAY,
        LIVE_EQUIPPED,
    }

    const val BOOMBOX_ITEM_ID = 9919
    private const val DECK_OF_EVERY_CARD_ID = 8382
    private const val REPLICA_DECK_ID = 11230
    private const val SKELETON_ID = 5881
    private const val GREAT_PANTS_ID = 4696
    private const val REPLICA_GREAT_PANTS_ID = 11209
    private const val SPACEGATE_BADGE_ID = 9404
    private const val APRILING_BAND_HELMET_ID = 11565
    private const val MAYAM_CALENDAR_ID = 11572
    private val LOATHING_IDOL_MICROPHONE_IDS = listOf(11279, 11278, 11277, 11263)

    data class Context(
        val plan: MaximizerEmitSlot.Plan,
        val charState: CharacterState,
        val activeEffects: List<EffectData>,
        val passiveSkillNames: Set<String> = emptySet(),
        val inventory: MaximizerEmitSlot.InventorySnapshot,
        val inventoryCount: (Int) -> Int,
        val gameDatabase: GameDatabase,
        val preferences: Preferences?,
        val mallPriceManager: net.sourceforge.kolmafia.mall.MallPriceManager?,
        val priceLevel: MaximizerPriceLevel,
        val carryFamiliars: List<String> = emptyList(),
        val thrallBonus: Double = 0.0,
        val skillManager: SkillManager? = null,
        val familiarManager: FamiliarManager? = null,
        val standardRequest: StandardRequest? = null,
        val includeAll: Boolean = false,
        val filters: Set<MaximizerFilterType> = MaximizerFilters.allEnabled(),
        val baseline: NonEquipmentBaseline = NonEquipmentBaseline.PLAN_OVERLAY,
    )

    fun build(ctx: Context): List<MaximizerBoost> {
        val boosts = mutableListOf<MaximizerBoost>()
        if (ctx.charState.inNoobcore) {
            boosts += MaximizerNoobcoreAbsorbBoosts.build(ctx)
        }
        if (MaximizerFilterType.OTHER in ctx.filters) {
            boosts += buildHorseryBoosts(ctx)
            boosts += buildBoomBoxBoosts(ctx)
            boosts += buildMcdBoosts(ctx)
        }
        boosts += buildEffectBoosts(ctx)
        return boosts.sorted()
    }

    private fun buildHorseryBoosts(ctx: Context): List<MaximizerBoost> {
        val horses = ModifierDatabase.byTypeAndName["Horsery"].orEmpty()
        if (horses.isEmpty()) return emptyList()
        if (ctx.standardRequest != null &&
            !StandardRequest.isAllowed(
                RestrictedItemType.ITEMS,
                "Horsery contract",
                ctx.charState,
            )
        ) {
            return emptyList()
        }
        val baseline = postEquipmentScore(ctx)
        val boosts = mutableListOf<MaximizerBoost>()
        for ((horseName, _) in horses) {
            val withHorse = postEquipmentScore(ctx, horseryOverride = horseName)
            val delta = withHorse - baseline
            if (delta <= 0.0) continue
            var cmd = "horsery $horseName"
            var text = cmd
            if (ctx.preferences?.getBoolean("horseryAvailable", false) != true) {
                cmd = ""
                if (!ctx.includeAll) continue
                text = "(get a horsery and ride a $horseName)"
            }
            text = "$text (${formatDelta(delta)})"
            val switchMeat = if (!ctx.preferences?.getString("_horsery", "").isNullOrBlank()) 500L else 0L
            if (switchMeat > 0 && ctx.charState.meat < switchMeat) {
                cmd = ""
            }
            if (ctx.preferences?.getBoolean("verboseMaximizer", false) == true && switchMeat > 0) {
                text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
                    text,
                    MaximizerBoostVerboseSuffix.BracketInfo(meatCost = switchMeat),
                    verboseMaximizer = true,
                )
            }
            boosts += MaximizerBoost(
                cmd = cmd,
                text = text,
                delta = delta,
                isEquipment = false,
            )
        }
        return boosts
    }

    private fun buildBoomBoxBoosts(ctx: Context): List<MaximizerBoost> {
        val songs = ModifierDatabase.byTypeAndName["BoomBox"].orEmpty()
        if (songs.isEmpty()) return emptyList()
        val baseline = postEquipmentScore(ctx)
        val hasBoomBox = ctx.inventoryCount(BOOMBOX_ITEM_ID) > 0
        val usesRemaining = ctx.preferences?.getInt("_boomBoxSongsLeft", 11) ?: 11
        val verbose = ctx.preferences?.getBoolean("verboseMaximizer", false) == true
        val boosts = mutableListOf<MaximizerBoost>()
        for ((songName, _) in songs) {
            val withSong = postEquipmentScore(ctx, boomBoxOverride = songName)
            val delta = withSong - baseline
            if (delta <= 0.0) continue
            var cmd = "boombox ${songName.lowercase()}"
            var text = cmd
            if (!hasBoomBox) {
                cmd = ""
                if (!ctx.includeAll) continue
                text = "(get a SongBoom&trade; BoomBox and play $songName)"
            }
            text = "$text (${formatDelta(delta)})"
            if (usesRemaining < 1) {
                cmd = ""
            }
            if (verbose) {
                text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
                    text,
                    MaximizerBoostVerboseSuffix.BracketInfo(usesRemaining = usesRemaining),
                    verboseMaximizer = true,
                )
            }
            boosts += MaximizerBoost(
                cmd = cmd,
                text = text,
                delta = delta,
                isEquipment = false,
            )
        }
        return boosts
    }

    private fun buildMcdBoosts(ctx: Context): List<MaximizerBoost> {
        val available = MaximizerMcdAvailability.mcdAvailable(ctx.charState, ctx.preferences)
        if (!available && !ctx.includeAll) return emptyList()
        val baseline = postEquipmentScore(ctx)
        val max = MaximizerMcdAvailability.maxLevel(ctx.charState)
        val endpoints = listOf(0, max).distinct()
        val boosts = mutableListOf<MaximizerBoost>()
        for (level in endpoints) {
            val withLevel = postEquipmentScore(ctx, mindControlOverride = level)
            val delta = withLevel - baseline
            if (delta <= 0.0) continue
            var cmd = "mcd $level"
            var text = cmd
            if (!available) {
                cmd = ""
                text = "(ascend into a non-Bad Moon sign and mcd $level)"
            }
            text = "$text (${formatDelta(delta)})"
            boosts += MaximizerBoost(
                cmd = cmd,
                text = text,
                delta = delta,
                isEquipment = false,
            )
        }
        return boosts
    }

    private fun buildEffectBoosts(ctx: Context): List<MaximizerBoost> {
        val prefs = ctx.preferences ?: return emptyList()
        val effectState = EffectState(ctx.activeEffects)
        val boosts = mutableListOf<MaximizerBoost>()
        val baseline = postEquipmentScore(ctx)
        val exprCtx = ExpressionContext.from(ctx.charState, ctx.activeEffects, ctx.passiveSkillNames)

        for (effectDef in EffectDatabase.all()) {
            if (!MaximizerContinuation.permitsContinue()) break
            if (effectDef.id <= 0) continue
            val modifierEntry = ModifierDatabase.getEffect(effectDef.name) ?: continue
            val effectValues = ModifierParser.parse(modifierEntry.modifiers, exprCtx)
            val hasEffect = ctx.activeEffects.any {
                it.id == effectDef.id || it.name.equals(effectDef.name, ignoreCase = true)
            }

            if (!hasEffect) {
                if (EffectGainGate.cannotGainEffect(
                        effectDef.id,
                        ctx.charState,
                        effectState,
                        prefs,
                    )
                ) {
                    continue
                }
                val overlay = ReplaceableEffectMutex.applyEffectGain(
                    ctx.activeEffects,
                    EffectData(
                        id = effectDef.id,
                        name = effectDef.name,
                        duration = 1,
                    ),
                )
                val baselineMods = postEquipmentModifierValues(ctx)
                val overlayMods = postEquipmentModifierValues(ctx, activeEffects = overlay)
                if (MaximizerMutexViolations.introducesNewViolations(baselineMods, overlayMods)) {
                    continue
                }
                val delta = postEquipmentScore(ctx, activeEffects = overlay) - baseline
                val constraint = ctx.plan.spec.evaluator.checkConstraints(effectValues)
                when (constraint) {
                    Evaluator.Constraint.VIOLATES -> continue
                    Evaluator.Constraint.IRRELEVANT -> if (delta <= 0.0) continue
                    Evaluator.Constraint.MEETS -> Unit
                }
                val priority = constraint == Evaluator.Constraint.MEETS
                val sources = effectSourcesForGain(ctx, effectDef)
                if (sources.isEmpty()) {
                    if (!ctx.includeAll) continue
                    boosts += effectBoost(
                        cmd = "",
                        text = "(no known source of ${effectDef.name}) (${formatDelta(delta)})",
                        delta = delta,
                        priority = priority,
                    )
                    continue
                }
                for (source in sources) {
                    if (!MaximizerContinuation.permitsContinue()) break
                    if (source.startsWith("#") && !ctx.includeAll) continue
                    if (!MaximizerFilters.allowsSource(source, ctx.filters)) continue
                    val boost = buildSourceBoost(
                        source,
                        effectDef.id,
                        effectDef.name,
                        delta,
                        ctx,
                        effectDuration = 1,
                        hasEffect = false,
                        priority = priority,
                    ) ?: continue
                    boosts += boost
                }
            } else {
                val overlay = ctx.activeEffects.filterNot {
                    it.id == effectDef.id || it.name.equals(effectDef.name, ignoreCase = true)
                }
                val delta = postEquipmentScore(ctx, activeEffects = overlay) - baseline
                when (ctx.plan.spec.evaluator.checkConstraints(effectValues)) {
                    Evaluator.Constraint.MEETS -> continue
                    Evaluator.Constraint.IRRELEVANT -> if (delta <= 0.0) continue
                    Evaluator.Constraint.VIOLATES -> Unit
                }
                val moodManager = ctx.skillManager?.let { MoodManager(it, prefs) }
                val source = moodManager?.getDefaultAction("gain_effect", effectDef.name)
                    ?: EffectDefinitionProxy.getDefaultAction(effectDef.id).orEmpty()
                if (source.isBlank()) {
                    if (!ctx.includeAll) continue
                    boosts += effectBoost(
                        cmd = "",
                        text = "(find some way to remove ${effectDef.name}) (${formatDelta(delta)})",
                        delta = delta,
                    )
                    continue
                }
                if (source.startsWith("#") && !ctx.includeAll) continue
                if (!MaximizerFilters.allowsSource(source, ctx.filters)) continue
                buildSourceBoost(
                    source,
                    effectDef.id,
                    effectDef.name,
                    delta,
                    ctx,
                    hasEffect = true,
                )?.let { boosts += it }
            }
        }
        return boosts
    }

    private fun effectSourcesForGain(
        ctx: Context,
        effectDef: net.sourceforge.kolmafia.data.EffectData,
    ): List<String> {
        val sources = EffectDefinitionProxy.getAllActions(effectDef.id).toMutableList()
        if (MaximizerFilterType.WISH in ctx.filters &&
            !effectDef.attributes.any { it.equals("nohookah", ignoreCase = true) }
        ) {
            sources += "monkeypaw effect ${effectDef.name}"
            sources += "genie effect ${effectDef.name}"
        }
        return sources
    }

    private fun buildSourceBoost(
        source: String,
        effectId: Int,
        effectName: String,
        delta: Double,
        ctx: Context,
        effectDuration: Int = 0,
        hasEffect: Boolean = false,
        priority: Boolean = false,
    ): MaximizerBoost? {
        val ruleCtx = MaximizerBoostSourceRules.SourceRuleContext(
            base = ctx,
            effectId = effectId,
            effectName = effectName,
            source = source,
            hasEffect = hasEffect,
        )
        val ruleResult = MaximizerBoostSourceRules.apply(ruleCtx)
        if (ruleResult?.skip == true) return null

        var cmd = ruleResult?.cmd ?: source
        var text = ruleResult?.text ?: source
        val itemId = itemIdFromSource(source, ctx)
        if (itemId != null && ctx.inventoryCount(itemId) == 0) {
            val itemName = ctx.gameDatabase.item(itemId)?.name ?: return null
            if (ctx.charState.inBeecore && Beeosity.hasBeeosity(itemName)) {
                return null
            }
            val checkedContext = checkedItemContext(ctx)
            val checked = MaximizerCheckedItemBuilder.build(itemId, itemName, checkedContext)
            val retrieveContext = retrieveContext(ctx)
            val method = RetrieveItemSimulator.simRetrieve(itemId, 1, retrieveContext)
            when {
                checked.creatable > 0 -> {
                    text = "make & $text"
                    cmd = "make \u00B6$itemId;$cmd"
                }
                checked.npcBuyable > 0 -> {
                    text = "buy & $text"
                    cmd = "buy 1 \u00B6$itemId;$cmd"
                }
                checked.pullable > 0 -> {
                    text = "pull & $text"
                    cmd = "pull \u00B6$itemId;$cmd"
                }
                method == "uncloset" -> {
                    text = "uncloset & $text"
                    cmd = "closet take 1 \u00B6$itemId;$cmd"
                }
                method == "pull" -> {
                    text = "pull & $text"
                    cmd = "pull 1 \u00B6$itemId;$cmd"
                }
                checked.initial > 0 || method != "have" -> {
                    text = "acquire & $text"
                }
            }
        }
        val itemName = itemNameFromSource(source, ctx)
        if (itemName != null && !meetsLevelRequirement(itemName, ctx.charState.level)) {
            if (!ctx.includeAll) return null
            text = "level up & $text"
            cmd = ""
        }
        text = "$text ("
        val checkedContext = checkedItemContext(ctx)
        val costCtx = MaximizerBoostCostSuffix.Context(
            gameDatabase = ctx.gameDatabase,
            charState = ctx.charState,
            preferences = ctx.preferences,
            skillMpCost = { name ->
                ctx.skillManager?.state?.value?.skills
                    ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?.mpCost
                    ?: SkillDefinitionProxy.getByIdOrName(name)?.mpCost
                    ?: 0
            },
            physicalAccessible = { id -> checkedContext.physicalAccessible(id) },
            mallPrice = { id -> ctx.mallPriceManager?.getMallPrice(id) ?: 0L },
        )
        val costs = MaximizerBoostCostSuffix.accumulateFromCmd(cmd, costCtx) +
            (ruleResult?.extraCosts ?: MaximizerBoostCostSuffix.BoostCosts())
        if (MaximizerBoostCostSuffix.shouldSkipBoost(costs, ctx.preferences)) return null
        text = MaximizerBoostCostSuffix.appendToText(text, costs)
        cmd = MaximizerBoostCostSuffix.applyCapacityGreyout(cmd, costs, ctx.charState)
        text += "${formatDelta(delta)})"
        val itemIdForUses = itemIdFromSource(source, ctx)
        val checked = itemIdForUses?.let { id ->
            ctx.gameDatabase.item(id)?.name?.let { name ->
                MaximizerCheckedItemBuilder.build(id, name, checkedItemContext(ctx))
            }
        }
        val verboseDuration = ruleResult?.duration ?: effectDuration
        val verboseUses = ruleResult?.usesRemaining
            ?: itemIdForUses?.let { itemUsesRemaining(it, ctx) }
            ?: Int.MAX_VALUE
        val verboseItemsRemaining = ruleResult?.itemsRemaining
            ?: itemIdForUses?.let { ctx.inventoryCount(it) }
            ?: 0
        val verboseCreatable = ruleResult?.itemsCreatable ?: checked?.creatable ?: 0
        text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            text,
            MaximizerBoostVerboseSuffix.BracketInfo(
                duration = verboseDuration,
                usesRemaining = verboseUses,
                itemsRemaining = verboseItemsRemaining,
                itemsCreatable = verboseCreatable,
            ),
            verboseMaximizer = ctx.preferences?.getBoolean("verboseMaximizer", false) == true,
        )
        return effectBoost(cmd = cmd, text = text, delta = delta, priority = priority)
    }

    private fun itemUsesRemaining(itemId: Int, ctx: Context): Int {
        val name = ctx.gameDatabase.item(itemId)?.name ?: return Int.MAX_VALUE
        val limitsCtx = net.sourceforge.kolmafia.request.ItemUseLimitsContext(
            character = ctx.charState,
            preferences = ctx.preferences,
            expressionContext = ExpressionContext.from(ctx.charState, ctx.activeEffects, ctx.passiveSkillNames),
            accessibleCount = ctx.inventoryCount,
        )
        return net.sourceforge.kolmafia.request.maximumUses(itemId, name, limitsCtx)
    }

    private fun effectBoost(
        cmd: String,
        text: String,
        delta: Double,
        priority: Boolean = false,
    ): MaximizerBoost =
        MaximizerBoost(
            cmd = cmd,
            text = text,
            delta = delta,
            isEquipment = false,
            priority = priority,
        )

    private fun postEquipmentModifierValues(
        ctx: Context,
        activeEffects: List<EffectData> = ctx.activeEffects,
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
    ) = when (ctx.baseline) {
        NonEquipmentBaseline.PLAN_OVERLAY -> MaximizerSpeculation.modifierValuesForPostEquipmentPlan(
            plan = ctx.plan,
            charState = ctx.charState,
            activeEffects = activeEffects,
            passiveSkillNames = ctx.passiveSkillNames,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
            carryFamiliars = ctx.carryFamiliars,
            gameDatabase = ctx.gameDatabase,
            preferences = ctx.preferences,
        )
        NonEquipmentBaseline.LIVE_EQUIPPED -> MaximizerSpeculation.modifierValuesForPostEquipmentLive(
            charState = ctx.charState,
            activeEffects = activeEffects,
            passiveSkillNames = ctx.passiveSkillNames,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
            gameDatabase = ctx.gameDatabase,
            preferences = ctx.preferences,
        )
    }

    private fun postEquipmentScore(
        ctx: Context,
        activeEffects: List<EffectData> = ctx.activeEffects,
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
    ): Double = when (ctx.baseline) {
        NonEquipmentBaseline.PLAN_OVERLAY -> MaximizerSpeculation.scorePostEquipmentPlan(
            plan = ctx.plan,
            charState = ctx.charState,
            activeEffects = activeEffects,
            passiveSkillNames = ctx.passiveSkillNames,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
            carryFamiliars = ctx.carryFamiliars,
            gameDatabase = ctx.gameDatabase,
            preferences = ctx.preferences,
            thrallBonus = ctx.thrallBonus,
        )
        NonEquipmentBaseline.LIVE_EQUIPPED -> MaximizerSpeculation.scorePostEquipmentLive(
            charState = ctx.charState,
            evaluator = ctx.plan.spec.evaluator,
            activeEffects = activeEffects,
            passiveSkillNames = ctx.passiveSkillNames,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
            gameDatabase = ctx.gameDatabase,
            preferences = ctx.preferences,
            thrallBonus = ctx.thrallBonus,
            maxBeeosity = ctx.plan.spec.maxBeeosity,
        )
    }

    private fun checkedItemContext(ctx: Context): MaximizerCheckedItemBuilder.Context =
        MaximizerCheckedItemBuilder.Context(
            spec = ctx.plan.spec,
            gameDatabase = ctx.gameDatabase,
            characterState = ctx.charState,
            preferences = ctx.preferences,
            mallPriceManager = ctx.mallPriceManager,
            inventoryCount = ctx.inventoryCount,
            closetContents = ctx.inventory.closetContents,
            storageContents = ctx.inventory.storageContents,
            displayContents = ctx.inventory.displayContents,
            stashContents = ctx.inventory.stashContents,
            priceLevel = ctx.priceLevel,
        )

    private fun retrieveContext(ctx: Context): RetrieveItemSimulator.Context =
        RetrieveItemSimulator.Context(
            inventoryCount = ctx.inventoryCount,
            closetContents = ctx.inventory.closetContents,
            storageContents = ctx.inventory.storageContents,
            displayContents = ctx.inventory.displayContents,
            stashContents = ctx.inventory.stashContents,
            pullAllowed = { itemId ->
                !ctx.charState.isHardcore &&
                    PullableItems.storagePullAllowed(ctx.charState, itemId, ctx.gameDatabase)
            },
        )

    private fun itemIdFromSource(source: String, ctx: Context): Int? {
        val trimmed = source.trim()
        when {
            trimmed.startsWith("gong ", ignoreCase = true) -> return ItemDatabase.GONG
            trimmed.startsWith("skeleton ", ignoreCase = true) -> return SKELETON_ID
            trimmed.startsWith("play", ignoreCase = true) -> {
                if (ctx.inventoryCount(DECK_OF_EVERY_CARD_ID) > 0) return DECK_OF_EVERY_CARD_ID
                if (ctx.charState.inLegacyOfLoathing && ctx.inventoryCount(REPLICA_DECK_ID) > 0) {
                    return REPLICA_DECK_ID
                }
                return DECK_OF_EVERY_CARD_ID
            }
            trimmed.startsWith("gap ", ignoreCase = true) -> {
                if (ctx.inventoryCount(GREAT_PANTS_ID) > 0) return GREAT_PANTS_ID
                if (ctx.charState.inLegacyOfLoathing && ctx.inventoryCount(REPLICA_GREAT_PANTS_ID) > 0) {
                    return REPLICA_GREAT_PANTS_ID
                }
                return GREAT_PANTS_ID
            }
            trimmed.startsWith("spacegate", ignoreCase = true) -> return SPACEGATE_BADGE_ID
            trimmed.startsWith("aprilband ", ignoreCase = true) -> return APRILING_BAND_HELMET_ID
            trimmed.startsWith("loathingidol ", ignoreCase = true) -> {
                LOATHING_IDOL_MICROPHONE_IDS.firstOrNull { ctx.inventoryCount(it) > 0 }
                    ?: LOATHING_IDOL_MICROPHONE_IDS.last()
            }
            trimmed.startsWith("mayam ", ignoreCase = true) -> return MAYAM_CALENDAR_ID
        }
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size < 2) return null
        val target = parts.drop(1).joinToString(" ")
        return itemIdFromTarget(target, ctx)
    }

    private fun itemNameFromSource(source: String, ctx: Context): String? {
        val parts = source.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        return itemNameFromTarget(parts.drop(1).joinToString(" "), ctx)
    }

    private fun itemIdFromTarget(target: String, ctx: Context): Int? {
        val trimmed = target.trim()
        val qtyMatch = Regex("""^(\d+)\s+(.+)$""").matchEntire(trimmed)
        val token = qtyMatch?.groupValues?.get(2) ?: trimmed
        token.removePrefix("\u00B6").toIntOrNull()?.let { return it }
        token.removePrefix("[").removeSuffix("]").toIntOrNull()?.let { return it }
        ctx.gameDatabase.item(token)?.id?.let { return it }
        return BangPotionResolver.resolveItemId(token, ctx.preferences)
    }

    private fun itemNameFromTarget(target: String, ctx: Context): String? {
        itemIdFromTarget(target, ctx)?.let { return ctx.gameDatabase.item(it)?.name }
        val trimmed = target.trim()
        val qtyMatch = Regex("""^(\d+)\s+(.+)$""").matchEntire(trimmed)
        return (qtyMatch?.groupValues?.get(2) ?: trimmed).takeIf { it.isNotBlank() }
    }

    private fun meetsLevelRequirement(name: String, characterLevel: Int): Boolean {
        val req = ConsumableDatabase.getLevelReqByName(name) ?: return true
        return characterLevel >= req
    }

    private fun formatDelta(delta: Double): String {
        val rounded = (delta * 100.0).toLong() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            "%.2f".format(rounded)
        }
    }
}
