package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlFormParserTest {

    @Test
    fun parseFirstForm_extractsInputNameAndValue() {
        val html = """
            <html><body>
            <form action="fight.php">
              <input type="hidden" name="action" value="attack">
              <input type="text" name="weapon" value="fist">
            </form>
            </body></html>
        """.trimIndent()
        val fields = HtmlFormParser.parseFirstForm(html)
        assertEquals("attack", fields["action"])
        assertEquals("fist", fields["weapon"])
    }

    @Test
    fun parseFirstForm_ignoresInputsOutsideFirstForm() {
        val html = """
            <input name="outer" value="x">
            <form><input name="inner" value="y"></form>
        """.trimIndent()
        val fields = HtmlFormParser.parseFirstForm(html)
        assertEquals("y", fields["inner"])
        assertEquals(null, fields["outer"])
    }

    @Test
    fun parseFirstForm_returnsEmptyWhenNoForm() {
        assertEquals(emptyMap(), HtmlFormParser.parseFirstForm("<html></html>"))
    }

    @Test
    fun parseFirstForm_handlesSingleQuotedAttributes() {
        val html = """<form><input name='pwd' value='secret'></form>"""
        assertEquals("secret", HtmlFormParser.parseFirstForm(html)["pwd"])
    }
}
