package net.sourceforge.kolmafia.equipment

/** Desktop `OUTFIT_ACTION_PATTERN` — embedded CLI/familiar/mood actions in custom outfit names. */
object EmbeddedOutfitActions {
    private val ACTION_PATTERN = Regex("""([a-zA-Z])=([^=]+)(?!=)""")

    fun displayName(outfitName: String): String {
        var result = outfitName
        var previous: String
        do {
            previous = result
            result = ACTION_PATTERN.replace(result) { "" }
        } while (result != previous)
        return result.trim()
    }

    fun runAfterWear(outfitName: String, execute: (command: String) -> Unit) {
        for (match in ACTION_PATTERN.findAll(outfitName)) {
            val key = match.groupValues[1].lowercase().firstOrNull() ?: continue
            val text = match.groupValues[2].trim()
            when (key) {
                'c' -> execute(text)
                'e' -> execute("equip familiar $text")
                'f' -> execute("familiar $text")
                'm' -> execute("mood $text")
                't' -> execute("enthrone $text")
                'b' -> execute("bjornify $text")
            }
        }
    }
}
