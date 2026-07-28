package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DescriptionCacheTest {

    @AfterTest
    fun tearDown() {
        DescriptionCache.clear()
    }

    @Test
    fun parseItemDescription_extractsTextBeforeScript() {
        val html = """
            <html><body>
            <!-- itemid: 2 -->
            <div id="description"><p>A sharp tooth from a seal.</p>
            <script type="text/javascript">var x = 1;</script>
            </body></html>
        """.trimIndent()
        assertEquals("<p>A sharp tooth from a seal.</p>", DescriptionCache.parseItemDescription(html))
    }

    @Test
    fun parseEffectOrSkillDescription_extractsDivContent() {
        val html = """<div id="description"><p>A bright light surrounds you.</p></div><div id="other">"""
        assertEquals("<p>A bright light surrounds you.</p>", DescriptionCache.parseEffectOrSkillDescription(html))
    }

    @Test
    fun parseItemIdFromHtml_readsCommentFallback() {
        assertEquals(42, DescriptionCache.parseItemIdFromHtml("<!-- itemid: 42 -->"))
        assertNull(DescriptionCache.parseItemIdFromHtml("<html>no comment</html>"))
    }

    @Test
    fun cacheAndRetrieve_itemEffectSkill() {
        val itemHtml = """<div id="description"><b>Item desc</b><script>"""
        val effectHtml = """<div id="description"><b>Effect desc</b></div>"""
        val skillHtml = """<div id="description"><b>Skill desc</b></div>"""

        DescriptionCache.cacheItem(2, itemHtml)
        DescriptionCache.cacheEffect(1, effectHtml)
        DescriptionCache.cacheSkill(1, skillHtml)

        assertEquals("<b>Item desc</b>", DescriptionCache.itemDescription(2))
        assertEquals("<b>Effect desc</b>", DescriptionCache.effectDescription(1))
        assertEquals("<b>Skill desc</b>", DescriptionCache.skillDescription(1))
    }

    @Test
    fun cache_skipsInvalidIds() {
        DescriptionCache.cacheItem(0, """<div id="description">x<script>""")
        DescriptionCache.cacheEffect(-1, """<div id="description">x</div>""")
        assertEquals("", DescriptionCache.itemDescription(0))
        assertEquals("", DescriptionCache.effectDescription(-1))
    }

    @Test
    fun clear_removesCachedDescriptions() {
        DescriptionCache.cacheItem(2, """<div id="description">cached<script>""")
        DescriptionCache.clear()
        assertEquals("", DescriptionCache.itemDescription(2))
    }
}
