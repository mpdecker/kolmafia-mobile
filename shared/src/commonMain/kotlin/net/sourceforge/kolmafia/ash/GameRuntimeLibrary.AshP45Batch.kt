package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.MonsterDrop
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierStats
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP45 — meat_drop / item_drops / item_drops_array from [MonsterDefinition] drop data.
 * Mirrors desktop [RuntimeLibrary] meat/item drop queries.
 *
 * Phase 6441–6450: RandomModifierStats overlay + fractional conditional / multi-drop rates
 * via [MonsterDrop.dropRate] Double parsing.
 *
 * Phase 6611–6630 (XLVI Track A): OCRS meat residual (broke / solid gold) +
 * DropFlag.UNKNOWN_RATE (`"0"`) when chance is 0 with no letter prefix.
 */
internal fun GameRuntimeLibrary.registerAshP45Batch(scope: AshScope) {
    val itemFloatType = AggregateType(AshType.ITEM, AshType.FLOAT)
    val itemDropArrayType = AggregateType(AshType.INT, ITEM_DROP_REC)

    fun lastMonster() =
        MonsterStatusTracker.getLastMonster()
            ?: resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    fun effective(monster: MonsterDefinition?): MonsterDefinition? {
        if (monster == null) return null
        return RandomModifierStats.apply(
            monster,
            monster.randomModifiers,
            buildMonsterExpressionContext(),
        )
    }

    fun meatDropValue(monster: MonsterDefinition?): AshValue {
        val m = effective(monster)
        return if (m == null) AshValue.of(-1L) else AshValue.of(m.meatDrop.toLong())
    }

    regFn(scope, "meat_drop", AshType.INT, emptyList()) { _, _ ->
        meatDropValue(lastMonster())
    }

    regFn(scope, "meat_drop", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        meatDropValue(resolveMonsterDefinition(args[0].toString()))
    }

    regFn(scope, "item_drops", itemFloatType, emptyList()) { _, _ ->
        buildItemDrops(effective(lastMonster()), itemFloatType)
    }

    regFn(scope, "item_drops", itemFloatType, listOf("monster" to AshType.MONSTER)) { _, args ->
        buildItemDrops(effective(resolveMonsterDefinition(args[0].toString())), itemFloatType)
    }

    regFn(scope, "item_drops_array", itemDropArrayType, emptyList()) { _, _ ->
        buildItemDropsArray(effective(lastMonster()), itemDropArrayType)
    }

    regFn(
        scope,
        "item_drops_array",
        itemDropArrayType,
        listOf("monster" to AshType.MONSTER),
    ) { _, args ->
        buildItemDropsArray(effective(resolveMonsterDefinition(args[0].toString())), itemDropArrayType)
    }
}

/** Desktop anonymous record `{item drop; float rate; string type;}`. */
internal val ITEM_DROP_REC = RecordType(
    "{item drop; float rate; string type;}",
    listOf(
        RecordField("drop", AshType.ITEM, 0),
        RecordField("rate", AshType.FLOAT, 1),
        RecordField("type", AshType.STRING, 2),
    ),
)

private fun buildItemDrops(monster: MonsterDefinition?, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    if (monster == null) return result
    for (drop in monster.drops) {
        result[AshValue.item(drop.itemName)] = AshValue.of(drop.dropRate)
    }
    return result
}

private fun buildItemDropsArray(monster: MonsterDefinition?, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    if (monster == null) return result
    monster.drops.forEachIndexed { i, drop ->
        result[AshValue.of(i)] = dropRecord(drop)
    }
    return result
}

private fun dropRecord(drop: MonsterDrop): RecordValue {
    val rec = RecordValue(ITEM_DROP_REC)
    rec.setField(0, AshValue.item(drop.itemName))
    rec.setField(1, AshValue.of(drop.dropRate))
    // Desktop DropFlag.toString(): letter id, or UNKNOWN_RATE "0" when chance==0 && NONE.
    val flag = when {
        drop.prefix != null -> drop.prefix.toString()
        drop.dropRate == 0.0 -> "0"
        else -> ""
    }
    if (flag.isNotEmpty()) {
        rec.setField(2, AshValue.of(flag))
    }
    return rec
}
