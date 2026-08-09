package net.sourceforge.kolmafia.mood

/** Desktop [EditMoodCommand] comma parameter parsing. */
object EditMoodCommandParser {

    /**
     * Parses `[type,] effect [, action]`.
     * Invalid/missing [type] defaults to `lose_effect`.
     */
    fun parseParameters(parameters: String): Triple<String, String, String?>? {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) return null

        var start = 0
        var end = trimmed.indexOf(',')
        if (end == -1) return null

        var type = trimmed.substring(start, end).trim()
        if (!type.equals("lose_effect", ignoreCase = true) &&
            !type.equals("gain_effect", ignoreCase = true) &&
            !type.equals("unconditional", ignoreCase = true)
        ) {
            type = "lose_effect"
            end = -1
        }

        start = end + 1
        end = trimmed.indexOf(',', start)
        val name = if (end != -1) {
            trimmed.substring(start, end).trim()
        } else {
            trimmed.substring(start).trim()
        }
        val action = if (end != -1) trimmed.substring(end + 1).trim() else null

        if (type.equals("unconditional", ignoreCase = true)) {
            return Triple(type, "", action)
        }
        if (name.isBlank()) return null
        return Triple(type, name, action)
    }
}
