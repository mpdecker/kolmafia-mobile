package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.request.HermitRequest

/** Desktop [ConditionalStatement.test] for CLI `if`/`while`/`elseif`. */
internal object CliConditional {

    private val STAT_DAY = Regex("^(today|tomorrow) is (.*?) day$", RegexOption.IGNORE_CASE)
    val FLOW_CONTROL_COMMANDS = mutableSetOf("if", "while", "else", "elseif", "try")
    val FULL_LINE_COMMANDS = setOf("cheapest", "expensive", "get", "set", "alias")

    fun isFlowControl(command: String): Boolean =
        FLOW_CONTROL_COMMANDS.contains(command.lowercase().removeSuffix("?"))

    fun isFullLine(command: String): Boolean =
        FULL_LINE_COMMANDS.contains(command.lowercase().removeSuffix("?"))

    fun test(parameters: String, lib: GameRuntimeLibrary, rt: AshRuntimeContext): Boolean {
        if (!MaximizerContinuation.permitsContinue()) return false
        val params = parameters.trim()
        if (params.isEmpty()) {
            rt.print("No condition specified.")
            return false
        }

        val dayMatch = STAT_DAY.find(params)
        if (dayMatch != null) {
            val whenWord = dayMatch.groupValues[1].lowercase()
            val statDayTest = dayMatch.groupValues[2].take(3).lowercase()
            val statDayToday = KolGameHolidayCalendar.getMoonEffect().lowercase()
            return statDayToday.contains(statDayTest) &&
                statDayToday.contains("bonus") &&
                !statDayToday.contains("not $whenWord")
        }

        val lower = params.lowercase()
        if (lower.startsWith("class is not ")) {
            val className = params.substring(13).trim().lowercase()
            val actual = lib.character?.state?.value?.className.orEmpty().lowercase()
            return !actual.contains(className)
        }
        if (lower.startsWith("class is ")) {
            val className = params.substring(9).trim().lowercase()
            val actual = lib.character?.state?.value?.className.orEmpty().lowercase()
            return actual.contains(className)
        }
        if (lower.startsWith("skill list lacks ")) {
            return !lib.hasCliSkill(params.substring(17).trim())
        }
        if (lower.startsWith("skill list contains ")) {
            return lib.hasCliSkill(params.substring(20).trim())
        }

        val operator = when {
            params.contains("==") -> "=="
            params.contains("!=") -> "!="
            params.contains(">=") -> ">="
            params.contains("<=") -> "<="
            params.contains("=") -> "=="
            params.contains("<>") -> "!="
            params.contains(">") -> ">"
            params.contains("<") -> "<"
            else -> null
        }
        if (operator == null) {
            rt.print("$params contains no comparison operator.")
            return false
        }

        val tokens = params.split(Regex("[!<>=]"))
        val left = tokens.first().trim()
        val right = tokens.last().trim()
        val leftValue: Long
        val rightValue: Long
        try {
            leftValue = lvalue(left, lib)
            rightValue = rvalue(left, right, lib, rt).toLong()
        } catch (_: Exception) {
            rt.print("$params is not a valid construct.")
            return false
        }
        return when (operator) {
            "==" -> leftValue == rightValue
            "!=" -> leftValue != rightValue
            ">=" -> leftValue >= rightValue
            ">" -> leftValue > rightValue
            "<=" -> leftValue <= rightValue
            "<" -> leftValue < rightValue
            else -> false
        }
    }

    private fun lvalue(left: String, lib: GameRuntimeLibrary): Long {
        if (left.toLongOrNull() != null) return left.toLong()
        val cs = lib.character?.state?.value
        return when (left.lowercase()) {
            "level" -> cs?.level?.toLong() ?: 0L
            "health" -> cs?.currentHp?.toLong() ?: 0L
            "mana" -> cs?.currentMp?.toLong() ?: 0L
            "meat" -> cs?.meat?.toLong() ?: 0L
            "adventures" -> cs?.adventuresLeft?.toLong() ?: 0L
            "inebriety", "drunkenness", "drunkness" -> cs?.inebriety?.toLong() ?: 0L
            "muscle" -> cs?.baseMusc?.toLong() ?: 0L
            "mysticality" -> cs?.baseMyst?.toLong() ?: 0L
            "moxie" -> cs?.baseMoxie?.toLong() ?: 0L
            "worthless item" -> {
                val inv = lib.inventoryManager?.state?.value?.items.orEmpty()
                    .mapValues { it.value.quantity }
                HermitRequest.worthlessCountFromMaps(inv, emptyMap(), emptyMap()).toLong()
            }
            "stickers" -> EquipmentSlot.STICKER_SLOTS.count { slot ->
                val name = cs?.equipment?.get(slot).orEmpty()
                name.isNotBlank() && !name.equals("none", ignoreCase = true)
            }.toLong()
            else -> itemOrEffectCount(left, lib)
        }
    }

    private fun rvalue(
        left: String,
        rightRaw: String,
        lib: GameRuntimeLibrary,
        rt: AshRuntimeContext,
    ): Int {
        var right = rightRaw
        if (right.endsWith("%")) {
            right = right.dropLast(1)
            val value = right.toIntOrNull() ?: 0
            val cs = lib.character?.state?.value
            return when (left.lowercase()) {
                "health" -> (value.toFloat() * (cs?.maxHp ?: 0).toFloat() / 100.0f).toInt()
                "mana" -> (value.toFloat() * (cs?.maxMp ?: 0).toFloat() / 100.0f).toInt()
                else -> value
            }
        }
        right.forEachIndexed { i, ch ->
            if (!ch.isDigit()) {
                resolveItem(right, lib)?.let { return itemQuantity(it, lib) }
                resolveEffect(right, lib)?.let { return effectTurns(it, lib) }
                if (i == 0 && ch == '-') return@forEachIndexed
                rt.print("Invalid operand [$right] on right side of operator")
                throw IllegalArgumentException(right)
            }
        }
        return right.toIntOrNull() ?: 0
    }

    private fun itemOrEffectCount(name: String, lib: GameRuntimeLibrary): Long {
        val item = resolveItem(name, lib)
        val effect = resolveEffect(name, lib)
        if (item != null && effect == null) return itemQuantity(item, lib).toLong()
        if (item == null && effect != null) return effectTurns(effect, lib).toLong()
        val lower = name.lowercase()
        if (item != null && item.lowercase().contains(lower)) {
            return itemQuantity(item, lib).toLong()
        }
        if (effect != null && effect.lowercase().contains(lower)) {
            return effectTurns(effect, lib).toLong()
        }
        if (item != null) return itemQuantity(item, lib).toLong()
        if (effect != null) return effectTurns(effect, lib).toLong()
        return 0L
    }

    private fun resolveItem(name: String, lib: GameRuntimeLibrary): String? {
        ItemDatabase.getByName(name)?.name?.let { return it }
        val lower = name.lowercase()
        ItemDatabase.all().firstOrNull { it.name.lowercase().contains(lower) }?.name?.let { return it }
        lib.inventoryManager?.state?.value?.items?.values
            ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.name?.let { return it }
        lib.inventoryManager?.state?.value?.items?.values
            ?.firstOrNull { it.name.lowercase().contains(lower) }
            ?.name?.let { return it }
        return null
    }

    private fun resolveEffect(name: String, lib: GameRuntimeLibrary): String? {
        EffectDatabase.getByName(name)?.name?.let { return it }
        val lower = name.lowercase()
        EffectDatabase.all().firstOrNull { it.name.lowercase().contains(lower) }?.name?.let { return it }
        lib.effectManager?.state?.value?.effects
            ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.name?.let { return it }
        lib.effectManager?.state?.value?.effects
            ?.firstOrNull { it.name.lowercase().contains(lower) }
            ?.name?.let { return it }
        return null
    }

    private fun itemQuantity(name: String, lib: GameRuntimeLibrary): Int {
        val items = lib.inventoryManager?.state?.value?.items ?: return 0
        val exact = items.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (exact != null) return exact.quantity
        val byId = ItemDatabase.getByName(name)?.id
        if (byId != null) return items[byId]?.quantity ?: 0
        return 0
    }

    private fun effectTurns(name: String, lib: GameRuntimeLibrary): Int {
        val effects = lib.effectManager?.state?.value?.effects.orEmpty()
        return effects.firstOrNull { it.name.equals(name, ignoreCase = true) }?.duration ?: 0
    }
}

internal fun GameRuntimeLibrary.hasCliSkill(name: String): Boolean {
    val lower = name.lowercase()
    return skillManager?.state?.value?.skills?.any {
        it.name.equals(name, ignoreCase = true) || it.name.lowercase().contains(lower)
    } == true
}

internal fun GameRuntimeLibrary.cliElseInvalid() {
    elseValid = false
}

internal fun GameRuntimeLibrary.cliSetElseRuns(shouldRun: Boolean) {
    elseShouldRun = shouldRun
    elseValid = true
}

internal fun GameRuntimeLibrary.cliConsumeElseRuns(rt: AshRuntimeContext): Boolean {
    if (!elseValid) {
        rt.print("'else' must follow a conditional command, and both must be at the outermost level.")
        return false
    }
    elseValid = false
    return elseShouldRun
}

internal fun GameRuntimeLibrary.runIfCli(parameters: String, continuation: String, rt: AshRuntimeContext) {
    if (CliConditional.test(parameters, this, rt)) {
        cliElseInvalid()
        dispatchCli(continuation, rt)
        cliSetElseRuns(false)
    } else {
        cliSetElseRuns(true)
    }
}

internal fun GameRuntimeLibrary.runWhileCli(parameters: String, continuation: String, rt: AshRuntimeContext) {
    val body = continuation
    cliSetElseRuns(true)
    while (CliConditional.test(parameters, this, rt) && MaximizerContinuation.permitsContinue()) {
        cliElseInvalid()
        dispatchCli(body, rt)
        cliSetElseRuns(false)
    }
}

internal fun GameRuntimeLibrary.runElseCli(parameters: String, continuation: String, rt: AshRuntimeContext) {
    if (parameters.isNotEmpty()) {
        rt.print("Condition not allowed for else.")
        return
    }
    if (cliConsumeElseRuns(rt)) {
        dispatchCli(continuation, rt)
    }
}

internal fun GameRuntimeLibrary.runElseIfCli(parameters: String, continuation: String, rt: AshRuntimeContext) {
    if (!cliConsumeElseRuns(rt)) {
        cliSetElseRuns(false)
    } else if (CliConditional.test(parameters, this, rt)) {
        dispatchCli(continuation, rt)
        cliSetElseRuns(false)
    } else {
        cliSetElseRuns(true)
    }
}

internal fun GameRuntimeLibrary.runTryCli(parameters: String, continuation: String, rt: AshRuntimeContext) {
    if (parameters.isNotEmpty()) {
        rt.print("Condition not allowed for try.")
        return
    }
    cliElseInvalid()
    try {
        dispatchCli(continuation, rt)
    } catch (_: ScriptException) {
        // Desktop abort sets error state; mobile abort throws. Swallow so else can recover.
    }
    if (MaximizerContinuation.permitsContinue()) {
        cliSetElseRuns(false)
    } else {
        MaximizerContinuation.forceContinue()
        cliSetElseRuns(true)
    }
}

internal fun GameRuntimeLibrary.flowContinuation(rest: String): String? {
    if (rest.isBlank()) return null
    var line = rest.trim()
    var seenCmd = false
    var needAnotherCmd = false
    while (line.isNotEmpty()) {
        val splitIndex = line.indexOf(';')
        val piece = if (splitIndex == -1) {
            val p = line
            line = ""
            p
        } else {
            val p = line.substring(0, splitIndex)
            line = line.substring(splitIndex + 1).trim()
            p
        }
        var command = piece.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        if (command.isEmpty()) continue
        if (command.endsWith("?")) command = command.dropLast(1)
        seenCmd = true
        needAnotherCmd = CliConditional.isFlowControl(command)
    }
    return if (seenCmd && !needAnotherCmd) rest.trim() else null
}
