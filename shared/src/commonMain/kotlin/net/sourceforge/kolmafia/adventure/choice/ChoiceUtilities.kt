package net.sourceforge.kolmafia.adventure.choice

object ChoiceUtilities {

    private val CHOICE_ID_REGEX =
        Regex("""<input[^>]+name="whichchoice"[^>]+value="(\d+)"""", RegexOption.IGNORE_CASE)

    // Match any <input> tag, capture it and everything after until the next <input or end.
    // Then extract name="option" and value= attributes regardless of order.
    private val OPTION_TAG_REGEX =
        Regex("""(<input[^>]+>)([\s\S]*?)(?=<input|$)""", RegexOption.IGNORE_CASE)
    private val OPTION_NAME_REGEX = Regex("""name=["']?option["']?""", RegexOption.IGNORE_CASE)
    private val OPTION_VALUE_REGEX = Regex("""value=["']?(\d+)["']?""", RegexOption.IGNORE_CASE)
    private val TITLE_REGEX = Regex("""title=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    private val FORM_REGEX =
        Regex("""<form[^>]*>([\s\S]*?)</form>""", RegexOption.IGNORE_CASE)
    private val SELECT_REGEX =
        Regex(
            """<select\s[^>]*name=["']?(\w+)["']?[^>]*>([\s\S]*?)</select>""",
            RegexOption.IGNORE_CASE,
        )
    private val SELECT_OPTION_REGEX =
        Regex(
            """<option\s[^>]*value=["']?(\d*)["']?[^>]*>([\s\S]*?)</option>""",
            RegexOption.IGNORE_CASE,
        )
    private val TEXT_INPUT_REGEX =
        Regex("""<input\s([^>]+)>""", RegexOption.IGNORE_CASE)
    private val ATTR_NAME_REGEX = Regex("""name=["']?([^'"\s>]+)["']?""", RegexOption.IGNORE_CASE)
    private val ATTR_TYPE_REGEX = Regex("""type=["']?([^'"\s>]+)["']?""", RegexOption.IGNORE_CASE)

    fun extractChoiceId(html: String): Int? =
        CHOICE_ID_REGEX.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun extractChoiceFromUrl(url: String): Int =
        Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull() ?: 0

    fun parseChoices(html: String): Map<Int, String> =
        OPTION_TAG_REGEX.findAll(html)
            .mapNotNull { m ->
                val tag = m.groupValues[1]
                if (!OPTION_NAME_REGEX.containsMatchIn(tag)) return@mapNotNull null
                val n = OPTION_VALUE_REGEX.find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return@mapNotNull null
                val text = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                    .ifEmpty {
                        Regex("""value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                            .find(tag)?.groupValues?.getOrNull(1)?.takeIf {
                                it.toIntOrNull() == null
                            }.orEmpty()
                    }
                if (text.isEmpty()) null else n to text
            }
            .toMap()

    /**
     * Desktop [ChoiceUtilities.parseChoicesWithSpoilers] — appends title= spoilers when present.
     * Full ChoiceAdventures spoiler DB is not ported; title attributes cover common cases.
     */
    fun parseChoicesWithSpoilers(html: String): Map<Int, String> {
        val base = parseChoices(html).toMutableMap()
        OPTION_TAG_REGEX.findAll(html).forEach { m ->
            val tag = m.groupValues[1]
            if (!OPTION_NAME_REGEX.containsMatchIn(tag)) return@forEach
            val n = OPTION_VALUE_REGEX.find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@forEach
            val title = TITLE_REGEX.find(tag)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            if (title.isNotEmpty()) {
                val current = base[n] ?: return@forEach
                if (!current.contains(title)) {
                    base[n] = "$current ($title)"
                }
            }
        }
        return base
    }

    /** CHOICE => select-name => (option-value => option-label). */
    fun parseSelectInputsWithTags(html: String): Map<Int, Map<String, Map<String, String>>> {
        val result = linkedMapOf<Int, Map<String, Map<String, String>>>()
        for (formMatch in FORM_REGEX.findAll(html)) {
            val form = formMatch.groupValues[1]
            val decision = optionDecisionFromForm(form) ?: continue
            val selects = linkedMapOf<String, Map<String, String>>()
            for (sel in SELECT_REGEX.findAll(form)) {
                val name = sel.groupValues[1]
                val options = linkedMapOf<String, String>()
                for (opt in SELECT_OPTION_REGEX.findAll(sel.groupValues[2])) {
                    val value = opt.groupValues[1]
                    val label = opt.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                    options[value] = label
                }
                if (options.isNotEmpty()) selects[name] = options
            }
            if (selects.isNotEmpty()) result[decision] = selects
        }
        return result
    }

    /** CHOICE => set of text-input names. */
    fun parseTextInputs(html: String): Map<Int, Set<String>> {
        val result = linkedMapOf<Int, MutableSet<String>>()
        for (formMatch in FORM_REGEX.findAll(html)) {
            val form = formMatch.groupValues[1]
            val decision = optionDecisionFromForm(form) ?: continue
            val names = linkedSetOf<String>()
            for (input in TEXT_INPUT_REGEX.findAll(form)) {
                val attrs = input.groupValues[1]
                val type = ATTR_TYPE_REGEX.find(attrs)?.groupValues?.getOrNull(1)?.lowercase()
                    ?: "text"
                if (type != "text" && type != "textfield") continue
                val name = ATTR_NAME_REGEX.find(attrs)?.groupValues?.getOrNull(1) ?: continue
                if (name.equals("option", ignoreCase = true) ||
                    name.equals("whichchoice", ignoreCase = true) ||
                    name.equals("pwd", ignoreCase = true)
                ) {
                    continue
                }
                names.add(name)
            }
            if (names.isNotEmpty()) result[decision] = names
        }
        return result
    }

    private fun optionDecisionFromForm(form: String): Int? {
        for (m in OPTION_TAG_REGEX.findAll(form)) {
            val tag = m.groupValues[1]
            if (!OPTION_NAME_REGEX.containsMatchIn(tag)) continue
            return OPTION_VALUE_REGEX.find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        return null
    }
}
