package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

class ScriptHookRunner(
    private val scriptManager: ScriptManager,
    private val preferences: Preferences,
) {
    fun onTurnConsumed() {
        if (!preferences.getBoolean(Preferences.AUTO_SCRIPTING, false)) return
        val auto = scriptManager.activeAutoscript() ?: return
        scriptManager.runScriptSync(auto.name)
    }
}
