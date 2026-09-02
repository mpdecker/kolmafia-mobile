package net.sourceforge.kolmafia.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleXPathDesktopCorpusTest {

    @Test
    fun childPathCheckboxChecked() {
        val html = """
            <div id="opt_flag_aabosses">
              <label><input type="checkbox" checked="checked"></label>
            </div>
        """.trimIndent()
        val xpath = "//*[@id=\"opt_flag_aabosses\"]/label/input[@type='checkbox']@checked"
        assertEquals(listOf("checked"), SimpleXPath.evaluate(html, xpath))
    }

    @Test
    fun decodesHtmlEntitiesInText() {
        val html = """<p>Tom &amp; Jerry &nbsp; rock</p>"""
        val results = SimpleXPath.evaluate(html, "//p")
        assertEquals(1, results.size)
        assertTrue(results[0].contains("Tom & Jerry"))
    }
}
