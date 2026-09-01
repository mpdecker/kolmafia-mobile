package net.sourceforge.kolmafia.session

/**
 * Desktop ConditionsCommand.extractCondition — parses condition strings for the condition CLI.
 */
object GoalConditionParser {

    data class ParsedCondition(
        val kind: Kind,
        val count: Int = 1,
        val text: String? = null,
        val itemName: String? = null,
        val choiceId: Int? = null,
    ) {
        enum class Kind {
            MEAT, LEVEL, CHOICE_ADVENTURES, CHOICE_ID, FACTOID_TEXT, FACTOID_COUNT,
            LEPRECONDO, FLOUNDRY, AUTOSTOP, SUBSTATS, ITEM_NAME, ITEM_ID, REMOVE,
        }
    }

    fun parse(raw: String): ParsedCondition? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()

        if (MEAT_PATTERN.matches(lower)) {
            val amount = lower.substringBefore(" meat").replace(",", "").toLongOrNull() ?: return null
            return ParsedCondition(ParsedCondition.Kind.MEAT, count = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }

        if (lower.endsWith("choiceadv") || lower.endsWith("choices") || lower.endsWith("choice")) {
            val parts = lower.split(Regex("\\s+"))
            val count = if (parts.size > 1) parts[0].toIntOrNull() ?: 1 else 1
            return ParsedCondition(ParsedCondition.Kind.CHOICE_ADVENTURES, count = count.coerceAtLeast(1))
        }

        if (lower.endsWith("manuel") || lower.endsWith("factoid") || lower.endsWith("factoids")) {
            val parts = lower.split(Regex("\\s+"))
            val count = if (parts.size > 1) parts[0].toIntOrNull() ?: 1 else 1
            return ParsedCondition(ParsedCondition.Kind.FACTOID_COUNT, count = count.coerceAtLeast(1))
        }

        if (lower.endsWith("leprecondo furniture")) {
            val parts = lower.split(Regex("\\s+"))
            val count = if (parts.size > 2) parts[0].toIntOrNull() ?: 1 else 1
            return ParsedCondition(ParsedCondition.Kind.LEPRECONDO, count = count.coerceAtLeast(1))
        }

        if (lower.endsWith("floundry fish")) {
            val parts = lower.split(Regex("\\s+"))
            val count = if (parts.size > 2) parts[0].toIntOrNull() ?: 1 else 1
            return ParsedCondition(ParsedCondition.Kind.FLOUNDRY, count = count.coerceAtLeast(1))
        }

        if (lower.endsWith("autostop")) {
            val parts = lower.split(Regex("\\s+"))
            val count = if (parts.size > 1) parts[0].toIntOrNull() ?: 1 else 1
            return ParsedCondition(ParsedCondition.Kind.AUTOSTOP, count = count.coerceAtLeast(1))
        }

        if (lower == "substats") {
            return ParsedCondition(ParsedCondition.Kind.SUBSTATS)
        }

        if (lower.startsWith("meat ")) {
            val n = trimmed.substringAfter(' ').trim().replace(",", "").toIntOrNull() ?: return null
            return ParsedCondition(ParsedCondition.Kind.MEAT, count = n)
        }

        if (lower.startsWith("level ")) {
            val n = trimmed.substringAfter(' ').trim().toIntOrNull() ?: return null
            return ParsedCondition(ParsedCondition.Kind.LEVEL, count = n)
        }

        if (lower.startsWith("choice ")) {
            val token = trimmed.substringAfter(' ').trim()
            val asId = token.toIntOrNull()
            return if (asId != null) {
                ParsedCondition(ParsedCondition.Kind.CHOICE_ID, choiceId = asId)
            } else {
                parse(token)
            }
        }

        if (lower.startsWith("factoid ")) {
            val text = trimmed.substringAfter(' ').trim()
            if (text.isEmpty()) return null
            return ParsedCondition(ParsedCondition.Kind.FACTOID_TEXT, text = text)
        }

        val itemRest = trimmed.removePrefix("item ").removePrefix("Item ").trim()
        if (itemRest.isNotEmpty() && itemRest != trimmed) {
            return parseItemName(itemRest)
        }

        return parseItemName(trimmed)
    }

    fun splitConditions(parameters: String): List<String> =
        parameters.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun parseItemName(raw: String): ParsedCondition? {
        val parts = raw.trim().split(Regex("\\s+"), limit = 2)
        val first = parts[0]
        val count = first.toIntOrNull()
        return if (count != null && parts.size > 1) {
            ParsedCondition(ParsedCondition.Kind.ITEM_NAME, count = count.coerceAtLeast(1), itemName = parts[1])
        } else {
            ParsedCondition(ParsedCondition.Kind.ITEM_NAME, count = 1, itemName = raw.trim())
        }
    }

    private val MEAT_PATTERN = Regex("""^[\d,]+\s+meat$""")
}
