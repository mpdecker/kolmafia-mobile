package net.sourceforge.kolmafia.ash

class ScriptException(message: String, val line: Int = -1) : Exception(
    if (line > 0) "Script error at line $line: $message" else "Script error: $message"
)
