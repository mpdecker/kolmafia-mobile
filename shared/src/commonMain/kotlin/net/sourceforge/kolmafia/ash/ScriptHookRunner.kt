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

    private fun runPrefScript(prefKey: String) {
        val raw = preferences.getString(prefKey, "").trim()
        if (raw.isEmpty()) return
        scriptManager.runScriptSync(normalizeScriptName(raw))
    }

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
