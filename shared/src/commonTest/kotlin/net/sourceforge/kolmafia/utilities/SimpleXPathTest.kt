package net.sourceforge.kolmafia.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.ash.ScriptException
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimpleXPathTest {

    @Test
    fun blankXpath_returnsSerializedHtml() {
        val html = "<html><head><title>Hello</title></head><body>World</body></html>"
        val results = SimpleXPath.evaluate(html, "")
        assertEquals(1, results.size)
        assertTrue(results[0].contains("<html>"))
        assertTrue(results[0].contains("World"))
    }

    @Test
    fun descendantTag_matchesBody() {
        val html = "<html><body>Hi</body></html>"
        val results = SimpleXPath.evaluate(html, "//body")
        assertEquals(1, results.size)
        assertTrue(results[0].contains("Hi"))
    }

    @Test
    fun attributeSuffix_returnsCheckedValue() {
        val html = """<div><label><input type="checkbox" checked="checked"></label></div>"""
        val results = SimpleXPath.evaluate(html, "//input[@type='checkbox']@checked")
        assertEquals(1, results.size)
        assertEquals("checked", results[0])
    }

    @Test
    fun attributeNodes_returnValues() {
        val fragment = """<select><option value="90485">Bonus Adventures from Hell</option></select>"""
        assertEquals(listOf("90485"), SimpleXPath.evaluate(fragment, "//@value"))
    }

    @Test
    fun textNodes_returnInnerText() {
        val fragment = """<select><option value="90485">Bonus Adventures from Hell</option></select>"""
        assertEquals(listOf("Bonus Adventures from Hell"), SimpleXPath.evaluate(fragment, "//text()"))
    }

    @Test
    fun nestedDescendants_matchOptions() {
        val html = """<select name="whichclan"><option value="1">Alpha</option><option value="2">Beta</option></select>"""
        val results = SimpleXPath.evaluate(html, """//select[@name="whichclan"]//option""")
        assertEquals(2, results.size)
        assertTrue(results[0].contains("Alpha"))
        assertTrue(results[1].contains("Beta"))
    }

    @Test
    fun invalidXPath_throws() {
        assertFailsWith<ScriptException> {
            SimpleXPath.evaluate("<p>", "//p[")
        }
    }
}
