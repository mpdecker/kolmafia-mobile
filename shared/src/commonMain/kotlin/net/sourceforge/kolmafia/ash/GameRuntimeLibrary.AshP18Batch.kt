package net.sourceforge.kolmafia.ash

/**
 * ASH-P18 behavioral batch — telescope upgrade helper.
 */
internal fun GameRuntimeLibrary.registerAshP18Batch(scope: AshScope) {
    regFn(scope, "telescope_upgrades", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("telescopeUpgrades", 0) ?: 0).toLong())
    }
    regFn(scope, "telescope_looked_high", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("telescopeLookedHigh") == true)
    }
}
