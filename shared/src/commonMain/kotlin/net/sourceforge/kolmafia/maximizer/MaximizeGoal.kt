package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/** Parsed maximize goal: primary stat plus optional constraints. */
data class MaximizeSpec(
    val primary: DoubleModifier,
    val requiredBooleans: Set<BooleanModifier> = emptySet(),
    val forbiddenBooleans: Set<BooleanModifier> = emptySet(),
    val equipRequired: List<String> = emptyList(),
    val switchFamiliars: List<String> = emptyList(),
)

/** Parses desktop-style maximize goal strings into modifier tags and constraints. */
object MaximizeGoal {

    fun parseSpec(goal: String): MaximizeSpec? {
        val trimmed = goal.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.contains(',')) {
            val primary = parseSingleModifier(trimmed) ?: return null
            return MaximizeSpec(primary)
        }
        var primary: DoubleModifier? = null
        val required = mutableSetOf<BooleanModifier>()
        val forbidden = mutableSetOf<BooleanModifier>()
        val equip = mutableListOf<String>()
        val switches = mutableListOf<String>()
        for (term in splitTerms(trimmed)) {
            val t = term.trim()
            if (t.isBlank()) continue
            when {
                t.startsWith("equip ", ignoreCase = true) ->
                    equip.add(unquote(t.substring(6).trim()))
                t.startsWith("switch ", ignoreCase = true) ->
                    switches.add(unquote(t.substring(7).trim()))
                t.startsWith('-') -> {
                    val tag = unquote(t.drop(1).trim())
                    BooleanModifier.byTag(tag)?.let { forbidden.add(it) }
                }
                t.startsWith('+') -> {
                    val rest = unquote(t.drop(1).trim())
                    BooleanModifier.byTag(rest)?.let { required.add(it) }
                        ?: parseSingleModifier(rest)?.let { if (primary == null) primary = it }
                }
                else -> {
                    BooleanModifier.byTag(t)?.let { required.add(it) }
                        ?: parseSingleModifier(t)?.let { primary = it }
                }
            }
        }
        return MaximizeSpec(
            primary = primary ?: DoubleModifier.MUS,
            requiredBooleans = required,
            forbiddenBooleans = forbidden,
            equipRequired = equip,
            switchFamiliars = switches,
        )
    }

    fun parse(goal: String): DoubleModifier? = parseSpec(goal)?.primary

    private fun parseSingleModifier(text: String): DoubleModifier? {
        val normalized = text.trim().lowercase()
        return when (normalized) {
            "all" -> DoubleModifier.MUS
            "mus", "muscle", "muscularity" -> DoubleModifier.MUS
            "mys", "myst", "mysticality" -> DoubleModifier.MYS
            "mox", "moxie" -> DoubleModifier.MOX
            "hp", "hit points", "maxhp" -> DoubleModifier.HP
            "mp", "mana", "maxmp" -> DoubleModifier.MP
            "init", "initiative" -> DoubleModifier.INITIATIVE
            "meat", "meat drop" -> DoubleModifier.MEATDROP
            "item", "item drop" -> DoubleModifier.ITEMDROP
            "exp", "experience" -> DoubleModifier.EXPERIENCE
            "abs", "absorb" -> DoubleModifier.ABSORB_STAT
            else -> DoubleModifier.byTag(text.trim())
        }
    }

    private fun splitTerms(goal: String): List<String> {
        val terms = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in goal) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                ch == ',' && !inQuotes -> {
                    terms.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) terms.add(current.toString())
        return terms
    }

    private fun unquote(text: String): String {
        val t = text.trim()
        if (t.length >= 2 && t.first() == '"' && t.last() == '"') {
            return t.substring(1, t.length - 1).trim()
        }
        return t
    }
}
