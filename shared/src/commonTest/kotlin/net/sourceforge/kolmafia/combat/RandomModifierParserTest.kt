package net.sourceforge.kolmafia.combat

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RandomModifierParserTest {

    @Test
    fun parseRandomModifiers_stripsMappedAdjectives() {
        val html = """<script>var ocrs = ["huge", "hot"];</script>"""
        val result = RandomModifierParser.parseRandomModifiers(
            "huge, red-hot Knob Goblin",
            html,
        )
        assertEquals("Knob Goblin", result.strippedName)
        assertEquals(listOf("huge", "red-hot"), result.modifiers)
    }

    @Test
    fun parseRandomModifiers_noOcrsReturnsOriginal() {
        val result = RandomModifierParser.parseRandomModifiers(
            "huge mosquito",
            "<html><body>fight</body></html>",
        )
        assertEquals("huge mosquito", result.strippedName)
        assertEquals(emptyList(), result.modifiers)
    }

    @Test
    fun parseRandomModifiers_skipsDrippyToken() {
        val html = """<script>var ocrs = ["drippy", "huge"];</script>"""
        val result = RandomModifierParser.parseRandomModifiers(
            "huge drippy bat",
            html,
        )
        assertEquals(listOf("huge"), result.modifiers)
    }

    @Test
    fun parseRandomModifiers_extraModifier() {
        val html = """<script>var ocrs = ["powerPixel"];</script>"""
        val result = RandomModifierParser.parseRandomModifiers(
            "powerPixel goblin",
            html,
        )
        assertEquals(listOf("powerPixel"), result.modifiers)
    }

    @Test
    fun parseRandomModifiers_leetTokenResolvesCanonicalName() = runBlocking {
        net.sourceforge.kolmafia.data.MonsterDatabase.load()
        val leetName = net.sourceforge.kolmafia.utilities.leetify("Naughty Sorceress")
        val html = """<script>var ocrs = ["leet"];</script>"""
        val result = RandomModifierParser.parseRandomModifiers("The $leetName", html)
        assertEquals("The Naughty Sorceress", result.strippedName)
        assertEquals(listOf("1337"), result.modifiers)
    }

    @Test
    fun parseMonsterId_extractsComment() {
        assertEquals(
            1341,
            RandomModifierParser.parseMonsterId("<!-- MONSTERID: 1341 -->"),
        )
        assertNull(RandomModifierParser.parseMonsterId("<html></html>"))
    }
}
