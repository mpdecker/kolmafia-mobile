package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatSkillDropdownParserTest {

    private val fightHtmlWithDropdown = """
        <html><body>
        You're fighting a monster
        <select name=whichskill>
        <option value="7311">Use the Force, May 4th! (3 uses left) (15 Mana Points)</option>
        <option value="7327">CHEAT CODE: Shrink Enemy (5 of today's remaining 42%) (0 Mana Points)</option>
        <option value="7585">Steal Monster's Heart (ABCD -> WXYZ) (0 Mana Points)</option>
        </select>
        </body></html>
    """.trimIndent()

    @Test
    fun parseAvailableCombatSkills_extractsSkillIdAndLabel() {
        val parsed = CombatSkillDropdownParser.parseAvailableCombatSkills(fightHtmlWithDropdown)
        assertEquals(3, parsed.size)
        assertEquals(7311, parsed[0].first)
        assertEquals(
            "Use the Force, May 4th! (3 uses left) (15 Mana Points)",
            parsed[0].second,
        )
        assertEquals(7327, parsed[1].first)
        assertEquals(
            "CHEAT CODE: Shrink Enemy (5 of today's remaining 42%) (0 Mana Points)",
            parsed[1].second,
        )
        assertEquals(7585, parsed[2].first)
        assertEquals(
            "Steal Monster's Heart (ABCD -> WXYZ) (0 Mana Points)",
            parsed[2].second,
        )
    }

    @Test
    fun parseAvailableCombatSkills_returnsEmptyWhenNoWhichSkillSelect() {
        val html = """<html><body>You're fighting a monster</body></html>"""
        assertTrue(CombatSkillDropdownParser.parseAvailableCombatSkills(html).isEmpty())
    }

    @Test
    fun parseAvailableCombatSkills_returnsEmptyWhenFightWon() {
        val html = """
            <html><body>
            You win the fight!
            <select name="whichskill">
            <option value="7311">Use the Force, May 4th! (3 uses left) (15 Mana Points)</option>
            </select>
            </body></html>
        """.trimIndent()
        assertTrue(CombatSkillDropdownParser.parseAvailableCombatSkills(html).isEmpty())
    }
}
