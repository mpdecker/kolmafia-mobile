package net.sourceforge.kolmafia.adventure.choice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChoiceUtilitiesTest {

    private val sampleHtml = """
        <form method="POST" action="choice.php">
        <input type="hidden" name="whichchoice" value="5">
        <input type="hidden" name="pwd" value="abc123">
        <p><input type="submit" name="option" value="1"> Pick up the rock</p>
        <p><input type="submit" name="option" value="2"> Leave it alone</p>
        </form>
    """.trimIndent()

    @Test fun extractChoiceId_returnsId() =
        assertEquals(5, ChoiceUtilities.extractChoiceId(sampleHtml))

    @Test fun extractChoiceId_missing_returnsNull() =
        assertNull(ChoiceUtilities.extractChoiceId("<html>no choice</html>"))

    @Test fun parseChoices_returnsOptionMap() {
        val choices = ChoiceUtilities.parseChoices(sampleHtml)
        assertEquals(2, choices.size)
        assertEquals("Pick up the rock", choices[1])
        assertEquals("Leave it alone", choices[2])
    }

    @Test fun parseChoices_stripsHtml() {
        val html = """
            <input type="hidden" name="whichchoice" value="7">
            <input type="submit" name="option" value="1"> <b>Fight it</b>
            <input type="submit" name="option" value="2"> <i>Run away</i>
        """.trimIndent()
        assertEquals("Fight it", ChoiceUtilities.parseChoices(html)[1])
        assertEquals("Run away",  ChoiceUtilities.parseChoices(html)[2])
    }

    @Test fun parseChoices_empty_returnsEmpty() =
        assertEquals(emptyMap(), ChoiceUtilities.parseChoices("<html></html>"))

    @Test fun parseChoices_multilineHtml() {
        val html = """
            <input type="hidden" name="whichchoice" value="3">
            <input type="submit" name="option" value="1">
            Pick up the sapling
            <input type="submit" name="option" value="2">
            Leave it
        """.trimIndent()
        val choices = ChoiceUtilities.parseChoices(html)
        // The regex captures text after the <input> tag until the next <input or end of string.
        // This text is trimmed and HTML tags are stripped, so newlines and whitespace are handled correctly.
        assertEquals("Pick up the sapling", choices[1])
        assertEquals("Leave it", choices[2])
    }
}
