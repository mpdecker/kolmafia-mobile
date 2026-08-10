package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.equipment.ModeableState
import net.sourceforge.kolmafia.inventory.PullableItems
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.item.RetrieveItemSimulator
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Maximizer.emitSlot boost/retrieve emission (Phase 384). */
object MaximizerEmitSlot {

    private const val CROWN_ITEM_ID = 4614

    data class Plan(
        val goal: String,
        val spec: MaximizeSpec,
        val scoreBefore: Double,
        val scoreAfter: Double,
        val bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        val familiarSwitch: String? = null,
        val enthronedRace: String? = null,
        val bjornifiedRace: String? = null,
        val modeSelections: Map<Modeable, String> = emptyMap(),
        val cardInSleeve: String? = null,
    )

    data class InventorySnapshot(
        val closetContents: Map<Int, Int> = emptyMap(),
        val storageContents: Map<Int, Int> = emptyMap(),
        val displayContents: Map<Int, Int> = emptyMap(),
        val stashContents: Map<Int, Int> = emptyMap(),
    )

    data class Context(
        val plan: Plan,
        val charState: CharacterState,
        val inventory: InventorySnapshot,
        val inventoryCount: (Int) -> Int,
        val gameDatabase: GameDatabase,
        val preferences: Preferences?,
        val mallPriceManager: MallPriceManager?,
        val priceLevel: MaximizerPriceLevel,
        val equipScope: MaximizerEquipScope = MaximizerEquipScope.SPECULATE,
        val carryFamiliars: List<String> = emptyList(),
        val thrallBonus: Double = 0.0,
    )

    fun buildBoosts(ctx: Context): List<MaximizerBoost> {
        val boosts = mutableListOf<MaximizerBoost>()
        val checkedContext = checkedItemContext(ctx)
        val retrieveContext = retrieveContext(ctx)

        for (slot in EquipmentSlot.ALL_EMIT_SLOTS) {
            if (slot == EquipmentSlot.FAMILIAR) {
                emitFamiliarSwitch(ctx, boosts)
            }
            emitEquipmentSlot(slot, ctx, checkedContext, retrieveContext, boosts)
        }
        return boosts
    }

    private fun emitFamiliarSwitch(ctx: Context, boosts: MutableList<MaximizerBoost>) {
        val target = ctx.plan.familiarSwitch ?: return
        val current = ctx.charState.familiarName
        if (target.equals(current, ignoreCase = true)) return
        val delta = familiarSwitchDelta(ctx, target)
        val cmd = "familiar $target"
        val text = "$cmd (${formatDelta(delta)})"
        boosts += MaximizerBoost(
            cmd = cmd,
            text = text,
            slot = EquipmentSlot.FAMILIAR,
            familiarRace = target,
            delta = delta,
        )
    }

    private fun emitEquipmentSlot(
        slot: EquipmentSlot,
        ctx: Context,
        checkedContext: MaximizerCheckedItemBuilder.Context,
        retrieveContext: RetrieveItemSimulator.Context,
        boosts: MutableList<MaximizerBoost>,
    ) {
        val bestName = ctx.plan.bestPerSlot[slot]?.first?.takeIf { it.isNotBlank() } ?: return
        val itemId = ctx.gameDatabase.item(bestName)?.id ?: return
        val currentName = ctx.charState.equipment[slot].orEmpty()
        val currentId = ctx.gameDatabase.item(currentName)?.id ?: 0

        val enthronedRace = ctx.plan.enthronedRace
        val bjornifiedRace = ctx.plan.bjornifiedRace
        val modeable = Modeable.find(itemId)
        val targetMode = modeable?.let { ctx.plan.modeSelections[it] }
        val liveMode = modeable?.let { ModeableState.currentMode(ctx.preferences, it) }

        val changeEnthroned = itemId == CROWN_ITEM_ID &&
            enthronedRace != null &&
            !enthronedRace.equals(ctx.charState.enthronedFamiliarName, ignoreCase = true)
        val changeBjorned = bestName.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true) &&
            bjornifiedRace != null &&
            !bjornifiedRace.equals(ctx.charState.bjornedFamiliarName, ignoreCase = true)
        val changeModeable = modeable != null &&
            targetMode != null &&
            !targetMode.equals(liveMode, ignoreCase = true)

        val garbageChampagneReset = itemId == MaximizerGarbageAuto.BROKEN_CHAMPAGNE_ID &&
            (ctx.preferences?.getInt("garbageChampagneCharge", 0) ?: 0) == 0 &&
            !(ctx.preferences?.getBoolean("_garbageItemChanged", false) ?: false)
        val garbageShirtReset = itemId == MaximizerGarbageAuto.GARBAGE_SHIRT_ID &&
            (ctx.preferences?.getInt("garbageShirtCharge", 0) ?: 0) == 0 &&
            !(ctx.preferences?.getBoolean("_garbageItemChanged", false) ?: false)

        if (currentName.equals(bestName, ignoreCase = true) &&
            !changeEnthroned &&
            !changeBjorned &&
            !changeModeable &&
            !garbageChampagneReset &&
            !garbageShirtReset
        ) {
            if (ctx.equipScope == MaximizerEquipScope.EQUIP_NOW) return
            boosts += MaximizerBoost(
                cmd = "",
                text = "keep ${slot.name}: $bestName",
                slot = slot,
                itemId = itemId,
                itemName = bestName,
                delta = 0.0,
            )
            return
        }

        val checked = MaximizerCheckedItemBuilder.build(itemId, bestName, checkedContext)
        if (!passesEmitGate(checked, ctx)) return

        val delta = slotDelta(ctx, slot, bestName)
        val count = priorItemCount(itemId, slot, ctx, boosts)

        var cmd: String
        var text: String
        when {
            changeEnthroned -> {
                cmd = "enthrone $enthronedRace"
                if (!currentName.equals(bestName, ignoreCase = true)) {
                    cmd += "; equip ${slot.name} \u00B6$itemId"
                }
                text = if (currentName.equals(bestName, ignoreCase = true)) {
                    cmd
                } else {
                    "equip ${slot.name} $bestName & $cmd"
                }
            }
            changeBjorned -> {
                cmd = "bjornify $bjornifiedRace"
                if (!currentName.equals(bestName, ignoreCase = true)) {
                    cmd += "; equip ${slot.name} \u00B6$itemId"
                }
                text = if (currentName.equals(bestName, ignoreCase = true)) {
                    cmd
                } else {
                    "equip ${slot.name} $bestName & $cmd"
                }
            }
            changeModeable && modeable != null && targetMode != null -> {
                val modeText = "${modeable.command} $targetMode"
                cmd = modeText
                if (modeable.mustEquipAfterChange || !currentName.equals(bestName, ignoreCase = true)) {
                    cmd += "; equip ${slot.name} \u00B6$itemId"
                }
                text = if (!currentName.equals(bestName, ignoreCase = true)) {
                    "equip ${slot.name} $bestName & $modeText"
                } else if (modeable.mustEquipAfterChange) {
                    "$modeText & equip ${slot.name} $bestName"
                } else {
                    modeText
                }
            }
            else -> {
                cmd = "equip ${slot.name} \u00B6$itemId"
                text = "equip ${slot.name} $bestName"
            }
        }

        text = "$text ("
        var price = 0L

        if (garbageChampagneReset || garbageShirtReset) {
            if (checked.initial > count) {
                text = "fold & $text"
                cmd = "fold \u00B6$itemId;$cmd"
            }
            if (currentName.equals(bestName, ignoreCase = true)) {
                text = "unequip & $text"
                cmd = "unequip ${slot.name};$cmd"
            }
        }

        if (!currentName.equals(bestName, ignoreCase = true) && checked.initial > count) {
            val method = RetrieveItemSimulator.simRetrieve(itemId, count + 1, retrieveContext)
            if (method != "have") {
                text = "$method & $text"
            }
            cmd = when (method) {
                "uncloset" -> "closet take 1 \u00B6$itemId;$cmd"
                "unstash" -> "stash take 1 \u00B6$itemId;$cmd"
                "undisplay" -> "display take 1 \u00B6$itemId;$cmd"
                "pull" -> "pull 1 \u00B6$itemId;$cmd"
                else -> cmd
            }
        } else if (checked.creatable + checked.initial > count) {
            text = "make & $text"
            cmd = "make \u00B6$itemId;$cmd"
            price = concoctionPrice(bestName)
        } else if (checked.npcBuyable + checked.initial > count) {
            text = "buy & $text"
            cmd = "buy 1 \u00B6$itemId;$cmd"
            price = concoctionPrice(bestName)
        } else if (checked.foldable + checked.initial > count) {
            val foldMethod = RetrieveItemSimulator.simRetrieve(
                checked.foldItemId,
                count + 1,
                retrieveContext,
            )
            if (foldMethod == "have" || foldMethod == "remove") {
                text = "fold & $text"
                cmd = "fold \u00B6$itemId;$cmd"
            } else {
                text = "$foldMethod & fold & $text"
                cmd = "acquire 1 \u00B6${checked.foldItemId};fold \u00B6$itemId;$cmd"
            }
        } else if (checked.pullable + checked.initial > count) {
            text = "pull & $text"
            cmd = "pull \u00B6$itemId;$cmd"
        } else if (checked.pullfoldable + checked.initial > count) {
            text = "pull & fold & $text"
            cmd = "pull 1 \u00B6${checked.foldItemId};fold \u00B6$itemId;$cmd"
        } else if (checked.pullBuyable + checked.initial > count) {
            text = "buy & pull & $text"
            cmd = "buy using storage 1 \u00B6$itemId;pull \u00B6$itemId;$cmd"
            if (ctx.priceLevel != MaximizerPriceLevel.DONT_CHECK) {
                price = ctx.mallPriceManager?.getMallPrice(itemId) ?: 0L
            }
        } else {
            text = "acquire & $text"
            if (ctx.priceLevel != MaximizerPriceLevel.DONT_CHECK) {
                price = ctx.mallPriceManager?.getMallPrice(itemId) ?: 0L
            }
        }

        val costCtx = MaximizerBoostCostSuffix.Context(
            gameDatabase = ctx.gameDatabase,
            charState = ctx.charState,
            preferences = ctx.preferences,
            physicalAccessible = { id -> checkedContext.physicalAccessible(id) },
            mallPrice = { id -> ctx.mallPriceManager?.getMallPrice(id) ?: 0L },
        )
        val costs = MaximizerBoostCostSuffix.accumulateFromCmd(cmd, costCtx)
        if (MaximizerBoostCostSuffix.shouldSkipBoost(costs, ctx.preferences)) return
        text = MaximizerBoostCostSuffix.appendToText(text, costs)
        if (price > 0) {
            text += "${formatMeat(price)} meat, "
        }
        cmd = MaximizerBoostCostSuffix.applyCapacityGreyout(
            cmd,
            costs,
            ctx.charState,
            checkMeat = false,
        )
        text += "${formatDelta(delta)})"

        boosts += MaximizerBoost(
            cmd = cmd,
            text = text,
            slot = slot,
            itemId = itemId,
            itemName = bestName,
            delta = delta,
        )
    }

    private fun passesEmitGate(checked: MaximizerCheckedItem, ctx: Context): Boolean {
        val maxPrice = ctx.plan.spec.maxPrice?.toLong() ?: return true
        val mallPrice = ctx.mallPriceManager?.getMallPrice(checked.itemId) ?: 0L
        val historicalPrice = ctx.mallPriceManager?.getHistoricalPrice(checked.itemId) ?: 0L
        return checked.passesEmitMallCheck(
            priceLevel = ctx.priceLevel,
            maxPrice = maxPrice,
            mallPrice = mallPrice,
            historicalPrice = historicalPrice,
            tradeable = ItemDatabase.isTradeable(checked.itemId),
        )
    }

    private fun priorItemCount(
        itemId: Int,
        slot: EquipmentSlot,
        ctx: Context,
        boosts: List<MaximizerBoost>,
    ): Int {
        if (ctx.equipScope == MaximizerEquipScope.EQUIP_NOW) {
            var count = 0
            for (searchSlot in EquipmentSlot.ALL_EMIT_SLOTS) {
                if (searchSlot == slot) break
                val equippedId = ctx.gameDatabase.item(ctx.charState.equipment[searchSlot].orEmpty())?.id
                if (equippedId == itemId) count++
            }
            return count
        }
        return boosts.count { it.itemId == itemId }
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

    private fun slotDelta(ctx: Context, slot: EquipmentSlot, itemName: String): Double {
        val assignment = ctx.charState.equipment.mapValues { (_, name) -> name to 0.0 }.toMutableMap()
        assignment[slot] = itemName to 0.0
        return MaximizerSpeculation.scoreLoadout(
            baseState = ctx.charState,
            assignment = assignment,
            evaluator = ctx.plan.spec.evaluator,
            familiarBonus = 0.0,
            thrallBonus = ctx.thrallBonus,
            bestModes = ctx.plan.modeSelections,
            carryFamiliars = ctx.carryFamiliars,
            gameDatabase = ctx.gameDatabase,
            cardInSleeve = ctx.plan.cardInSleeve,
            preferences = ctx.preferences,
            maxBeeosity = ctx.plan.spec.maxBeeosity,
        ) - ctx.plan.scoreBefore
    }

    private fun familiarSwitchDelta(ctx: Context, targetRace: String): Double {
        val assignment = ctx.plan.bestPerSlot
        val baseBonus = 0.0
        val withSwitch = MaximizerSpeculation.scoreLoadout(
            baseState = ctx.charState.copy(familiarName = targetRace),
            assignment = assignment,
            evaluator = ctx.plan.spec.evaluator,
            familiarBonus = 0.0,
            thrallBonus = ctx.thrallBonus,
            bestModes = ctx.plan.modeSelections,
            carryFamiliars = ctx.carryFamiliars,
            gameDatabase = ctx.gameDatabase,
            cardInSleeve = ctx.plan.cardInSleeve,
            preferences = ctx.preferences,
            maxBeeosity = ctx.plan.spec.maxBeeosity,
        )
        return withSwitch - ctx.plan.scoreBefore
    }

    private fun adventureCost(
        itemId: Int,
        checkedContext: MaximizerCheckedItemBuilder.Context,
        ctx: Context,
    ): Int {
        val concoction = ConcoctionDatabase.getByResult(ctx.gameDatabase.item(itemId)?.name ?: return 0)
            ?: return 0
        if (concoction.ingredients.isEmpty()) return 0
        return CreatableTurns.adventuresNeeded(
            itemId = itemId,
            quantityNeeded = 1,
            inventoryCount = { checkedContext.physicalAccessible(itemId) },
            isPermitted = { true },
        )
    }

    private fun concoctionPrice(name: String): Long {
        val concoction = ConcoctionDatabase.getByResult(name) ?: return 0L
        return ConcoctionCreationCost.creationCost(concoction.methods)
    }

    private fun formatDelta(delta: Double): String {
        val rounded = (delta * 100.0).toLong() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            "%.2f".format(rounded)
        }
    }

    private fun formatMeat(price: Long): String =
        price.toString().reversed().chunked(3).joinToString(",").reversed()
}
