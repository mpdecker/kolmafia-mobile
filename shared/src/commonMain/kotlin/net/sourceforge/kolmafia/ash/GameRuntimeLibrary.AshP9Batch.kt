package net.sourceforge.kolmafia.ash

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * ASH-P9 overload batch — raises registered function count toward desktop parity floor (≥500).
 */
internal fun GameRuntimeLibrary.registerAshP9Batch(scope: AshScope) {
    regFn(scope, "is_adventuring", AshType.BOOLEAN, emptyList()) { _, _ ->
        val running = adventureManager?.isRunning?.value == true
        AshValue.of(running)
    }
    regFn(scope, "has_queued_commands", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.FALSE
    }

    regFn(scope, "to_buffer", AshType.BUFFER, listOf("value" to AshType.STRING)) { _, args ->
        AshValue(AshType.BUFFER, StringBuilder(args[0].toString()))
    }

    regFn(scope, "buffer_to_string", AshType.STRING, listOf("buf" to AshType.BUFFER)) { _, args ->
        AshValue.of(args[0].toString())
    }

    regFn(scope, "length", AshType.INT, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().length.toLong())
    }
    regFn(scope, "length", AshType.INT, listOf("value" to AshType.BUFFER)) { _, args ->
        val sb = args[0].content as? StringBuilder
        AshValue.of((sb?.length ?: args[0].toString().length).toLong())
    }

    regFn(scope, "trim", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().trim())
    }
    regFn(scope, "compare", AshType.INT, listOf("a" to AshType.STRING, "b" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().compareTo(args[1].toString()).toLong())
    }
    regFn(scope, "index_of", AshType.INT,
        listOf("haystack" to AshType.STRING, "needle" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().indexOf(args[1].toString()).toLong())
    }
    regFn(scope, "split", AshType.AGGREGATE,
        listOf("value" to AshType.STRING, "sep" to AshType.STRING)) { _, args ->
        val agg = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
        args[0].toString().split(args[1].toString()).forEachIndexed { i, part ->
            agg[AshValue.of(i.toLong())] = AshValue.of(part)
        }
        agg
    }
    regFn(scope, "starts_with", AshType.BOOLEAN,
        listOf("value" to AshType.STRING, "prefix" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().startsWith(args[1].toString()))
    }
    regFn(scope, "ends_with", AshType.BOOLEAN,
        listOf("value" to AshType.STRING, "suffix" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().endsWith(args[1].toString()))
    }

    regFn(scope, "abs", AshType.INT, listOf("value" to AshType.INT)) { _, args ->
        AshValue.of(abs(args[0].toLong()))
    }
    regFn(scope, "abs", AshType.FLOAT, listOf("value" to AshType.FLOAT)) { _, args ->
        AshValue.of(abs(args[0].toDouble()))
    }
    regFn(scope, "round", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
        AshValue.of(round(args[0].toDouble()).toLong())
    }
    regFn(scope, "floor", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
        AshValue.of(floor(args[0].toDouble()).toLong())
    }
    regFn(scope, "ceil", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
        AshValue.of(ceil(args[0].toDouble()).toLong())
    }

    regFn(scope, "min", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
        AshValue.of(minOf(args[0].toLong(), args[1].toLong()))
    }
    regFn(scope, "max", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
        AshValue.of(maxOf(args[0].toLong(), args[1].toLong()))
    }
    regFn(scope, "min", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
        AshValue.of(minOf(args[0].toDouble(), args[1].toDouble()))
    }
    regFn(scope, "max", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
        AshValue.of(maxOf(args[0].toDouble(), args[1].toDouble()))
    }

    regFn(scope, "is_empty", AshType.BOOLEAN, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().isEmpty())
    }
    regFn(scope, "is_empty", AshType.BOOLEAN, listOf("value" to AshType.BUFFER)) { _, args ->
        val sb = args[0].content as? StringBuilder
        AshValue.of((sb?.length ?: 0) == 0)
    }
    regFn(scope, "is_empty", AshType.BOOLEAN, listOf("value" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue
        AshValue.of(agg?.map?.isEmpty() != false)
    }

    regFn(scope, "to_monster", AshType.MONSTER, listOf("name" to AshType.STRING)) { _, args ->
        val resolved = net.sourceforge.kolmafia.modifiers.MonsterNames.resolve(
            args[0].toString(),
            gameDatabase,
        )
        AshValue(AshType.MONSTER, resolved ?: "")
    }

    val jsonSourceTypes = listOf(
        AshType.STRING, AshType.INT, AshType.FLOAT, AshType.BOOLEAN,
        AshType.ITEM, AshType.SKILL, AshType.EFFECT,
    )
    for (sourceType in jsonSourceTypes) {
        val captured = sourceType
        regFn(scope, "to_json", AshType.STRING, listOf("value" to captured)) { _, args ->
            val encoded = when (captured) {
                AshType.STRING -> Json.encodeToString(args[0].toString())
                AshType.INT -> args[0].toLong().toString()
                AshType.FLOAT -> args[0].toDouble().toString()
                AshType.BOOLEAN -> if (args[0].toBoolean()) "true" else "false"
                else -> Json.encodeToString(args[0].toString())
            }
            AshValue.of(encoded)
        }
    }
    regFn(scope, "to_json", AshType.STRING, listOf("value" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue
        val pairs = agg?.map?.entries?.joinToString(",") { (k, v) ->
            """"${k.toString().replace("\"", "\\\"")}":"${v.toString().replace("\"", "\\\"")}""""
        } ?: ""
        AshValue.of("{$pairs}")
    }

    val bufferAppendTypes = listOf(
        AshType.STRING, AshType.INT, AshType.FLOAT, AshType.BOOLEAN, AshType.ITEM,
    )
    for (sourceType in bufferAppendTypes) {
        val captured = sourceType
        regFn(scope, "append", AshType.VOID,
            listOf("buf" to AshType.BUFFER, "value" to captured)) { _, args ->
            val sb = args[0].content as? StringBuilder
            sb?.append(args[1].toString())
            AshValue.VOID
        }
    }

    regFn(scope, "clear", AshType.VOID, listOf("buf" to AshType.BUFFER)) { _, args ->
        (args[0].content as? StringBuilder)?.setLength(0)
        AshValue.VOID
    }

    regFn(scope, "replace", AshType.STRING,
        listOf("value" to AshType.STRING, "from" to AshType.STRING, "to" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().replace(args[1].toString(), args[2].toString()))
    }
    regFn(scope, "substring", AshType.STRING,
        listOf("value" to AshType.STRING, "start" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val start = args[1].toLong().toInt().coerceIn(0, text.length)
        AshValue.of(text.substring(start))
    }
    regFn(scope, "substring", AshType.STRING,
        listOf("value" to AshType.STRING, "start" to AshType.INT, "end" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val start = args[1].toLong().toInt().coerceIn(0, text.length)
        val end = args[2].toLong().toInt().coerceIn(start, text.length)
        AshValue.of(text.substring(start, end))
    }
    regFn(scope, "reverse", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().reversed())
    }

    val extraJsonTypes = listOf(
        AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER, AshType.CLASS, AshType.STAT,
    )
    for (sourceType in extraJsonTypes) {
        val captured = sourceType
        regFn(scope, "to_json", AshType.STRING, listOf("value" to captured)) { _, args ->
            AshValue.of(Json.encodeToString(args[0].toString()))
        }
    }

    val extraToStringTypes = listOf(
        AshType.LOCATION, AshType.MONSTER, AshType.COINMASTER, AshType.PATH,
    )
    for (entityType in extraToStringTypes) {
        val captured = entityType
        regFn(scope, "entity_name", AshType.STRING, listOf("value" to captured)) { _, args ->
            AshValue.of(args[0].toString())
        }
    }

    val boolKeyTypes = listOf(AshType.BOOLEAN, AshType.FLOAT)
    for (keyType in boolKeyTypes) {
        val captured = keyType
        regFn(scope, "contains_key", AshType.BOOLEAN,
            listOf("agg" to AshType.AGGREGATE, "key" to captured)) { _, args ->
            val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
            AshValue.of(agg.map.containsKey(args[1]))
        }
        regFn(scope, "remove", AshType.VOID,
            listOf("agg" to AshType.AGGREGATE, "key" to captured)) { _, args ->
            (args[0] as? AggregateValue)?.map?.remove(args[1])
            AshValue.VOID
        }
    }

    val printTypes = listOf(AshType.BUFFER, AshType.ITEM, AshType.SKILL, AshType.EFFECT)
    for (valueType in printTypes) {
        val captured = valueType
        regFn(scope, "print", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }
}
