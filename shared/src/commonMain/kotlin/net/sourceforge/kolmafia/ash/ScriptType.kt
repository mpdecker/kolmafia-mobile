package net.sourceforge.kolmafia.ash

import kotlinx.serialization.Serializable

@Serializable
enum class ScriptType {
    NORMAL,
    AUTOSCRIPT,
    COMBAT,
}
