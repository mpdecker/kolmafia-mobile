package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.RestrictedItemType

/** Shared HTML parse for standard.php / thrifty.php restricted-item lists. */
internal object RestrictedItemsParse {

    private val SECTION_PATTERN = Regex("""<b>(.*?)</b><p>(.*?)<p>""", RegexOption.DOT_MATCHES_ALL)
    private val OBJECT_PATTERN = Regex("""<span class="i">(.*?)(?:,\s*)?</span>""")

    fun parseSections(html: String): Map<RestrictedItemType, Set<String>> {
        val result = mutableMapOf<RestrictedItemType, MutableSet<String>>()
        for (match in SECTION_PATTERN.findAll(html)) {
            val type = RestrictedItemType.fromString(match.groupValues[1].trim()) ?: continue
            val body = match.groupValues[2]
            for (obj in OBJECT_PATTERN.findAll(body)) {
                val name = obj.groupValues[1].trim().lowercase()
                if (name.isNotEmpty()) {
                    result.getOrPut(type) { mutableSetOf() }.add(name)
                }
            }
        }
        return result
    }
}
