package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.NoobcoreAbsorbs
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.inventory.PullableItems
import net.sourceforge.kolmafia.item.RetrieveItemSimulator
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser

/** Desktop Maximizer noobcore absorb boosts (Phase 388). */
object MaximizerNoobcoreAbsorbBoosts {

    private const val NOOB_SKILL_MIN = 23001
    private const val NOOB_SKILL_MAX = 23125

    data class Absorbable(
        val cmd: String,
        val text: String,
        val canMake: Boolean,
        val checked: MaximizerCheckedItem,
        val meatPrice: Long = 0L,
    )

    fun build(ctx: MaximizerNonEquipmentBoosts.Context): List<MaximizerBoost> {
        if (!ctx.charState.inNoobcore) return emptyList()
        val absorbsLeft = NoobcoreAbsorbs.absorbsRemaining(ctx.charState)
        if (absorbsLeft < 1) return emptyList()
        val baseline = postEquipmentScore(ctx)
        val boosts = mutableListOf<MaximizerBoost>()
        boosts += buildSkillAbsorbBoosts(ctx, baseline, absorbsLeft)
        boosts += buildEquipmentAbsorbBoosts(ctx, baseline, absorbsLeft)
        return boosts
    }

    private fun buildSkillAbsorbBoosts(
        ctx: MaximizerNonEquipmentBoosts.Context,
        baseline: Double,
        absorbsLeft: Int,
    ): List<MaximizerBoost> {
        val boosts = mutableListOf<MaximizerBoost>()
        for (skillId in NOOB_SKILL_MIN..NOOB_SKILL_MAX) {
            if (hasSkill(ctx, skillId)) continue
            val skillName = SkillDefinitionDatabase.getById(skillId)?.name ?: continue
            val mods = ModifierDatabase.getSkill(skillName)?.modifiers?.takeIf {
                it.isNotBlank() && !it.equals("none", ignoreCase = true)
            } ?: continue
            val delta = postEquipmentScore(ctx, customModifierOverlay = mods) - baseline
            if (delta <= 0.0) continue
            val items = ItemDatabase.getItemListByNoobSkillId(skillId)
            if (items.isEmpty()) continue
            var count = 0
            for (itemId in items) {
                val absorbable = getAbsorbable(itemId, ctx) ?: continue
                if (!absorbable.canMake) continue
                var text = absorbable.text + formatDelta(delta) + ")"
                text += " [$absorbsLeft absorbs remaining]"
                if (count > 0) text = "  or $text"
                boosts += MaximizerBoost(
                    cmd = absorbable.cmd,
                    text = text,
                    delta = delta,
                    isEquipment = false,
                )
                count++
            }
        }
        return boosts
    }

    private fun buildEquipmentAbsorbBoosts(
        ctx: MaximizerNonEquipmentBoosts.Context,
        baseline: Double,
        absorbsLeft: Int,
    ): List<MaximizerBoost> {
        val boosts = mutableListOf<MaximizerBoost>()
        for (itemId in EquipmentDatabase.allEquipmentItemIds()) {
            val item = ctx.gameDatabase.item(itemId) ?: ItemDatabase.getById(itemId) ?: continue
            if (!item.isEquipment || item.primaryUse == ItemPrimaryUse.FAMILIAR) continue
            if (!ItemDatabase.isDiscardable(itemId)) continue
            if (!ItemDatabase.isTradeable(itemId) && !ItemDatabase.isGiftItem(itemId)) continue
            val overlay = itemDoubleModifierOverlay(item.name) ?: continue
            val delta = postEquipmentScore(ctx, customModifierOverlay = overlay) - baseline
            if (delta <= 0.0) continue
            val absorbable = getAbsorbable(itemId, ctx) ?: continue
            if (!absorbable.canMake) continue
            val inventory = ctx.inventoryCount(itemId)
            var text = absorbable.text
            text += "lasts til end of day, ${formatDelta(delta)})"
            text += formatEquipmentBracket(absorbable.checked, inventory, absorbsLeft)
            boosts += MaximizerBoost(
                cmd = absorbable.cmd,
                text = text,
                delta = delta,
                isEquipment = false,
            )
        }
        return boosts
    }

    internal fun getAbsorbable(
        itemId: Int,
        ctx: MaximizerNonEquipmentBoosts.Context,
    ): Absorbable? {
        val itemName = ctx.gameDatabase.item(itemId)?.name ?: ItemDatabase.getItemName(itemId)
        if (itemName.isBlank()) return null
        val checked = MaximizerCheckedItemBuilder.build(
            itemId,
            itemName,
            checkedItemContext(ctx),
        )
        var cmd = "absorb \u00B6$itemId"
        var text = "absorb $itemName ("
        var meatPrice = 0L
        var canMake = true

        if (ctx.inventoryCount(itemId) > 0) {
            // already in inventory
        } else if (checked.initial > 0) {
            val method = RetrieveItemSimulator.simRetrieve(itemId, 1, retrieveContext(ctx))
            if (method != "have") {
                text = "$method & $text"
            }
            when (method) {
                "uncloset" -> cmd = "closet take 1 \u00B6$itemId;$cmd"
                "pull" -> cmd = "pull 1 \u00B6$itemId;$cmd"
            }
        } else if (checked.creatable > 0) {
            text = "make & $text"
            cmd = "make \u00B6$itemId;$cmd"
            meatPrice = ctx.gameDatabase.npcPrice(itemName).toLong()
        } else if (checked.npcBuyable > 0) {
            text = "buy & $text"
            cmd = "buy 1 \u00B6$itemId;$cmd"
            meatPrice = ctx.gameDatabase.npcPrice(itemName).toLong()
        } else if (checked.pullable > 0) {
            text = "pull & $text"
            cmd = "pull \u00B6$itemId;$cmd"
        } else if (checked.mallBuyable > 0) {
            text = "acquire & $text"
            if (ctx.priceLevel != MaximizerPriceLevel.DONT_CHECK) {
                meatPrice = ctx.mallPriceManager?.getMallPrice(itemId) ?: 0L
            }
        } else if (checked.pullBuyable > 0) {
            text = "buy & pull & $text"
            cmd = "buy using storage 1 \u00B6$itemId;pull \u00B6$itemId;$cmd"
            if (ctx.priceLevel != MaximizerPriceLevel.DONT_CHECK) {
                meatPrice = ctx.mallPriceManager?.getMallPrice(itemId) ?: 0L
            }
        } else {
            canMake = false
        }

        if (meatPrice > 0L) {
            text += "$meatPrice meat, "
        }
        return Absorbable(cmd = cmd, text = text, canMake = canMake, checked = checked, meatPrice = meatPrice)
    }

    private fun itemDoubleModifierOverlay(itemName: String): String? {
        val raw = ModifierDatabase.getItem(itemName)?.modifiers ?: return null
        if (raw.isBlank() || raw.equals("none", ignoreCase = true)) return null
        val parsed = ModifierParser.parse(raw)
        val parts = buildList {
            for (mod in DoubleModifier.entries) {
                val value = parsed.get(mod)
                if (value != 0.0) add("${mod.tag}: $value")
            }
        }
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun formatEquipmentBracket(
        checked: MaximizerCheckedItem,
        inventory: Int,
        absorbsLeft: Int,
    ): String {
        val parts = mutableListOf("$absorbsLeft absorbs remaining")
        if (inventory > 0) parts += "$inventory in inventory"
        val obtainable = checked.initial - inventory
        if (obtainable > 0) parts += "$obtainable obtainable"
        if (checked.creatable > 0) parts += "${checked.creatable} creatable"
        if (checked.npcBuyable > 0) parts += "${checked.npcBuyable} NPC buyable"
        if (checked.pullable > 0) parts += "${checked.pullable} pullable"
        return " [${parts.joinToString(", ")}]"
    }

    private fun hasSkill(ctx: MaximizerNonEquipmentBoosts.Context, skillId: Int): Boolean =
        ctx.skillManager?.state?.value?.skills?.any { it.id == skillId } == true

    private fun postEquipmentScore(
        ctx: MaximizerNonEquipmentBoosts.Context,
        customModifierOverlay: String? = null,
    ): Double = MaximizerSpeculation.scorePostEquipmentPlan(
        plan = ctx.plan,
        charState = ctx.charState,
        activeEffects = ctx.activeEffects,
        carryFamiliars = ctx.carryFamiliars,
        gameDatabase = ctx.gameDatabase,
        preferences = ctx.preferences,
        thrallBonus = ctx.thrallBonus,
        customModifierOverlay = customModifierOverlay,
    )

    private fun checkedItemContext(ctx: MaximizerNonEquipmentBoosts.Context): MaximizerCheckedItemBuilder.Context =
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

    private fun retrieveContext(ctx: MaximizerNonEquipmentBoosts.Context): RetrieveItemSimulator.Context =
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

    private fun formatDelta(delta: Double): String {
        val rounded = (delta * 100.0).toLong() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            "%.2f".format(rounded)
        }
    }
}
