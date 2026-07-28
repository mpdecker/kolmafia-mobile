package net.sourceforge.kolmafia.ash

/**
 * ASH-P129 behavioral batch — VYKEA concoction pricing overload.
 */
internal fun GameRuntimeLibrary.registerAshP129Batch(scope: AshScope) {
    regFn(scope, "concoction_price", AshType.INT, listOf("vykea" to AshType.VYKEA)) { _, args ->
        AshValue.of(concoctionPriceForVykea(args[0].toString()))
    }
}
