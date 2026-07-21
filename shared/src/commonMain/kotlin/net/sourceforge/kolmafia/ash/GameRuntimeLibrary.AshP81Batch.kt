package net.sourceforge.kolmafia.ash

/**
 * AshP81 — cargo cult picked-pocket ASH queries.
 * Mirrors desktop [RuntimeLibrary.picked_pockets] and [RuntimeLibrary.picked_scraps].
 */
internal fun GameRuntimeLibrary.registerAshP81Batch(scope: AshScope) {
    val pocketSetType = AggregateType(AshType.INT, AshType.BOOLEAN)

    regFn(scope, "picked_pockets", pocketSetType, emptyList()) { _, _ ->
        buildPocketSet(cargoPocketSync?.pickedPocketIds().orEmpty(), pocketSetType)
    }

    regFn(scope, "picked_scraps", pocketSetType, emptyList()) { _, _ ->
        val scrapKeys = yegDemonNameSync?.knownScrapPockets()?.keys.orEmpty()
        buildPocketSet(scrapKeys, pocketSetType)
    }
}
