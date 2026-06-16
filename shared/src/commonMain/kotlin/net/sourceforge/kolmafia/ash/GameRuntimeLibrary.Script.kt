package net.sourceforge.kolmafia.ash

import kotlinx.serialization.json.Json

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
        val result = kotlinx.coroutines.runBlocking { mgr.maximize(args[0].toString()) }
        AshValue.of(result.success)
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
