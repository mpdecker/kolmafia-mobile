package net.sourceforge.kolmafia.data

/**
 * Session cache of parsed description text from desc_item/effect/skill visit HTML.
 * Mirrors desktop [DebugDatabase] description regex patterns.
 */
object DescriptionCache {
    private val itemDescriptions = mutableMapOf<Int, String>()
    private val effectDescriptions = mutableMapOf<Int, String>()
    private val skillDescriptions = mutableMapOf<Int, String>()

    private val ITEM_DESC = Regex("""<div id="description"[^>]*>(.*?)<script""", RegexOption.DOT_MATCHES_ALL)
    private val EFFECT_DESC = Regex("""<div id="description"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
    private val ITEM_ID_COMMENT = Regex("""<!-- itemid: (\d+) -->""")

    fun parseItemDescription(html: String): String? =
        ITEM_DESC.find(html)?.groupValues?.get(1)?.trim()

    fun parseEffectOrSkillDescription(html: String): String? =
        EFFECT_DESC.find(html)?.groupValues?.get(1)?.trim()

    fun parseItemIdFromHtml(html: String): Int? =
        ITEM_ID_COMMENT.find(html)?.groupValues?.get(1)?.toIntOrNull()

    fun cacheItem(itemId: Int, html: String) {
        if (itemId <= 0) return
        parseItemDescription(html)?.let { itemDescriptions[itemId] = it }
    }

    fun cacheEffect(effectId: Int, html: String) {
        if (effectId <= 0) return
        parseEffectOrSkillDescription(html)?.let { effectDescriptions[effectId] = it }
    }

    fun cacheSkill(skillId: Int, html: String) {
        if (skillId <= 0) return
        parseEffectOrSkillDescription(html)?.let { skillDescriptions[skillId] = it }
    }

    fun itemDescription(itemId: Int): String = itemDescriptions[itemId].orEmpty()

    fun effectDescription(effectId: Int): String = effectDescriptions[effectId].orEmpty()

    fun skillDescription(skillId: Int): String = skillDescriptions[skillId].orEmpty()

    fun clear() {
        itemDescriptions.clear()
        effectDescriptions.clear()
        skillDescriptions.clear()
    }
}
