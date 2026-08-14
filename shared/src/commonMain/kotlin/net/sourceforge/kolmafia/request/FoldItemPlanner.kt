package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.TorsoAwareness
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop FoldItemCommand planning (source walk, gates, special equipped folds). */
object FoldItemPlanner {

    const val SPIRIT_OF_RIGATONI = 3011
    const val SPECIAL_SAUCE_GLOVE = 531
    const val GARBAGE_TOTE = 9690
    const val DECEASED_TREE = 9691
    const val BROKEN_CHAMPAGNE = 9692
    const val MAKESHIFT_GARBAGE_SHIRT = 9699
    const val REPLICA_GARBAGE_TOTE = 11238
    const val GARBAGE_CHOICE = 1275

    data class Context(
        val inventoryCount: (Int) -> Int,
        val equippedSlot: (String) -> EquipmentSlot?,
        val accessibleCount: (Int) -> Int,
        val skills: List<SkillData>,
        val charState: CharacterState,
        val preferences: Preferences?,
        val itemId: (String) -> Int?,
        val itemName: (Int) -> String?,
        val isShirt: (Int) -> Boolean,
        val isChefStaff: (String) -> Boolean,
        val foldGroup: (String) -> FoldGroup? = { FoldGroupDatabase.groupFor(it) },
    )

    enum class Special {
        BORIS_HELM,
        JARLSBERG_PAN,
        PETE_JACKET,
        TOGGLE,
        LOATHING_LEGION,
        GARBAGE_TOTE,
    }

    data class Plan(
        val alreadyHave: Boolean = false,
        val error: String? = null,
        val retrieveItemId: Int? = null,
        val unequipSlot: EquipmentSlot? = null,
        val special: Special? = null,
        val legionSlot: EquipmentSlot? = null,
        val sourceItemId: Int? = null,
        val useItemIds: List<Int> = emptyList(),
        val hpDamage: Long = 0,
        val toteItemId: Int? = null,
        val toteOption: Int? = null,
        val checkOnlyLine: String? = null,
    )

    fun plan(targetId: Int, ctx: Context): Plan {
        val targetName = ctx.itemName(targetId) ?: return Plan(error = "That's not a transformable item!")
        if (ctx.inventoryCount(targetId) > 0 && !shouldRefoldGarbage(targetId, ctx)) {
            return Plan(alreadyHave = true)
        }
        val group = ctx.foldGroup(targetName) ?: return Plan(error = "That's not a transformable item!")
        if (ctx.isShirt(targetId) && !TorsoAwareness.hasTorsoAwareness(ctx.skills)) {
            return Plan(error = "You can't make a shirt")
        }
        if (ctx.isChefStaff(targetName) && !canMakeChefStaff(ctx)) {
            return Plan(error = "You can't make a chefstaff")
        }
        val targetIndex = group.items.indexOfFirst { it.equals(targetName, ignoreCase = true) }
        if (targetIndex < 0) {
            return Plan(error = "Internal error: cannot find $targetName in fold group")
        }
        val walk = walkSource(group, targetIndex, ctx)
        val equippedSlot = ctx.equippedSlot(targetName)
        if (targetName.startsWith("Boris's Helm", ignoreCase = true) && equippedSlot != null) {
            return Plan(special = Special.BORIS_HELM, unequipSlot = equippedSlot)
        }
        if (targetName.startsWith("Jarlsberg's pan", ignoreCase = true) && equippedSlot != null) {
            return Plan(special = Special.JARLSBERG_PAN)
        }
        if (targetName.startsWith("Sneaky Pete's leather jacket", ignoreCase = true) && equippedSlot != null) {
            return Plan(special = Special.PETE_JACKET)
        }
        if (targetName.startsWith("toggle switch", ignoreCase = true) && equippedSlot != null) {
            return Plan(special = Special.TOGGLE)
        }
        var sourceId = walk.sourceId
        var retrieveId = walk.retrieveId
        var wornSlot = walk.wornSlot
        var wornId = walk.wornId
        var wornIndex = walk.wornIndex
        if (sourceId == null && wornId == null && retrieveId != null) {
            sourceId = retrieveId
        } else {
            retrieveId = null
        }
        var legionSlot: EquipmentSlot? = null
        if (sourceId == null) {
            if (walk.multipleWorn && ctx.preferences?.getBoolean("errorOnAmbiguousFold", false) == true) {
                return Plan(error = "Unequip the item you want to fold into that.")
            }
            if (wornId == null) {
                return Plan(error = "You don't have anything transformable into that item!")
            }
            if (targetName.startsWith("Loathing Legion", ignoreCase = true) &&
                sameConsumption(targetId, wornId, ctx)
            ) {
                legionSlot = wornSlot
            } else {
                // unequip worn then use it
            }
            sourceId = wornId
        }
        val sourceName = sourceId?.let { ctx.itemName(it) } ?: targetName
        val checkOnly = "$sourceName => $targetName"
        if (targetName.startsWith("Loathing Legion", ignoreCase = true)) {
            return Plan(
                retrieveItemId = retrieveId,
                unequipSlot = if (legionSlot == null) wornSlot else null,
                special = Special.LOATHING_LEGION,
                legionSlot = legionSlot,
                sourceItemId = sourceId,
                checkOnlyLine = checkOnly,
            )
        }
        val groupHead = group.items.firstOrNull().orEmpty()
        if (groupHead.equals("january's garbage tote", ignoreCase = true) ||
            groupHead.equals("January's Garbage Tote", ignoreCase = true)
        ) {
            val tote = if (ctx.charState.inLegacyOfLoathing && ctx.inventoryCount(REPLICA_GARBAGE_TOTE) > 0) {
                REPLICA_GARBAGE_TOTE
            } else {
                GARBAGE_TOTE
            }
            return Plan(
                retrieveItemId = tote,
                special = Special.GARBAGE_TOTE,
                toteItemId = tote,
                toteOption = targetIndex,
                checkOnlyLine = checkOnly,
            )
        }
        val damagePct = group.hpDamagePct
        val hpDamage = if (damagePct == 0) {
            0L
        } else {
            ctx.charState.maxHp.toLong() * damagePct / 100 + 2
        }
        val useIds = foldUseChain(group, sourceId, targetIndex, ctx)
        return Plan(
            retrieveItemId = retrieveId,
            unequipSlot = if (sourceId == wornId) wornSlot else null,
            sourceItemId = sourceId,
            useItemIds = useIds,
            hpDamage = hpDamage,
            checkOnlyLine = checkOnly,
        )
    }

    private data class Walk(
        val sourceId: Int?,
        val wornId: Int?,
        val wornSlot: EquipmentSlot?,
        val wornIndex: Int,
        val retrieveId: Int?,
        val multipleWorn: Boolean,
    )

    private fun walkSource(group: FoldGroup, targetIndex: Int, ctx: Context): Walk {
        val groupSize = group.items.size
        var sourceIndex = if (targetIndex > 0) targetIndex - 1 else groupSize - 1
        var sourceId: Int? = null
        var wornId: Int? = null
        var wornSlot: EquipmentSlot? = null
        var wornIndex = 0
        var retrieveId: Int? = null
        var multipleWorn = false
        while (sourceIndex != targetIndex) {
            val form = group.items[sourceIndex]
            val itemId = ctx.itemId(form) ?: run {
                sourceIndex = if (sourceIndex > 0) sourceIndex - 1 else groupSize - 1
                continue
            }
            if (ctx.inventoryCount(itemId) > 0) {
                sourceId = itemId
                break
            }
            val slot = ctx.equippedSlot(form)
            if (slot != null) {
                if (wornId != null) multipleWorn = true
                if (wornSlot == null || slot.ordinal > wornSlot.ordinal) {
                    wornId = itemId
                    wornSlot = slot
                    wornIndex = sourceIndex
                }
            } else if (retrieveId == null && ctx.accessibleCount(itemId) > 0) {
                retrieveId = itemId
            }
            sourceIndex = if (sourceIndex > 0) sourceIndex - 1 else groupSize - 1
        }
        return Walk(sourceId, wornId, wornSlot, wornIndex, retrieveId, multipleWorn)
    }

    private fun foldUseChain(
        group: FoldGroup,
        sourceId: Int?,
        targetIndex: Int,
        ctx: Context,
    ): List<Int> {
        val groupSize = group.items.size
        val sourceName = sourceId?.let { ctx.itemName(it) } ?: return emptyList()
        var sourceIndex = group.items.indexOfFirst { it.equals(sourceName, ignoreCase = true) }
        if (sourceIndex < 0) return emptyList()
        val uses = mutableListOf<Int>()
        while (sourceIndex != targetIndex) {
            val itemId = ctx.itemId(group.items[sourceIndex]) ?: break
            uses += itemId
            sourceIndex = if (sourceIndex < groupSize - 1) sourceIndex + 1 else 0
        }
        return uses
    }

    private fun shouldRefoldGarbage(targetId: Int, ctx: Context): Boolean {
        if (ctx.preferences?.getBoolean("_garbageItemChanged", false) == true) return false
        return when (targetId) {
            DECEASED_TREE -> ctx.preferences?.getInt("garbageTreeCharge", 0) == 0
            BROKEN_CHAMPAGNE -> ctx.preferences?.getInt("garbageChampagneCharge", 0) == 0
            MAKESHIFT_GARBAGE_SHIRT -> ctx.preferences?.getInt("garbageShirtCharge", 0) == 0
            else -> false
        }
    }

    private fun canMakeChefStaff(ctx: Context): Boolean {
        if (ctx.skills.any { it.id == SPIRIT_OF_RIGATONI }) return true
        if (ctx.charState.isJarlsberg) return true
        if (ctx.charState.isSauceror) {
            val gloveName = ctx.itemName(SPECIAL_SAUCE_GLOVE) ?: "special sauce glove"
            if (ctx.equippedSlot(gloveName) != null) return true
        }
        return false
    }

    private fun sameConsumption(a: Int, b: Int, ctx: Context): Boolean {
        val na = ctx.itemName(a) ?: return false
        val nb = ctx.itemName(b) ?: return false
        return ctx.isShirt(a) == ctx.isShirt(b) &&
            ctx.isChefStaff(na) == ctx.isChefStaff(nb)
    }
}
