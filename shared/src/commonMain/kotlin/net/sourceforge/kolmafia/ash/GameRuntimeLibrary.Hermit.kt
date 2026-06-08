package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerHermit(scope: AshScope) {

    // helper: resolve item name → game ID (Int), null if unknown
    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // hermit(it: item, n: int) → int
    // Trades [n] of [it] with the hermit. Returns count on success, 0 on failure.
    // Note: hermit() uses item-first argument order (hermit(item, count)) matching
    // desktop KoLmafia's ASH API, unlike mobile's other item functions which are (qty, item).
    regFn(scope, "hermit", AshType.INT,
        listOf("it" to AshType.ITEM, "n" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(0L)
        val count  = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val req = hermitRequest ?: return@regFn AshValue.of(0L)
        val success = kotlinx.coroutines.runBlocking { req.trade(itemId, count) }.isSuccess
        AshValue.of(if (success) count.toLong() else 0L)
    }
}
