package net.sourceforge.kolmafia.ash

import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.maximizer.MaximizerEquipScope
import net.sourceforge.kolmafia.maximizer.MaximizerFilters

internal fun GameRuntimeLibrary.registerScriptFunctions(scope: AshScope) {
    regFn(scope, "runscript", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { ctx, args ->
        AshValue.of(runSavedScript(args[0].toString(), ctx))
    }

    regFn(scope, "sync_quests", AshType.BOOLEAN, emptyList()) { _, _ ->
        val req = questLogRequest ?: return@regFn AshValue.of(false)
        kotlinx.coroutines.runBlocking { req.syncAll() }
        AshValue.of(true)
    }

    regFn(scope, "maximize", AshType.BOOLEAN, emptyList()) { _, _ ->
        val mgr = maximizerManager ?: return@regFn AshValue.of(false)
        val result = kotlinx.coroutines.runBlocking { mgr.maximize("all") }
        AshValue.of(result.success)
    }

    regFn(scope, "maximize", AshType.BOOLEAN, listOf("goal" to AshType.STRING)) { _, args ->
        val mgr = maximizerManager ?: return@regFn AshValue.of(false)
        val result = runBlocking { mgr.maximize(args[0].toString()) }
        AshValue.of(result.success)
    }

    regFn(
        scope,
        "maximize",
        AshType.BOOLEAN,
        listOf(
            "goal" to AshType.STRING,
            "maxPrice" to AshType.INT,
            "priceLevel" to AshType.INT,
        ),
    ) { _, args ->
        val mgr = maximizerManager ?: return@regFn AshValue.of(false)
        val result = runBlocking {
            mgr.maximize(args[0].toString(), MaximizerFilters.allEnabled())
        }
        AshValue.of(result.success)
    }

    regFn(
        scope,
        "maximize",
        AshType.BOOLEAN,
        listOf(
            "goal" to AshType.STRING,
            "maxPrice" to AshType.INT,
            "priceLevel" to AshType.INT,
            "equipScope" to AshType.INT,
            "filters" to AshType.STRING,
        ),
    ) { _, args ->
        val mgr = maximizerManager ?: return@regFn AshValue.of(false)
        val filters = MaximizerFilters.parseFromString(args[4].toString())
        if (filters.isEmpty()) return@regFn AshValue.of(false)
        val goal = args[0].toString()
        val equipScope = MaximizerEquipScope.byIndex(args[3].toLong().toInt())
        val success = runBlocking {
            when (equipScope) {
                MaximizerEquipScope.EQUIP_NOW -> mgr.maximize(goal, filters).success
                MaximizerEquipScope.SPECULATE -> {
                    val lines = mgr.speculate(goal, filters)
                    lines.none {
                        it.startsWith("No improvement", ignoreCase = true) ||
                            it.startsWith("Invalid goal", ignoreCase = true)
                    }
                }
            }
        }
        AshValue.of(success)
    }
}

internal fun GameRuntimeLibrary.runCallScriptCli(parameters: String, rt: AshRuntimeContext) {
    var params = parameters.trim()
    if (params.isEmpty()) return
    var runCount = 1
    val firstSpace = params.indexOf(' ')
    val firstToken = if (firstSpace < 0) params else params.substring(0, firstSpace)
    if (firstToken.length > 1 &&
        firstToken.endsWith("x", ignoreCase = true) &&
        firstToken.dropLast(1).all { it.isDigit() }
    ) {
        runCount = firstToken.dropLast(1).toIntOrNull() ?: 0
        if (runCount <= 0) return
        if (firstSpace < 0) return
        params = params.substring(firstSpace + 1).trim()
        if (params.isEmpty()) return
    }
    repeat(runCount) {
        if (!runSavedScript(params, rt)) {
            rt.print("Script '$params' not found")
            return
        }
    }
}

internal fun GameRuntimeLibrary.runSavedScript(
    name: String,
    outputContext: AshRuntimeContext? = null,
): Boolean {
    val json = preferences?.getString(ScriptManager.SCRIPTS_PREF_KEY, "[]") ?: return false
    val scripts = try {
        Json.decodeFromString<List<ScriptEntry>>(json)
    } catch (_: Exception) {
        emptyList()
    }
    val entry = scripts.find { it.name.equals(name, ignoreCase = true) } ?: return false
    val runtime = if (outputContext is AshRuntime) outputContext else AshRuntime(this)
    val nodes = AshParser().parse(entry.source)
    try {
        runtime.execute(nodes)
    } catch (e: ScriptException) {
        outputContext?.print("Script error: ${e.message}")
        return false
    }
    val out = runtime.output.toString()
    if (outputContext != null && out.isNotEmpty()) {
        out.lines().filter { it.isNotEmpty() }.forEach { outputContext.print(it) }
    }
    return true
}
