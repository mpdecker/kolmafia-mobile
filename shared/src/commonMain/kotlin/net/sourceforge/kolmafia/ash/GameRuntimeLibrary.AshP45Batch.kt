package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.MonsterDrop
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP45 — meat_drop / item_drops / item_drops_array from [MonsterDefinition] drop data.
 * Mirrors desktop [RuntimeLibrary] meat/item drop queries.
 */
internal fun GameRuntimeLibrary.registerAshP45Batch(scope: AshScope) {
    val itemFloatType = AggregateType(AshType.ITEM, AshType.FLOAT)
    val itemDropArrayType = AggregateType(AshType.INT, ITEM_DROP_REC)

    fun lastMonster() =
        resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    fun meatDropValue(monster: MonsterDefinition?): AshValue =
        if (monster == null) AshValue.of(-1L) else AshValue.of(monster.meatDrop.toLong())

    regFn(scope, "meat_drop", AshType.INT, emptyList()) { _, _ ->
        meatDropValue(lastMonster())
    }

    regFn(scope, "meat_drop", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        meatDropValue(resolveMonsterDefinition(args[0].toString()))
    }

    regFn(scope, "item_drops", itemFloatType, emptyList()) { _, _ ->
        buildItemDrops(lastMonster(), itemFloatType)
    }

    regFn(scope, "item_drops", itemFloatType, listOf("monster" to AshType.MONSTER)) { _, args ->
        buildItemDrops(resolveMonsterDefinition(args[0].toString()), itemFloatType)
    }

    regFn(scope, "item_drops_array", itemDropArrayType, emptyList()) { _, _ ->
        buildItemDropsArray(lastMonster(), itemDropArrayType)
    }

    regFn(
        scope,
        "item_drops_array",
        itemDropArrayType,
        listOf("monster" to AshType.MONSTER),
    ) { _, args ->
        buildItemDropsArray(resolveMonsterDefinition(args[0].toString()), itemDropArrayType)
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
        result[AshValue.item(drop.itemName)] = AshValue.of(drop.dropRate.toDouble())
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
    rec.setField(1, AshValue.of(drop.dropRate.toDouble()))
    // Desktop leaves type as empty string when DropFlag.NONE (null prefix).
    val flag = drop.prefix?.toString().orEmpty()
    if (flag.isNotEmpty()) {
        rec.setField(2, AshValue.of(flag))
    }
    return rec
}
