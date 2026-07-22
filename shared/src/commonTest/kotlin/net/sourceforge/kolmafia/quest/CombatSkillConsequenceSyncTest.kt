package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.CombatSkillConsequenceDatabase
import net.sourceforge.kolmafia.data.ConsequenceRule
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class CombatSkillConsequenceSyncTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        CombatSkillConsequenceDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun registerSkill(id: Int, name: String) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = id,
                name = name,
                image = "test.gif",
                tags = setOf("combat"),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    private fun injectRules(skillId: Int, vararg ruleLines: String) {
        val rules = ruleLines.map { line ->
            val parts = line.split('\t')
            ConsequenceRule(
                spec = parts[0],
                pattern = Regex(parts[1]),
                actions = listOfNotNull(
                    net.sourceforge.kolmafia.data.ConsequenceActionParser.parseAction(parts[2]),
                ),
            )
        }
        CombatSkillConsequenceDatabase.injectForTest(mapOf(skillId to rules))
    }

    private fun fightHtml(vararg options: String): String = """
        <html><body>
        You're fighting a monster
        <select name=whichskill>
        ${options.joinToString("\n")}
        </select>
        </body></html>
    """.trimIndent()

    @Test
    fun applyFromFightHtml_evaluatesUseTheForceUses() {
        registerSkill(7311, "Use the Force")
        injectRules(
            7311,
            "Use the Force\tUse the Force, [\\w ]+! \\((\\d+) uses? left\\)\t_saberForceUses=[5-\$1]",
        )
        val html = fightHtml(
            """<option value="7311">Use the Force, May 4th! (2 uses left) (15 Mana Points)</option>""",
        )
        val p = prefs()
        CombatSkillConsequenceSync.applyFromFightHtml(html, p)
        assertEquals(3, p.getInt("_saberForceUses", -1))
    }

    @Test
    fun applyFromFightHtml_evaluatesPowerfulGloveBattery() {
        registerSkill(7327, "CHEAT CODE: Shrink Enemy")
        injectRules(
            7327,
            "CHEAT CODE: Shrink Enemy\tCHEAT CODE: Shrink Enemy \\(5 of today's remaining (\\d+)%\\)\t_powerfulGloveBatteryPowerUsed=[100-\$1]",
        )
        val html = fightHtml(
            """<option value="7327">CHEAT CODE: Shrink Enemy (5 of today's remaining 37%) (0 Mana Points)</option>""",
        )
        val p = prefs()
        CombatSkillConsequenceSync.applyFromFightHtml(html, p)
        assertEquals(63, p.getInt("_powerfulGloveBatteryPowerUsed", -1))
    }

    @Test
    fun applyFromFightHtml_capturesHeartstoneLetters() {
        registerSkill(7585, "Steal Monster's Heart")
        injectRules(
            7585,
            "Steal Monster's Heart\tSteal Monster's Heart \\(([A-Z]+) -> [A-Z]+\\)\theartstoneLetters=\$1",
        )
        val html = fightHtml(
            """<option value="7585">Steal Monster's Heart (WXYZ -> ABCD) (0 Mana Points)</option>""",
        )
        val p = prefs()
        CombatSkillConsequenceSync.applyFromFightHtml(html, p)
        assertEquals("WXYZ", p.getString("heartstoneLetters", ""))
    }

    @Test
    fun applyFromFightHtml_evaluatesBackUpUsesWithRobotPath() {
        registerSkill(7381, "Back-Up to your Last Enemy")
        injectRules(
            7381,
            "Back-Up to your Last Enemy\tBack-Up to your Last Enemy \\((\\d+) uses? today\\)\t_backUpUses=[(11+path(You, Robot)*5)-\$1]",
        )
        val html = fightHtml(
            """<option value="7381">Back-Up to your Last Enemy (4 uses today) (0 Mana Points)</option>""",
        )
        val p = prefs()
        CombatSkillConsequenceSync.applyFromFightHtml(
            html,
            p,
            ExpressionContext(challengePath = "You, Robot"),
        )
        assertEquals(12, p.getInt("_backUpUses", -1))
    }

    @Test
    fun applyFromFightHtml_backUpUsesDefaultsWithoutPath() {
        registerSkill(7381, "Back-Up to your Last Enemy")
        injectRules(
            7381,
            "Back-Up to your Last Enemy\tBack-Up to your Last Enemy \\((\\d+) uses? today\\)\t_backUpUses=[(11+path(You, Robot)*5)-\$1]",
        )
        val html = fightHtml(
            """<option value="7381">Back-Up to your Last Enemy (4 uses today) (0 Mana Points)</option>""",
        )
        val p = prefs()
        CombatSkillConsequenceSync.applyFromFightHtml(html, p)
        assertEquals(7, p.getInt("_backUpUses", -1))
    }
}
