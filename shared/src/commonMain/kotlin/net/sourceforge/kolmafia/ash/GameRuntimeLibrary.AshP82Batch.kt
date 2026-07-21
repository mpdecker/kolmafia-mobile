package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.PocketDatabase.MonsterPocket
import net.sourceforge.kolmafia.data.PocketDatabase.Pocket
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.character.CharacterState

/**
 * AshP82 — PocketDatabase cargo routing ASH.
 * Mirrors desktop [RuntimeLibrary] pocket_monster, *_pockets, available_pocket, pick_pocket.
 */
internal fun GameRuntimeLibrary.registerAshP82Batch(scope: AshScope) {
    val pocketSetType = AggregateType(AshType.INT, AshType.BOOLEAN)

    regFn(scope, "pocket_monster", AshType.MONSTER, listOf("pocket" to AshType.INT)) { _, args ->
        val pocketNum = args[0].toLong().toInt()
        val pocket = PocketDatabase.pocketByNumber(pocketNum) as? MonsterPocket
        if (pocket != null) {
            AshValue(AshType.MONSTER, pocket.monster.name)
        } else {
            AshValue(AshType.MONSTER, "")
        }
    }

    regFn(scope, "monster_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.allMonsterPockets, pocketSetType)
    }

    regFn(scope, "effect_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.allEffectPockets, pocketSetType)
    }

    regFn(scope, "item_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.allItemPockets, pocketSetType)
    }

    regFn(scope, "stats_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.allStatsPockets, pocketSetType)
    }

    regFn(scope, "available_pocket", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        availablePocketValue(sortedMonsterPockets(args[0].toString()))
    }

    regFn(scope, "available_pocket", AshType.INT, listOf("effect" to AshType.EFFECT)) { _, args ->
        availablePocketValue(sortedEffectPockets(args[0].toString()))
    }

    regFn(scope, "available_pocket", AshType.INT, listOf("item" to AshType.ITEM)) { _, args ->
        availablePocketValue(sortedItemPockets(args[0].toString()))
    }

    regFn(scope, "available_pocket", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
        availablePocketValue(sortedStatPockets(args[0].toString()))
    }

    regFn(scope, "pick_pocket", AshType.BOOLEAN, listOf("pocketNumber" to AshType.INT)) { _, args ->
        pickPocketValue(args[0].toLong().toInt())
    }

    regFn(scope, "pick_pocket", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        pickPocketValue(sortedMonsterPockets(args[0].toString()))
    }

    regFn(scope, "pick_pocket", AshType.BOOLEAN, listOf("effect" to AshType.EFFECT)) { _, args ->
        pickPocketValue(sortedEffectPockets(args[0].toString()))
    }

    regFn(scope, "pick_pocket", AshType.BOOLEAN, listOf("item" to AshType.ITEM)) { _, args ->
        pickPocketValue(sortedItemPockets(args[0].toString()))
    }

    regFn(scope, "pick_pocket", AshType.BOOLEAN, listOf("stat" to AshType.STAT)) { _, args ->
        pickPocketValue(sortedStatPockets(args[0].toString()))
    }
}

private fun GameRuntimeLibrary.availablePocketValue(sorted: List<Pocket>): AshValue {
    val picked = cargoPocketSync?.pickedPocketIds().orEmpty()
    val pocket = PocketDatabase.firstUnpickedPocket(sorted, picked)
    return if (pocket == null) AshValue.ZERO else AshValue.of(pocket.pocket.toLong())
}

private fun GameRuntimeLibrary.pickPocketValue(pocketNum: Int): AshValue {
    val pocket = PocketDatabase.pocketByNumber(pocketNum) ?: return AshValue.FALSE
    return pickPocketValue(listOf(pocket))
}

private fun GameRuntimeLibrary.pickPocketValue(sorted: List<Pocket>): AshValue {
    val picked = cargoPocketSync?.pickedPocketIds().orEmpty()
    val pocket = PocketDatabase.firstUnpickedPocket(sorted, picked) ?: return AshValue.FALSE
    return pickPocketByNumber(pocket.pocket)
}

private fun GameRuntimeLibrary.pickPocketByNumber(pocketNum: Int): AshValue {
    val mgr = cargoCultManager ?: return AshValue.FALSE
    val invState = inventoryManager?.state?.value ?: InventoryState()
    val charState = character?.state?.value
    val ok = runBlocking {
        mgr.pickPocketNumber(pocketNum, invState, charState)
    }
    return if (ok) AshValue.TRUE else AshValue.FALSE
}
