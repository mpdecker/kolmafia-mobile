package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.UserDataFileIO

/**
 * Phases 6471–6490 — ASH behavioral deepen XLIII Tracks E–F.
 *
 * Track E (6471–6480): mall_price / historical_age day-gate, retrieve_item count≤0,
 * npc_price validate, daily_special resolve.
 * Track F (6481–6490): modifier MODIFIER entity leftovers, SimpleXPath text()/following-sibling,
 * file_to_map compact arity, session_logs daily files, form_fields polish.
 *
 * REVISION is phase6490 (parent mega wrap-up).
 */
internal fun GameRuntimeLibrary.registerPhase6490(scope: AshScope) {
    registerPhase6490ModifierEdges(scope)
}

/**
 * Register `numeric_modifier` / `boolean_modifier` / `string_modifier` overloads that take
 * typed entity + [AshType.MODIFIER] for leftover entity classes (Phase 3710 only covered
 * ITEM/EFFECT/SKILL/FAMILIAR). Desktop registers at least THRALL + MODIFIER.
 */
private fun GameRuntimeLibrary.registerPhase6490ModifierEdges(scope: AshScope) {
    data class EntitySpec(
        val type: AshType,
        val param: String,
        val resolve: GameRuntimeLibrary.(String) -> net.sourceforge.kolmafia.data.ModifierEntry?,
    )

    val entities = listOf(
        EntitySpec(AshType.THRALL, "thr") { ref ->
            gameDatabase?.thrallModifier(ref)
                ?: net.sourceforge.kolmafia.data.ModifierDatabase.get("Thrall", ref)
        },
        EntitySpec(AshType.LOCATION, "loc") { ref ->
            resolveLocationQueryName(ref)?.let { gameDatabase?.locationModifier(it) }
                ?: net.sourceforge.kolmafia.data.ModifierDatabase.getLocation(ref)
        },
        EntitySpec(AshType.PATH, "path") { ref ->
            gameDatabase?.pathModifier(ref)
                ?: net.sourceforge.kolmafia.data.ModifierDatabase.get("Path", ref)
        },
        EntitySpec(AshType.STAT, "stat") { ref ->
            val name = net.sourceforge.kolmafia.modifiers.StatNames.resolve(ref).orEmpty()
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Stat", name)
        },
        EntitySpec(AshType.ELEMENT, "value") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Element", ref)
        },
        EntitySpec(AshType.CLASS, "class") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Class", ref)
        },
        EntitySpec(AshType.SERVANT, "servant") { ref ->
            val name = net.sourceforge.kolmafia.modifiers.ServantData.resolve(ref)?.type ?: ref
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Servant", name)
        },
        EntitySpec(AshType.VYKEA, "vykea") { ref -> resolveVykeaModifierEntry(ref) },
        EntitySpec(AshType.COINMASTER, "cm") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Coinmaster", ref)
        },
        EntitySpec(AshType.BOUNTY, "bounty") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Bounty", ref)
        },
        EntitySpec(AshType.SLOT, "slot") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Slot", ref)
        },
        EntitySpec(AshType.PHYLUM, "phylum") { ref ->
            net.sourceforge.kolmafia.data.ModifierDatabase.get("Phylum", ref)
        },
    )

    for (spec in entities) {
        val resolve = spec.resolve
        // ELEMENT numeric_modifier(STRING) already live-routes resistance; MODIFIER overload
        // uses static Element rows when present, else resistance fallback for known tags.
        if (spec.type == AshType.ELEMENT) {
            regFn(scope, "numeric_modifier", AshType.FLOAT,
                listOf(spec.param to spec.type, "modifier" to AshType.MODIFIER)) { _, args ->
                val tag = args[1].toString()
                val resistance = elementalResistanceModifier(args[0].toString())
                if (resistance != null &&
                    (tag.equals(resistance.tag, ignoreCase = true) ||
                        tag.contains("Resistance", ignoreCase = true))
                ) {
                    return@regFn AshValue.of(buildCurrentModifiers().values.get(resistance))
                }
                numericStatic6490(resolve(args[0].toString()), tag)
            }
        } else {
            regFn(scope, "numeric_modifier", AshType.FLOAT,
                listOf(spec.param to spec.type, "modifier" to AshType.MODIFIER)) { _, args ->
                numericStatic6490(resolve(args[0].toString()), args[1].toString())
            }
        }
        regFn(scope, "boolean_modifier", AshType.BOOLEAN,
            listOf(spec.param to spec.type, "modifier" to AshType.MODIFIER)) { _, args ->
            AshValue.of(booleanFromEntry(resolve(args[0].toString()), args[1].toString()))
        }
        regFn(scope, "string_modifier", AshType.STRING,
            listOf(spec.param to spec.type, "modifier" to AshType.MODIFIER)) { _, args ->
            AshValue.of(stringFromEntry(resolve(args[0].toString()), args[1].toString()))
        }
    }
}

private fun numericStatic6490(
    entry: net.sourceforge.kolmafia.data.ModifierEntry?,
    tag: String,
): AshValue = AshValue.of(numericFromEntry(entry, tag))

/** Shared session-log file reader used by 1-/2-/3-arg `session_logs` overloads. */
internal fun GameRuntimeLibrary.sessionLogsForDays(player: String, dayCount: Int): AggregateValue {
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    if (dayCount < 0) {
        throw ScriptException("Can't get session logs for a negative number of days")
    }
    if (dayCount < 1) return AggregateValue(stringArray)
    val result = AggregateValue(stringArray)
    val safe = player.replace(' ', '_').ifBlank { "unknown" }
    val dayMillis = 86_400_000L
    val now = currentTimeMillis()
    for (i in 0 until dayCount) {
        val stamp = formatAshDateTime("yyyyMMdd", now - i * dayMillis, null)
        val contents = readSessionLogDay(safe, stamp)
        result[AshValue.of(i.toLong())] = AshValue.of(contents)
    }
    return result
}

internal fun readSessionLogDay(playerSafe: String, yyyymmdd: String): String {
    val base = "sessions/${playerSafe}_$yyyymmdd"
    UserDataFileIO.readText("$base.txt")?.let { return it }
    // Headless platforms may not gunzip; accept pre-extracted .txt.gz text if present.
    UserDataFileIO.readText("$base.txt.gz")?.let { return it }
    return ""
}
