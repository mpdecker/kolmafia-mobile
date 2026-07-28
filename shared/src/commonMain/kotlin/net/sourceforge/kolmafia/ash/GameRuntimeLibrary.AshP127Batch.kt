package net.sourceforge.kolmafia.ash

/**
 * ASH-P127 behavioral batch — validate=true shop probes wired into craft helpers.
 * Public is_npc_item/is_coinmaster_item remain validate=false per desktop.
 */
internal fun GameRuntimeLibrary.registerAshP127Batch(scope: AshScope) {
    regFn(scope, "npc_item_accessible", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        AshValue.of(npcItemAccessible(itemId))
    }

    regFn(scope, "coinmaster_item_accessible", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        AshValue.of(coinmasterItemAccessible(itemId))
    }
}

internal fun GameRuntimeLibrary.npcItemAccessible(itemId: Int): Boolean =
    net.sourceforge.kolmafia.data.NpcStoreDatabase.containsItem(
        itemId,
        validate = true,
        state = craftCharacterState(),
        prefs = preferences,
        accessibleCount = { id -> craftAccessibleCount(id) },
    )

internal fun GameRuntimeLibrary.coinmasterItemAccessible(itemId: Int): Boolean =
    net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuyItem(
        itemId,
        validate = true,
        state = craftCharacterState(),
        prefs = preferences,
        accessibleCount = { id -> craftAccessibleCount(id) },
    )
