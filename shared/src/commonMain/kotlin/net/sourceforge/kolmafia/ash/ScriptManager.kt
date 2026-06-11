package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class ScriptManager(
    private val library: GameRuntimeLibrary,
    private val preferences: Preferences,
    private val eventBus: GameEventBus
) {
    companion object {
        const val SCRIPTS_PREF_KEY = "ashScripts"
    }

    private val _state = MutableStateFlow(ScriptState())
    val state: StateFlow<ScriptState> = _state.asStateFlow()

    private var runJob: Job? = null

    fun initialize() {
        loadScripts()
    }

    private fun loadScripts() {
        val json = preferences.getString(SCRIPTS_PREF_KEY, "[]")
        val scripts = try {
            Json.decodeFromString<List<ScriptEntry>>(json)
        } catch (_: Exception) {
            emptyList()
        }
        _state.value = _state.value.copy(scripts = scripts)
    }

    private fun persistScripts(scripts: List<ScriptEntry>) {
        preferences.setString(SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        _state.value = _state.value.copy(scripts = scripts)
    }

    fun saveScript(entry: ScriptEntry) {
        val updated = _state.value.scripts.toMutableList()
        val existing = updated.indexOfFirst { it.name == entry.name }
        if (existing >= 0) updated[existing] = entry else updated.add(entry)
        persistScripts(updated)
    }

    fun deleteScript(name: String) {
        persistScripts(_state.value.scripts.filter { it.name != name })
    }

    fun runScript(name: String, scope: CoroutineScope) {
        val entry = _state.value.scripts.find { it.name == name }
            ?: run { _state.value = _state.value.copy(error = "Script '$name' not found"); return }
        runJob?.cancel()
        _state.value = _state.value.copy(runningScript = name, output = "", error = null)
        eventBus.tryEmit(GameEvent.ScriptStarted(name))

        runJob = scope.launch {
            try {
                val runtime = AshRuntime(library)
                val nodes = AshParser().parse(entry.source)
                runtime.execute(nodes)
                val out = runtime.output.toString()
                // Emit each output line to the event bus before persisting
                out.lines().filter { it.isNotEmpty() }.forEach { line ->
                    eventBus.tryEmit(GameEvent.ScriptOutput(line))
                }
                // Update lastRunAt
                val updatedScripts = _state.value.scripts.map {
                    if (it.name == name) it.copy(lastRunAt = currentTimeMillis()) else it
                }
                persistScripts(updatedScripts)
                _state.value = _state.value.copy(output = out, runningScript = null, error = null)
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = true, error = null))
            } catch (e: ScriptException) {
                _state.value = _state.value.copy(runningScript = null, error = e.message)
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = false, error = e.message))
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                _state.value = _state.value.copy(runningScript = null, error = msg)
                eventBus.tryEmit(GameEvent.ScriptFinished(name, success = false, error = msg))
            }
        }
    }

    fun stopScript() {
        runJob?.cancel()
        runJob = null
        _state.value = _state.value.copy(runningScript = null)
    }
}
