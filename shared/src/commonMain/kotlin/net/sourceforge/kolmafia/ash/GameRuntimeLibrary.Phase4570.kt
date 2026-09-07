package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.modifiers.SlotNames
import net.sourceforge.kolmafia.platform.UserDataFileIO

/**
 * Phases 4511–4570 — ASH behavioral deepen XI + HTTP request residual glue.
 *
 * 4511 visit_url BUFFER/0-arg/usePostMethod · 4512 make_url 3-arg ·
 * 4513 load_html local html · 4514 get_path_full/variables ·
 * 4515 get_stack_trace · 4516 enable/disable · 4517 dump ·
 * 4518 abort() · 4519 cli_execute_output(string) · 4520 has_queued_commands live ·
 * 4521–4523 adventure/adv1 filter + swapped args · 4524 unequip ·
 * 4525 auto_equip · 4526 council HTTP · 4527 session_logs extra ·
 * 4528 print_html log flag · 4529 get_counters already live · 4530–4570 request hubs
 */
internal fun GameRuntimeLibrary.registerPhase4570(scope: AshScope) {
    val stackTraceRec = STACK_TRACE_REC
    val stackArray = AggregateType(AshType.INT, stackTraceRec)

    regFn(scope, "get_stack_trace", stackArray, emptyList()) { rt, _ ->
        val frames = (rt as? AshRuntime)?.getCallFrames().orEmpty()
        val result = AggregateValue(stackArray)
        // Skip the topmost get_stack_trace frame (desktop parity).
        val visible = frames.dropLast(1).asReversed()
        visible.forEachIndexed { i, frame ->
            val rec = RecordValue(stackTraceRec)
            rec.setField(0, AshValue.of(frame.fileName))
            rec.setField(1, AshValue.of(frame.name))
            rec.setField(2, AshValue.of(frame.lineNumber.toLong()))
            result[AshValue.of(i)] = rec
        }
        result
    }

    regFn(scope, "get_path_full", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(lastVisitPath)
    }
    regFn(scope, "get_path_variables", AshType.STRING, emptyList()) { _, _ ->
        val q = lastVisitPath.indexOf('?')
        AshValue.of(if (q < 0) "" else lastVisitPath.substring(q))
    }

    regFn(scope, "enable", AshType.VOID, listOf("name" to AshType.STRING)) { _, args ->
        disabledFeatures.remove(args[0].toString().lowercase())
        AshValue.VOID
    }
    regFn(scope, "disable", AshType.VOID, listOf("name" to AshType.STRING)) { _, args ->
        disabledFeatures.add(args[0].toString().lowercase())
        AshValue.VOID
    }

    fun dumpValue(runtime: AshRuntimeContext, value: AshValue, indent: String, color: String) {
        when (value) {
            is AggregateValue -> {
                for ((key, child) in value.map) {
                    val line = "$indent$key => $child"
                    if (color.isBlank()) runtime.print(line) else runtime.print(line)
                    dumpValue(runtime, child, "$indent  ", color)
                }
            }
            is RecordValue -> {
                runtime.print("$indent$value")
            }
            else -> runtime.print("$indent$value")
        }
    }

    regFn(scope, "dump", AshType.VOID, listOf("arg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "dump", AshType.VOID, listOf("arg" to AshType.INT)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "dump", AshType.VOID, listOf("arg" to AshType.BUFFER)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "dump", AshType.VOID, listOf("arg" to AshType.AGGREGATE)) { runtime, args ->
        runtime.print(args[0].toString())
        dumpValue(runtime, args[0], "", "")
        AshValue.VOID
    }
    regFn(scope, "dump", AshType.VOID,
        listOf("arg" to AshType.STRING, "color" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "dump", AshType.VOID,
        listOf("arg" to AshType.AGGREGATE, "color" to AshType.STRING)) { runtime, args ->
        // Desktop dump(arg, color) recurses CompositeValue keys; color is a print hint.
        dumpValue(runtime, args[0], "", args[1].toString())
        AshValue.VOID
    }

    regFn(scope, "cli_execute_output", AshType.STRING, listOf("cmd" to AshType.STRING)) { runtime, args ->
        lastCliOutput.clear()
        val capturing = GameRuntimeLibrary.CliCapturingContext(runtime, lastCliOutput)
        dispatchCli(args[0].toString(), capturing)
        AshValue.of(lastCliOutput.toString().trimEnd())
    }

    regFn(scope, "print_html", AshType.VOID,
        listOf("html" to AshType.STRING, "logToSession" to AshType.BOOLEAN)) { runtime, args ->
        val stripped = args[0].toString()
            .replace(Regex("<[^>]+>"), "")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        runtime.print(stripped)
        if (args[1].toBoolean()) {
            sessionLogger?.appendRawLine(stripped)
        }
        AshValue.VOID
    }

    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    fun sessionLogArray(lines: List<String>): AggregateValue {
        val result = AggregateValue(stringArray)
        lines.forEachIndexed { i, line -> result[AshValue.of(i.toLong())] = AshValue.of(line) }
        return result
    }

    regFn(scope, "session_logs", stringArray,
        listOf("player" to AshType.STRING, "days" to AshType.INT)) { _, args ->
        val days = args[1].toLong().toInt()
        val fromLogger = sessionLogger?.recentLines(days.coerceAtLeast(1)).orEmpty()
        if (fromLogger.isNotEmpty()) return@regFn sessionLogArray(fromLogger)
        sessionLogArray(readSessionLogFiles(args[0].toString(), days))
    }
    regFn(scope, "session_logs", stringArray,
        listOf("player" to AshType.STRING, "baseDate" to AshType.STRING, "count" to AshType.INT)) { _, args ->
        sessionLogArray(readSessionLogFiles(args[0].toString(), args[2].toLong().toInt(), args[1].toString()))
    }

    // Desktop bare unequip → unequip all
    regFn(scope, "unequip", AshType.BOOLEAN, emptyList()) { _, _ ->
        val ok = runBlocking { equipmentRequest?.unequipAll()?.isSuccess ?: false }
        AshValue.of(ok)
    }
    regFn(scope, "unequip", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        if (itemName.equals("none", ignoreCase = true) || itemName.isBlank()) {
            val ok = runBlocking { equipmentRequest?.unequipAll()?.isSuccess ?: false }
            return@regFn AshValue.of(ok)
        }
        val item = gameDatabase?.item(itemName) ?: return@regFn AshValue.FALSE
        val slot = equipmentManager?.findSlot(item.id) ?: return@regFn AshValue.FALSE
        val ok = runBlocking { equipmentRequest?.unequipSlot(slot)?.isSuccess ?: false }
        AshValue.of(ok)
    }
    regFn(scope, "unequip", AshType.BOOLEAN, listOf("slot" to AshType.SLOT)) { _, args ->
        val slot = SlotNames.toEquipmentSlot(args[0].toString()) ?: return@regFn AshValue.FALSE
        val ok = runBlocking { equipmentRequest?.unequipSlot(slot)?.isSuccess ?: false }
        AshValue.of(ok)
    }

    regFn(scope, "auto_equip", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val item = gameDatabase?.item(name) ?: ItemDatabase.getByName(name) ?: return@regFn AshValue.FALSE
        val mgr = equipmentManager
        if (mgr != null) {
            mgr.autoequipItem(item.id, swapInventory = true)
            return@regFn AshValue.TRUE
        }
        val slot = SlotNames.toEquipmentSlot(
            when (item.primaryUse) {
                net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT -> "hat"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON -> "weapon"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND -> "off-hand"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.SHIRT -> "shirt"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.PANTS -> "pants"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.ACCESSORY -> "acc1"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.CONTAINER -> "container"
                net.sourceforge.kolmafia.data.ItemPrimaryUse.FAMILIAR -> "familiar"
                else -> return@regFn AshValue.FALSE
            },
        ) ?: return@regFn AshValue.FALSE
        val ok = runBlocking { equipmentRequest?.equipItem(item.id, slot)?.isSuccess ?: false }
        AshValue.of(ok)
    }
}

internal val STACK_TRACE_REC = RecordType(
    "stack_trace_record",
    listOf(
        RecordField("file", AshType.STRING, 0),
        RecordField("name", AshType.STRING, 1),
        RecordField("line", AshType.INT, 2),
    ),
)

private fun GameRuntimeLibrary.readSessionLogFiles(
    player: String,
    count: Int,
    baseDate: String? = null,
): List<String> {
    val safe = player.replace(' ', '_')
    val n = kotlin.math.abs(count).coerceAtLeast(0)
    if (n == 0 && baseDate == null) return emptyList()
    val lines = mutableListOf<String>()
    if (!baseDate.isNullOrBlank()) {
        val name = "${safe}_$baseDate.txt"
        UserDataFileIO.readText("sessions/$name")?.let { lines += it }
        return lines
    }
    sessionLogger?.recentLines(n.coerceAtLeast(1))?.let { lines += it }
    return lines
}
