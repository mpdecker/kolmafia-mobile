package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.session.WandDiscovery

internal fun GameRuntimeLibrary.registerZapWandFunctions(scope: AshScope) {
    regFn(scope, "get_zap_wand", AshType.ITEM, emptyList()) { _, _ ->
        val ascensionNumber = character?.state?.value?.ascensionNumber ?: 0
        val wandId = WandDiscovery.findWand(inventoryManager, preferences, ascensionNumber)
            ?: return@regFn AshValue.item("")
        val name = gameDatabase?.item(wandId)?.name
            ?: ItemDatabase.getById(wandId)?.name
            ?: wandId.toString()
        AshValue.item(name)
    }
}

internal fun GameRuntimeLibrary.registerZapActionFunctions(scope: AshScope) {
    regFn(scope, "zap", AshType.ITEM, listOf("item" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = gameDatabase?.item(itemName)?.id
            ?: ItemDatabase.getByName(itemName)?.id
            ?: return@regFn AshValue.item("")
        val request = zapRequest ?: return@regFn AshValue.item("")
        val acquiredId = kotlinx.coroutines.runBlocking {
            request.zap(itemId).getOrNull() ?: -1
        }
        if (acquiredId <= 0) {
            return@regFn AshValue.item("")
        }
        val acquiredName = gameDatabase?.item(acquiredId)?.name
            ?: ItemDatabase.getById(acquiredId)?.name
            ?: return@regFn AshValue.item("")
        AshValue.item(acquiredName)
    }
}
