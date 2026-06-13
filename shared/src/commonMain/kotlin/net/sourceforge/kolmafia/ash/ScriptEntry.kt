package net.sourceforge.kolmafia.ash

import kotlinx.serialization.Serializable

@Serializable
data class ScriptEntry(
    val name: String,
    val source: String,
    val lastRunAt: Long = 0L,   // epoch millis; 0 = never run
    val type: ScriptType = ScriptType.NORMAL,
)
