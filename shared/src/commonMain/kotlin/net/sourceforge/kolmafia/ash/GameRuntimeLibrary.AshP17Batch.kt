package net.sourceforge.kolmafia.ash

/**
 * ASH-P17 behavioral batch — Naughty Sorceress challenge prefs and corpus helpers.
 */
internal fun GameRuntimeLibrary.registerAshP17Batch(scope: AshScope) {
    regFn(scope, "ns_challenge", AshType.STRING, listOf("challenge" to AshType.INT)) { _, args ->
        val index = args[0].toLong().toInt().coerceIn(1, 5)
        AshValue.of(preferences?.getString("nsChallenge$index", "none") ?: "none")
    }
    regFn(scope, "ns_challenge", AshType.STRING, listOf("challenge" to AshType.STRING)) { _, args ->
        val index = args[0].toString().toIntOrNull()?.coerceIn(1, 5) ?: 1
        AshValue.of(preferences?.getString("nsChallenge$index", "none") ?: "none")
    }
    regFn(scope, "telescope_line", AshType.STRING, listOf("line" to AshType.INT)) { _, args ->
        val index = args[0].toLong().toInt().coerceIn(1, 7)
        AshValue.of(preferences?.getString("telescope$index", "") ?: "")
    }
}
