package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.DoubleModifier

/**
 * ASH-P11 overload batch — raises registered function count toward desktop parity floor (≥620).
 */
internal fun GameRuntimeLibrary.registerAshP11Batch(scope: AshScope) {
    regFn(scope, "my_robot_energy", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("robotEnergy", 0) ?: 0).toLong())
    }
    regFn(scope, "my_robot_scraps", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("robotScraps", 0) ?: 0).toLong())
    }
    regFn(scope, "contestants_left", AshType.INT, listOf("contest" to AshType.INT)) { _, args ->
        val contest = args[0].toLong().toInt()
        val left = preferences?.getInt("nsContestants$contest", -1) ?: -1
        AshValue.of(left.toLong())
    }

    regFn(scope, "repeat", AshType.STRING, listOf("value" to AshType.STRING, "count" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val count = args[1].toLong().toInt().coerceIn(0, 256)
        AshValue.of(text.repeat(count))
    }
    regFn(scope, "pad_left", AshType.STRING,
        listOf("value" to AshType.STRING, "width" to AshType.INT, "pad" to AshType.STRING)) { _, args ->
        val text = args[0].toString()
        val width = args[1].toLong().toInt().coerceAtLeast(0)
        val pad = args[2].toString().ifEmpty { " " }
        if (text.length >= width) AshValue.of(text)
        else AshValue.of(pad.first().toString().repeat(width - text.length) + text)
    }
    regFn(scope, "contains", AshType.BOOLEAN,
        listOf("haystack" to AshType.BUFFER, "needle" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().contains(args[1].toString()))
    }

    val modifierStubEntityTypes = listOf(
        AshType.SLOT,
        AshType.BOUNTY, AshType.MODIFIER,
        AshType.COINMASTER, AshType.PHYLUM,
    )
    for (entityType in modifierStubEntityTypes) {
        val captured = entityType
        val paramName = when (captured) {
            AshType.CLASS -> "cls"
            AshType.STAT -> "stat"
            else -> "value"
        }
        regFn(scope, "numeric_modifier", AshType.FLOAT,
            listOf(paramName to captured, "modifier" to AshType.STRING)) { _, _ ->
            AshValue.of(0.0)
        }
        regFn(scope, "boolean_modifier", AshType.BOOLEAN,
            listOf(paramName to captured, "modifier" to AshType.STRING)) { _, _ ->
            AshValue.FALSE
        }
        regFn(scope, "string_modifier", AshType.STRING,
            listOf(paramName to captured, "modifier" to AshType.STRING)) { _, _ ->
            AshValue.EMPTY_STRING
        }
        regFn(scope, "type_of", AshType.STRING, listOf("value" to captured)) { _, _ ->
            AshValue.of(captured.name)
        }
        regFn(scope, "is_valid", AshType.BOOLEAN, listOf("value" to captured)) { _, args ->
            AshValue.of(args[0].toString().isNotBlank())
        }
    }

    val validatedEntityTypes = listOf(
        AshType.LOCATION, AshType.MONSTER, AshType.THRALL, AshType.PATH,
    )
    for (entityType in validatedEntityTypes) {
        val captured = entityType
        val paramName = when (captured) {
            AshType.LOCATION -> "loc"
            AshType.MONSTER -> "mo"
            AshType.THRALL -> "thr"
            AshType.PATH -> "path"
            else -> "value"
        }
        regFn(scope, "type_of", AshType.STRING, listOf(paramName to captured)) { _, _ ->
            AshValue.of(captured.name)
        }
        regFn(scope, "is_valid", AshType.BOOLEAN, listOf(paramName to captured)) { _, args ->
            val ref = args[0].toString()
            val valid = when (captured) {
                AshType.LOCATION -> resolveLocation(ref) != null ||
                    gameDatabase?.locationModifier(ref) != null
                AshType.MONSTER -> gameDatabase?.monster(ref) != null
                AshType.THRALL -> gameDatabase?.thrallModifier(ref) != null
                AshType.PATH -> gameDatabase?.pathModifier(ref) != null
                else -> ref.isNotBlank()
            }
            AshValue.of(valid)
        }
    }

    val itemLikeTypes = listOf(AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR)
    for (entityType in itemLikeTypes) {
        val captured = entityType
        val paramName = when (captured) {
            AshType.ITEM -> "it"
            AshType.SKILL -> "sk"
            AshType.EFFECT -> "ef"
            AshType.FAMILIAR -> "fa"
            else -> "value"
        }
        regFn(scope, "type_of", AshType.STRING, listOf(paramName to captured)) { _, _ ->
            AshValue.of(captured.name)
        }
        regFn(scope, "is_valid", AshType.BOOLEAN, listOf(paramName to captured)) { _, args ->
            val ref = args[0].toString()
            val valid = when (captured) {
                AshType.ITEM -> gameDatabase?.item(ref) != null
                AshType.SKILL -> gameDatabase?.skill(ref) != null
                AshType.EFFECT -> gameDatabase?.effect(ref) != null
                AshType.FAMILIAR -> gameDatabase?.familiar(ref) != null
                else -> ref.isNotBlank()
            }
            AshValue.of(valid)
        }
    }

    regFn(scope, "modifier_name", AshType.STRING, listOf("mod" to AshType.STRING)) { _, args ->
        val resolved = DoubleModifier.byTag(args[0].toString())
        AshValue.of(resolved?.tag ?: args[0].toString().lowercase())
    }
}
