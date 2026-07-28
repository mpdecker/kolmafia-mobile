package net.sourceforge.kolmafia.ash

/**
 * ASH-P125 behavioral batch — creatable_turns 3-arg with free-crafting credit.
 */
internal fun GameRuntimeLibrary.registerAshP125Batch(scope: AshScope) {
    regFn(
        scope,
        "creatable_turns",
        AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "freeCrafting" to AshType.INT),
    ) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AshValue.ZERO
        val count = args[1].toLong().toInt()
        val considerFree = args[2].toLong().toInt() == 1
        AshValue.of(creatableTurnsFor(itemId, count, considerFree))
    }

    regFn(
        scope,
        "creatable_turns",
        AshType.INT,
        listOf("id" to AshType.INT, "count" to AshType.INT, "freeCrafting" to AshType.INT),
    ) { _, args ->
        val itemId = args[0].toLong().toInt()
        val count = args[1].toLong().toInt()
        val considerFree = args[2].toLong().toInt() == 1
        AshValue.of(creatableTurnsFor(itemId, count, considerFree))
    }
}
