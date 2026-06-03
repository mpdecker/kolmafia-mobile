package net.sourceforge.kolmafia.ash

data class ScriptState(
    val scripts: List<ScriptEntry> = emptyList(),
    val runningScript: String? = null,    // name of the currently executing script, or null
    val output: String = "",
    val error: String? = null
)
