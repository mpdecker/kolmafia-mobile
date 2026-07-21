package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.PocketDatabase.PocketType

/**
 * AshP83 — cargo cult pocket introspection ASH.
 * Mirrors desktop [RuntimeLibrary] meat/poem/scrap/joke/restoration lists,
 * pocket_* content readers, and potential_pockets.
 */
internal fun GameRuntimeLibrary.registerAshP83Batch(scope: AshScope) {
    val pocketSetType = AggregateType(AshType.INT, AshType.BOOLEAN)
    val pocketListType = AggregateType(AshType.INT, AshType.INT)
    val pocketEffectsType = AggregateType(AshType.EFFECT, AshType.INT)
    val pocketItemsType = AggregateType(AshType.ITEM, AshType.INT)
    val pocketStatsType = AggregateType(AshType.STAT, AshType.INT)
    val indexedTextType = AggregateType(AshType.INT, AshType.STRING)

    regFn(scope, "meat_pockets", pocketListType, emptyList()) { _, _ ->
        buildPocketList(PocketDatabase.meatPockets, pocketListType)
    }

    regFn(scope, "poem_pockets", pocketListType, emptyList()) { _, _ ->
        buildPocketList(PocketDatabase.poemHalfLines, pocketListType)
    }

    regFn(scope, "scrap_pockets", pocketListType, emptyList()) { _, _ ->
        buildPocketList(PocketDatabase.scrapSyllables, pocketListType)
    }

    regFn(scope, "joke_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.getPockets(PocketType.JOKE)?.keys.orEmpty(), pocketSetType)
    }

    regFn(scope, "restoration_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(PocketDatabase.getPockets(PocketType.RESTORE)?.keys.orEmpty(), pocketSetType)
    }

    regFn(scope, "pocket_effects", pocketEffectsType, listOf("pocket" to AshType.INT)) { _, args ->
        buildPocketEffects(args[0].toLong().toInt(), pocketEffectsType)
    }

    regFn(scope, "pocket_items", pocketItemsType, listOf("pocket" to AshType.INT)) { _, args ->
        buildPocketItems(args[0].toLong().toInt(), pocketItemsType)
    }

    regFn(scope, "pocket_stats", pocketStatsType, listOf("pocket" to AshType.INT)) { _, args ->
        buildPocketStats(args[0].toLong().toInt(), pocketStatsType)
    }

    regFn(scope, "pocket_scrap", indexedTextType, listOf("pocket" to AshType.INT)) { _, args ->
        buildIndexedText(
            args[0].toLong().toInt(),
            indexedTextType,
            yegDemonNameSync?.knownScrapPockets().orEmpty(),
        )
    }

    regFn(scope, "pocket_poem", indexedTextType, listOf("pocket" to AshType.INT)) { _, args ->
        buildIndexedText(args[0].toLong().toInt(), indexedTextType, emptyMap())
    }

    regFn(scope, "pocket_meat", indexedTextType, listOf("pocket" to AshType.INT)) { _, args ->
        buildIndexedText(args[0].toLong().toInt(), indexedTextType, emptyMap())
    }

    regFn(scope, "pocket_joke", AshType.STRING, listOf("pocket" to AshType.INT)) { _, args ->
        pocketJokeText(args[0].toLong().toInt())
    }

    regFn(scope, "potential_pockets", pocketListType, listOf("monster" to AshType.MONSTER)) { _, args ->
        buildPocketList(sortedMonsterPockets(args[0].toString()), pocketListType)
    }

    regFn(scope, "potential_pockets", pocketListType, listOf("effect" to AshType.EFFECT)) { _, args ->
        buildPocketList(sortedEffectPockets(args[0].toString()), pocketListType)
    }

    regFn(scope, "potential_pockets", pocketListType, listOf("item" to AshType.ITEM)) { _, args ->
        buildPocketList(sortedItemPockets(args[0].toString()), pocketListType)
    }

    regFn(scope, "potential_pockets", pocketListType, listOf("stat" to AshType.STAT)) { _, args ->
        buildPocketList(sortedStatPockets(args[0].toString()), pocketListType)
    }
}
