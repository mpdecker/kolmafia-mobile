package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.PocketDatabase.JokePocket
import net.sourceforge.kolmafia.data.PocketDatabase.MeatPocket
import net.sourceforge.kolmafia.data.PocketDatabase.OneResultPocket
import net.sourceforge.kolmafia.data.PocketDatabase.Pocket
import net.sourceforge.kolmafia.data.PocketDatabase.PoemPocket
import net.sourceforge.kolmafia.data.PocketDatabase.ScrapPocket
import net.sourceforge.kolmafia.data.PocketDatabase.StatsPocket
import net.sourceforge.kolmafia.data.PocketDatabase.TwoResultPocket

internal fun sortedMonsterPockets(name: String): List<Pocket> {
    val pocket = PocketDatabase.monsterPockets[name.lowercase()] ?: return emptyList()
    return listOf(pocket)
}

internal fun sortedEffectPockets(name: String): List<Pocket> {
    val pockets = PocketDatabase.effectPockets[name] ?: return emptyList()
    return PocketDatabase.sortResults(name, pockets)
}

internal fun sortedItemPockets(name: String): List<Pocket> {
    val pockets = PocketDatabase.itemPockets[name] ?: return emptyList()
    return PocketDatabase.sortResults(name, pockets)
}

internal fun sortedStatPockets(name: String): List<Pocket> {
    val pockets = PocketDatabase.statsPockets[name.lowercase()] ?: return emptyList()
    return PocketDatabase.sortStats(name, pockets)
}

internal fun sortedPocketsForAshType(type: AshType, name: String): List<Pocket> = when (type) {
    AshType.MONSTER -> sortedMonsterPockets(name)
    AshType.EFFECT -> sortedEffectPockets(name)
    AshType.ITEM -> sortedItemPockets(name)
    AshType.STAT -> sortedStatPockets(name)
    else -> emptyList()
}

internal fun buildPocketSet(pockets: Collection<Int>, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    for (pocket in pockets.sorted()) {
        result[AshValue.of(pocket.toLong())] = AshValue.TRUE
    }
    return result
}

internal fun buildPocketList(pockets: List<Pocket>, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    pockets.forEachIndexed { index, pocket ->
        result[AshValue.of(index.toLong())] = AshValue.of(pocket.pocket.toLong())
    }
    return result
}

internal fun buildPocketEffects(pocketNum: Int, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    val pocket = PocketDatabase.pocketByNumber(pocketNum) ?: return result
    if (pocket.pocket !in PocketDatabase.allEffectPockets) return result
    when (pocket) {
        is OneResultPocket -> {
            result[AshValue(AshType.EFFECT, pocket.result1.name)] =
                AshValue.of(pocket.result1.duration.toLong())
        }
        is TwoResultPocket -> {
            result[AshValue(AshType.EFFECT, pocket.result1.name)] =
                AshValue.of(pocket.result1.duration.toLong())
            result[AshValue(AshType.EFFECT, pocket.result2.name)] =
                AshValue.of(pocket.result2.duration.toLong())
        }
        is JokePocket -> {
            result[AshValue(AshType.EFFECT, pocket.result1.name)] =
                AshValue.of(pocket.result1.duration.toLong())
        }
        else -> Unit
    }
    return result
}

internal fun buildPocketItems(pocketNum: Int, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    val pocket = PocketDatabase.pocketByNumber(pocketNum) ?: return result
    if (pocket.pocket !in PocketDatabase.allItemPockets) return result
    when (pocket) {
        is OneResultPocket -> {
            result[AshValue(AshType.ITEM, pocket.result1.name)] =
                AshValue.of(pocket.result1.duration.toLong())
        }
        is TwoResultPocket -> {
            result[AshValue(AshType.ITEM, pocket.result1.name)] =
                AshValue.of(pocket.result1.duration.toLong())
            result[AshValue(AshType.ITEM, pocket.result2.name)] =
                AshValue.of(pocket.result2.duration.toLong())
        }
        else -> Unit
    }
    return result
}

internal fun buildPocketStats(pocketNum: Int, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    val pocket = PocketDatabase.pocketByNumber(pocketNum) as? StatsPocket ?: return result
    if (pocket.pocket !in PocketDatabase.allStatsPockets) return result
    result[AshValue(AshType.STAT, "muscle")] = AshValue.of(pocket.muscle.toLong())
    result[AshValue(AshType.STAT, "mysticality")] = AshValue.of(pocket.mysticality.toLong())
    result[AshValue(AshType.STAT, "moxie")] = AshValue.of(pocket.moxie.toLong())
    return result
}

internal fun buildIndexedText(
    pocketNum: Int,
    type: AggregateType,
    knownScraps: Map<Int, String>,
): AggregateValue {
    val result = AggregateValue(type)
    when (val pocket = PocketDatabase.pocketByNumber(pocketNum)) {
        is ScrapPocket -> {
            val syllable = knownScraps[pocket.pocket].orEmpty()
            result[AshValue.of(pocket.scrap.toLong())] = AshValue.of(syllable)
        }
        is PoemPocket -> {
            result[AshValue.of(pocket.index.toLong())] = AshValue.of(pocket.text)
        }
        is MeatPocket -> {
            result[AshValue.of(pocket.meat.toLong())] = AshValue.of(pocket.text)
        }
        else -> Unit
    }
    return result
}

internal fun pocketJokeText(pocketNum: Int): AshValue {
    val pocket = PocketDatabase.pocketByNumber(pocketNum) as? JokePocket ?: return AshValue.EMPTY_STRING
    return AshValue.of(pocket.joke)
}
