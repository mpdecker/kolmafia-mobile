package net.sourceforge.kolmafia.combat

/**
 * Desktop [CombatEncounterKey] — CCS section header match key with optional
 * `$element[...]` / `$phylum[...]` / `$item[...]` filters (Phases 1131–1145).
 */
class CombatEncounterKey(rawKey: String) {
    val encounterKey: String = rawKey.trim()
    private var monsterName: String = encounterKey
    private var element: String? = null
    private var phylum: String? = null
    private var itemName: String? = null

    init {
        var name = encounterKey
        ELEMENT_PATTERN.find(name)?.let {
            element = it.groupValues[1].trim().lowercase()
            name = name.replace(it.value, "").trim()
        }
        PHYLUM_PATTERN.find(name)?.let {
            phylum = it.groupValues[1].trim().lowercase()
            name = name.replace(it.value, "").trim()
        }
        ITEM_PATTERN.find(name)?.let {
            itemName = it.groupValues[1].trim().lowercase()
            name = name.replace(it.value, "").trim()
        }
        monsterName = name
    }

    /**
     * @param haystack lowercase encounter / location / zone string
     * @param monsterPhylum optional monster phylum for `$phylum` filters
     * @param monsterElement optional defense element for `$element` filters
     * @param monsterItemNames optional drop names for `$item` filters
     */
    fun matches(
        haystack: String,
        monsterPhylum: String? = null,
        monsterElement: String? = null,
        monsterItemNames: Collection<String> = emptyList(),
    ): Boolean {
        if (element != null) {
            if (monsterElement == null || !monsterElement.equals(element, ignoreCase = true)) {
                return false
            }
        }
        if (phylum != null) {
            if (monsterPhylum == null || !monsterPhylum.equals(phylum, ignoreCase = true)) {
                return false
            }
        }
        if (itemName != null) {
            if (monsterItemNames.none { it.equals(itemName, ignoreCase = true) }) {
                return false
            }
        }
        if (monsterName.isEmpty()) return true
        return haystack.contains(monsterName, ignoreCase = true)
    }

    override fun toString(): String = encounterKey

    companion object {
        private val ELEMENT_PATTERN = Regex("""\s*\${'$'}element\[([^\]]+)\]""", RegexOption.IGNORE_CASE)
        private val PHYLUM_PATTERN = Regex("""\s*\${'$'}phylum\[([^\]]+)\]""", RegexOption.IGNORE_CASE)
        private val ITEM_PATTERN = Regex("""\s*\${'$'}item\[([^\]]+)\]""", RegexOption.IGNORE_CASE)
    }
}
