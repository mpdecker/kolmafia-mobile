package net.sourceforge.kolmafia.ash

/**
 * ASH-P19 behavioral batch — live entity modifier lookups via ModifierDatabase.
 */
internal fun GameRuntimeLibrary.registerAshP19Batch(scope: AshScope) {
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("loc" to AshType.LOCATION, "modifier" to AshType.STRING)) { _, args ->
        val name = resolveLocationQueryName(args[0].toString())
        val entry = name?.let { gameDatabase?.locationModifier(it) }
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("path" to AshType.PATH, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.pathModifier(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("thr" to AshType.THRALL, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.thrallModifier(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("loc" to AshType.LOCATION, "modifier" to AshType.STRING)) { _, args ->
        val name = resolveLocationQueryName(args[0].toString())
        val entry = name?.let { gameDatabase?.locationModifier(it) }
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("path" to AshType.PATH, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.pathModifier(args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("thr" to AshType.THRALL, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.thrallModifier(args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "string_modifier", AshType.STRING,
        listOf("loc" to AshType.LOCATION, "modifier" to AshType.STRING)) { _, args ->
        val name = resolveLocationQueryName(args[0].toString())
        val entry = name?.let { gameDatabase?.locationModifier(it) }
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "string_modifier", AshType.STRING,
        listOf("path" to AshType.PATH, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.pathModifier(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }

    regFn(scope, "string_modifier", AshType.STRING,
        listOf("thr" to AshType.THRALL, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.thrallModifier(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
}
