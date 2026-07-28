package net.sourceforge.kolmafia.ash

/**
 * ASH-P18 behavioral batch — telescope upgrade helper.
 */
internal fun GameRuntimeLibrary.registerAshP18Batch(scope: AshScope) {
    regFn(scope, "telescope_upgrades", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(
            (character?.state?.value?.telescopeUpgrades
                ?: preferences?.getInt("telescopeUpgrades", 0)
                ?: 0).toLong(),
        )
    }
    regFn(scope, "telescope_looked_high", AshType.BOOLEAN, emptyList()) { _, _ ->
        val cs = character?.state?.value
        AshValue.of(
            cs?.telescopeLookedHigh == true ||
                preferences?.getBoolean("telescopeLookedHigh") == true,
        )
    }
}
