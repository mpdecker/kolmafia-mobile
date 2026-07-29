package net.sourceforge.kolmafia.inventory

/**
 * Desktop [FlaggedItems] `itemflags.txt` import/export format.
 */
data class ItemFlagsSections(
    val junk: List<String> = emptyList(),
    val singleton: List<String> = emptyList(),
    val memento: List<String> = emptyList(),
    val profitable: List<String> = emptyList(),
)

object ItemFlagsParser {

    fun parse(text: String, nameExists: (String) -> Boolean): ItemFlagsSections {
        val junk = mutableListOf<String>()
        val singleton = mutableListOf<String>()
        val memento = mutableListOf<String>()
        val profitable = mutableListOf<String>()
        var current: MutableList<String>? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith(">") || line.startsWith(" >")) {
                val sectionName = line.substringAfter('>').trim().lowercase()
                current = when {
                    sectionName.endsWith("junk") -> junk
                    sectionName.endsWith("singleton") -> singleton
                    sectionName.endsWith("mementos") -> memento
                    sectionName.endsWith("profitable") -> profitable
                    else -> null
                }
                continue
            }

            val itemName = parseItemName(line)
            if (current != null && nameExists(itemName) && itemName !in current) {
                current.add(itemName)
            }
        }

        for (name in singleton) {
            if (name !in junk) {
                junk.add(name)
            }
        }

        return ItemFlagsSections(junk, singleton, memento, profitable)
    }

    fun export(
        junk: List<String>,
        singleton: Set<String>,
        memento: List<String>,
        profitable: List<Pair<String, Int>>,
    ): String = buildString {
        appendLine(" > junk")
        appendLine()
        for (name in junk) {
            if (name !in singleton) {
                appendLine(name)
            }
        }
        appendLine()
        appendLine(" > singleton")
        appendLine()
        for (name in singleton) {
            appendLine(name)
        }
        appendLine()
        appendLine(" > mementos")
        appendLine()
        for (name in memento) {
            appendLine(name)
        }
        appendLine()
        appendLine(" > profitable")
        appendLine()
        for ((name, count) in profitable) {
            appendLine("$count $name")
        }
    }

    internal fun parseItemName(line: String): String {
        val trimmed = line.trim()
        val spaceIdx = trimmed.indexOf(' ')
        if (spaceIdx > 0) {
            val prefix = trimmed.substring(0, spaceIdx)
            if (prefix.toIntOrNull() != null) {
                return trimmed.substring(spaceIdx + 1).trim()
            }
        }
        return trimmed
    }
}
