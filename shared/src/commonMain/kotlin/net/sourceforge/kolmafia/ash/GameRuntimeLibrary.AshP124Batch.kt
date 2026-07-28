package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.item.CreatableTurns

/**
 * ASH-P124 behavioral batch — creatable_turns craft adventure estimates.
 */
internal fun GameRuntimeLibrary.registerAshP124Batch(scope: AshScope) {
    regFn(scope, "creatable_turns", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AshValue.ZERO
        AshValue.of(creatableTurnsFor(itemId, 1))
    }

    regFn(scope, "creatable_turns", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        AshValue.of(creatableTurnsFor(itemId, 1))
    }

    regFn(scope, "creatable_turns", AshType.INT, listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AshValue.ZERO
        val count = args[1].toLong().toInt()
        AshValue.of(creatableTurnsFor(itemId, count))
    }

    regFn(scope, "creatable_turns", AshType.INT, listOf("id" to AshType.INT, "count" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        val count = args[1].toLong().toInt()
        AshValue.of(creatableTurnsFor(itemId, count))
    }
}
