package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TCRSDeriverTest {

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
        DescriptionCache.clear()
    }

    @Test
    fun deriveFromHtml_parsesNameSizeQualityAndModifiers() {
        val html = buildString {
            append("""<b>spicy bouncing batwing</b>""")
            append("""Type: <b>food <font color=green>(good)</font></b>""")
            append("""Size: <b>2</b>""")
            append("""<font color=blue>+5% Meat from Monsters<br></font>""")
        }
        val entry = TCRSDeriver.deriveFromHtml(471, html)
        assertEquals("spicy bouncing batwing", entry.name)
        assertEquals(2, entry.size)
        assertEquals("good", entry.quality)
        assertTrue(entry.modifiers.contains("Meat Drop: +5"))
    }

    @Test
    fun deriveFromCache_usesDescriptionCache() {
        DescriptionCache.cacheItem(
            471,
            """<div id="description"><b>cached wing</b><font color=blue>Combat Initiative +30%<br></font><script></script>""",
        )
        val entry = TCRSDeriver.deriveFromCache(471)
        requireNotNull(entry)
        assertEquals("cached wing", entry.name)
        assertTrue(entry.modifiers.contains("Initiative: +30"))
    }
}
