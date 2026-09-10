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

    @Test
    fun containsPredicate_matchesPartialAttr() {
        val html = """<div><input name="flag_invimages" checked="checked"><input name="other"></div>"""
        val results = SimpleXPath.evaluate(html, "//input[contains(@name,'invimages')]@checked")
        assertEquals(listOf("checked"), results)
    }

    @Test
    fun positionPredicate_selectsFirstAndLast() {
        val html = """<select><option value="1">A</option><option value="2">B</option><option value="3">C</option></select>"""
        assertEquals(listOf("1"), SimpleXPath.evaluate(html, "//option[1]/@value"))
        assertEquals(listOf("3"), SimpleXPath.evaluate(html, "//option[last()]/@value"))
    }

    @Test
    fun midPathAttributeStep_returnsValues() {
        val html = """<select name="whichclan"><option value="9">Clan</option></select>"""
        assertEquals(listOf("9"), SimpleXPath.evaluate(html, """//select[@name="whichclan"]//option/@value"""))
    }

    @Test
    fun childTextStep_returnsDirectText() {
        val html = """<select><option value="90485">Bonus Adventures from Hell</option></select>"""
        assertEquals(
            listOf("Bonus Adventures from Hell"),
            SimpleXPath.evaluate(html, "//option/text()"),
        )
    }

    @Test
    fun followingSiblingAxis_selectsLaterSiblings() {
        val html = """
            <div>
              <span id="a">A</span>
              <span id="b">B</span>
              <span id="c">C</span>
            </div>
        """.trimIndent()
        val results = SimpleXPath.evaluate(html, """//span[@id='a']/following-sibling::span""")
        assertEquals(2, results.size)
        assertTrue(results[0].contains("B"))
        assertTrue(results[1].contains("C"))
    }

    @Test
    fun followingSiblingWildcard_withPosition() {
        val html = """<p><b>one</b><i>two</i><u>three</u></p>"""
        assertEquals(
            listOf("two"),
            SimpleXPath.evaluate(html, "//b/following-sibling::*[1]/text()").map { it.trim() },
        )
    }
}
