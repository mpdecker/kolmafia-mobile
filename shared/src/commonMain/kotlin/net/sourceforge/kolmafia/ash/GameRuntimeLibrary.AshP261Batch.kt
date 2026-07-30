package net.sourceforge.kolmafia.ash

/** AshP261 — Interactive ASH soft defaults (`user_confirm` / `user_prompt`; no native dialog UI). */
internal fun GameRuntimeLibrary.registerAshP261Batch(scope: AshScope) {
    regFn(scope, "user_confirm", AshType.BOOLEAN, listOf("message" to AshType.STRING)) { _, _ ->
        AshValue.of(true)
    }

    regFn(
        scope,
        "user_confirm",
        AshType.BOOLEAN,
        listOf(
            "message" to AshType.STRING,
            "timeOut" to AshType.INT,
            "defaultBoolean" to AshType.BOOLEAN,
        ),
    ) { _, args ->
        AshValue.of(args[2].toBoolean())
    }

    regFn(scope, "user_prompt", AshType.STRING, listOf("message" to AshType.STRING)) { _, _ ->
        AshValue.of("")
    }

    regFn(
        scope,
        "user_prompt",
        AshType.STRING,
        listOf("message" to AshType.STRING, "options" to AshType.AGGREGATE),
    ) { _, args ->
        AshValue.of(firstAggregateKey(args[1]))
    }

    regFn(
        scope,
        "user_prompt",
        AshType.STRING,
        listOf(
            "message" to AshType.STRING,
            "timeOut" to AshType.INT,
            "defaultString" to AshType.STRING,
        ),
    ) { _, args ->
        AshValue.of(args[2].toString())
    }
}

private fun firstAggregateKey(options: AshValue): String {
    val aggregate = options as? AggregateValue ?: return ""
    return aggregate.map.keys.firstOrNull()?.toString() ?: ""
}
