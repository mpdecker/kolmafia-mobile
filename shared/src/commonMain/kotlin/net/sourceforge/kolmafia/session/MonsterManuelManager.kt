package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.MonsterDatabase

/**
 * Desktop MonsterManuelManager cache seed. Entries are detached HTML table fragments; [updates]
 * records Manuel metadata that differs from the bundled monster database without mutating it.
 */
object MonsterManuelManager {
    const val NO_FACTOIDS = ""

    private val manuelEntries = sortedMapOf<Int, String>()
    private val manuelFactoidCounts = sortedMapOf<Int, Int>()
    val updates: MutableMap<Int, MutableMap<String, Any>> = mutableMapOf()

    private val variableNamedMonsters = setOf(1667, 1669, 2105, 2106, 2107, 2108)

    fun flushCache() {
        manuelEntries.clear()
        manuelFactoidCounts.clear()
        updates.clear()
    }

    /** Login reset preserves complete (three-factoid) entries, as desktop does. */
    fun reset() {
        variableNamedMonsters.forEach(::reset)
        manuelFactoidCounts.filterValues { it < 3 }.keys.toList().forEach(::reset)
    }

    fun reset(monsterId: Int) {
        if (monsterId <= 0) return
        manuelEntries.remove(monsterId)
        manuelFactoidCounts.remove(monsterId)
        updates.remove(monsterId)
    }

    fun registerMonster(id: Int, text: String) {
        if (id <= 0 || text.isBlank()) return
        val old = manuelEntries[id]
        if (old == text) return
        manuelEntries[id] = text
        manuelFactoidCounts[id] = countFactoids(text)

        val known = MonsterDatabase.getById(id) ?: return
        val name = extractMonsterName(text)
        val image = extractMonsterImage(text)
        val article = extractMonsterArticle(text)
        val delta = linkedMapOf<String, Any>()
        if (id !in variableNamedMonsters && name.isNotBlank() &&
            !name.equals(known.manuelName ?: known.name, ignoreCase = false)
        ) {
            delta["manuelName"] = name
        }
        if (image.isNotBlank() && image !in known.images && image != known.image &&
            image != "noart.gif" && image != "qmark.gif" && id !in setOf(210, 1669)
        ) {
            delta["image"] = image
        }
        if (article != known.article) delta["article"] = article
        if (delta.isNotEmpty()) updates[id] = delta
    }

    fun getCachedManuelText(id: Int): String = manuelEntries[id] ?: NO_FACTOIDS

    fun getFactoids(id: Int): List<String> {
        val text = getCachedManuelText(id)
        val block = FACTOIDS.find(text)?.groupValues?.get(1) ?: return emptyList()
        return FACTOID.findAll(block).map { it.groupValues[1] }.toList()
    }

    fun getFactoidsAvailable(id: Int): Int = manuelFactoidCounts[id] ?: 0

    fun countFactoids(text: String): Int {
        val block = FACTOIDS.find(text)?.groupValues?.get(1) ?: return 0
        return FACTOID.findAll(block).count()
    }

    fun extractMonsterName(text: String): String =
        NAME.find(text)?.groupValues?.get(1)?.trim().orEmpty()

    fun extractMonsterImage(text: String): String {
        val match = IMAGE.find(text) ?: return ""
        val directory = match.groupValues[1]
        val file = match.groupValues[2]
        return if (directory.isBlank() || directory == "adventureimages") file else "$directory/$file"
    }

    fun extractMonsterAttack(text: String): String = ATTACK.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterDefense(text: String): String = DEFENSE.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterHp(text: String): String = HP.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterPhylum(text: String): String = PHYLUM.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterElement(text: String): String = ELEMENT.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterInitiative(text: String): String = INITIATIVE.find(text)?.groupValues?.get(1).orEmpty()
    fun extractMonsterArticle(text: String): String = ARTICLE.find(text)?.groupValues?.get(2).orEmpty()

    fun parseInitiative(text: String): Int = when {
        text.startsWith("Never") -> -10_000
        text.startsWith("Always") -> 10_000
        else -> Regex("""-?\d+""").find(text)?.value?.toIntOrNull() ?: 0
    }

    private val NAME =
        Regex("""<td rowspan=4 valign=top class=small><b><font size=\+2>(.*?)</font></b>""",
            RegexOption.IGNORE_CASE)
    private val IMAGE =
        Regex("""<td rowspan=4 valign=top width=100><img src=[^>]*?(?:cloudfront\.net|images\.kingdomofloathing\.com|/images)/(?:(adventureimages|otherimages)/(?:\.\./)?)?(.*?\.gif).*?</td>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ATTACK = Regex("""Attack Power \(approximate\).*?<font size=\+2>(.*?)</font>""",
        RegexOption.DOT_MATCHES_ALL)
    private val DEFENSE = Regex("""Defense \(approximate\).*?<font size=\+2>(.*?)</font>""",
        RegexOption.DOT_MATCHES_ALL)
    private val HP = Regex("""Hit Points \(approximate\).*?<font size=\+2>(.*?)</font>""",
        RegexOption.DOT_MATCHES_ALL)
    private val PHYLUM = Regex("""This monster is (?:an? )?(.*?)["']""")
    private val ELEMENT = Regex("""This monster is (Hot|Cold|Spooky|Stinky|Sleazy)""")
    private val INITIATIVE =
        Regex("""["'](Never wins initiative|Always wins initiative|Initiative \+.*?%)["']""")
    private val ARTICLE =
        Regex("""<td rowspan=4 valign=top class=small><b><font size=\+2>(.*?)</font></b><!-- article:(.*?) -->""",
            RegexOption.DOT_MATCHES_ALL)
    private val FACTOIDS = Regex("""<ul>(.*?)</ul>""", RegexOption.DOT_MATCHES_ALL)
    private val FACTOID = Regex("""<li>(.*?)(?=<li>|$)""", RegexOption.DOT_MATCHES_ALL)
}
