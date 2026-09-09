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

    @Test
    fun inventoryTabFlagViaContains() {
        val html = """
            <div class="opt">
              <label><input type="checkbox" checked="checked"  name="flag_invimages"></label>
            </div>
        """.trimIndent()
        assertEquals(
            listOf("checked"),
            SimpleXPath.evaluate(html, "//input[contains(@name,'flag_invimages')]@checked"),
        )
    }

    @Test
    fun autoattackSelectedOptionValue() {
        val html = """
            <select name="autoattack">
              <option value="0">None</option>
              <option value="90485" selected="selected">Bonus Adventures from Hell</option>
            </select>
        """.trimIndent()
        assertEquals(
            listOf("90485"),
            SimpleXPath.evaluate(
                html,
                """//select[@name='autoattack']/option[@selected='selected']@value""",
            ),
        )
    }
}
