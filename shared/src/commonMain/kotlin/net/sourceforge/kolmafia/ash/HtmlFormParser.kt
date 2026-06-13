package net.sourceforge.kolmafia.ash

object HtmlFormParser {
    /** Returns map of input name → value for the first `<form>` in [html]. */
    fun parseFirstForm(html: String): Map<String, String> {
        val formStart = html.indexOf("<form", ignoreCase = true)
        if (formStart < 0) return emptyMap()
        val formEnd = html.indexOf("</form>", formStart, ignoreCase = true)
        val formHtml = if (formEnd >= 0) html.substring(formStart, formEnd) else html.substring(formStart)

        val fields = linkedMapOf<String, String>()
        val inputTag = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)
        val nameAttr = Regex("""name\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val valueAttr = Regex("""value\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

        inputTag.findAll(formHtml).forEach { match ->
            val tag = match.value
            val name = nameAttr.find(tag)?.groupValues?.get(1) ?: return@forEach
            val value = valueAttr.find(tag)?.groupValues?.get(1) ?: ""
            fields[name] = value
        }
        return fields
    }
}
