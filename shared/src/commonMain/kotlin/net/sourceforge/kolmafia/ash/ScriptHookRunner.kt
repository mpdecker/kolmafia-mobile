package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

class ScriptHookRunner(
    private val scriptManager: ScriptManager,
    private val preferences: Preferences,
) {
    /** Desktop [KoLmafia.executeScript] for [Preferences.BETWEEN_BATTLE_SCRIPT]. */
    fun onBetweenBattle() {
        runPrefScript(Preferences.BETWEEN_BATTLE_SCRIPT)
    }

    /**
     * Desktop after-adventure script + optional autoscript.
     * Pref [Preferences.AFTER_ADVENTURE_SCRIPT] runs first when set.
     */
    fun onTurnConsumed() {
        runPrefScript(Preferences.AFTER_ADVENTURE_SCRIPT)
        if (!preferences.getBoolean(Preferences.AUTO_SCRIPTING, false)) return
        val auto = scriptManager.activeAutoscript() ?: return
        scriptManager.runScriptSync(auto.name)
    }

    /** Desktop [LoginManager] `loginScript` after session initialize. */
    fun onLogin(): Boolean = runPrefScript(Preferences.LOGIN_SCRIPT)

    /** Desktop [KoLCharacter.liberateKing] `kingLiberatedScript`. */
    fun onKingLiberated(): Boolean = runPrefScript(Preferences.KING_LIBERATED_SCRIPT)

    /**
     * Desktop [PvpManager] `beforePVPScript` ASH path.
     * Returns true when a saved ASH script ran (caller should skip CLI).
     */
    fun onBeforePvp(): Boolean = runPrefScript(Preferences.BEFORE_PVP_SCRIPT)

    /**
     * Desktop [ChoiceManager.invokeChoiceAdventureScript].
     * Returns true when a saved script was found and executed.
     */
    fun onChoiceAdventure(choiceId: Int, responseText: String): Boolean {
        val spec = parsePrefScript(preferences.getString(Preferences.CHOICE_ADVENTURE_SCRIPT, ""))
            ?: return false
        val args = listOf(AshValue.of(choiceId.toLong()), AshValue.of(responseText))
        return scriptManager.runScriptSync(
            name = spec.scriptName,
            functionName = spec.functionName,
            args = args,
            executeTopLevel = spec.executeTopLevel,
        ) != null
    }

    /**
     * Desktop [GenericRequest.invokeCounterScript].
     * Returns true when the script's return value is non-zero (handled).
     */
    fun onCounter(label: String, turnsRemaining: Int): Boolean {
        val spec = parsePrefScript(preferences.getString(Preferences.COUNTER_SCRIPT, ""))
            ?: return false
        val args = listOf(AshValue.of(label), AshValue.of(turnsRemaining.toString()))
        val result = scriptManager.runScriptSync(
            name = spec.scriptName,
            functionName = spec.functionName,
            args = args,
            executeTopLevel = spec.executeTopLevel,
        ) ?: return false
        return result.toBoolean() || result.toLong() != 0L
    }

    /**
     * Run a desktop executeLine-style pref: optional `function@script`, optional `call ` prefix.
     * Returns true when a saved ASH script ran.
     */
    fun runPrefScript(prefKey: String): Boolean {
        val spec = parsePrefScript(preferences.getString(prefKey, "")) ?: return false
        return scriptManager.runScriptSync(
            name = spec.scriptName,
            functionName = spec.functionName,
            args = emptyList(),
            executeTopLevel = spec.executeTopLevel,
        ) != null
    }

    private fun parsePrefScript(raw: String): PrefScript? {
        var text = raw.trim().trim('"')
        if (text.isEmpty()) return null
        if (text.startsWith("call ", ignoreCase = true)) {
            text = text.substring(5).trim()
        }
        var functionName = "main"
        val at = text.indexOf('@')
        if (at > 0) {
            functionName = text.substring(0, at).trim().ifEmpty { "main" }
            text = text.substring(at + 1).trim()
        }
        val scriptName = normalizeScriptName(text)
        if (scriptName.isEmpty()) return null
        return PrefScript(
            scriptName = scriptName,
            functionName = functionName,
            executeTopLevel = functionName.equals("main", ignoreCase = true),
        )
    }

    private data class PrefScript(
        val scriptName: String,
        val functionName: String,
        val executeTopLevel: Boolean,
    )

    companion object {
        /** Strip path + `.ash` so pref values match [ScriptEntry.name]. */
        fun normalizeScriptName(raw: String): String {
            var name = raw.trim().trim('"')
            val slash = maxOf(name.lastIndexOf('/'), name.lastIndexOf('\\'))
            if (slash >= 0 && slash < name.lastIndex) name = name.substring(slash + 1)
            if (name.endsWith(".ash", ignoreCase = true)) {
                name = name.dropLast(4)
            }
            return name
        }
    }
}
