package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConsequenceRule
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class SkillDescriptionConsequenceSyncTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        SkillDescriptionConsequenceDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun registerSkill(id: Int, name: String) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = id,
                name = name,
                image = "test.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
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
        SkillDescriptionConsequenceDatabase.injectForTest(mapOf(skillId to rules))
    }

    @Test
    fun applySkillDescription_evaluatesSlimyShouldersExpression() {
        registerSkill(48, "Slimy Shoulders")
        injectRules(
            48,
            "Slimy Shoulders\tgiving you \\+(\\d+)\tskillLevel48=[\$1/2]",
        )
        val p = prefs()
        SkillDescriptionConsequenceSync.applySkillDescription(
            48,
            "This skill is giving you +10 Damage Absorption",
            p,
        )
        assertEquals(5, p.getInt("skillLevel48", -1))
    }

    @Test
    fun applySkillDescription_capturesBanishingShoutMonsters() {
        registerSkill(11020, "Banishing Shout")
        injectRules(
            11020,
            "Banishing Shout\tcurrently banished:<br>([^<]+)<br>(?:([^<]+)<br>(?:([^<]+)<br>|</blockquote>)|</blockquote>)\tbanishingShoutMonsters=\$1|\$2|\$3",
        )
        val html = """
            currently banished:<br>orc<br>goblin<br>snake<br></blockquote>
        """.trimIndent()
        val p = prefs()
        SkillDescriptionConsequenceSync.applySkillDescription(11020, html, p)
        assertEquals("orc|goblin|snake", p.getString("banishingShoutMonsters", ""))
    }

    @Test
    fun applySkillDescription_capturesFavoriteBirdName() {
        registerSkill(190, "Visit your Favorite Bird")
        injectRules(
            190,
            "Visit your Favorite Bird\tyour favorite bird, the (.*?), and get\tyourFavoriteBird=\$1",
        )
        val p = prefs()
        SkillDescriptionConsequenceSync.applySkillDescription(
            190,
            "Visit your favorite bird, the red-breasted robin, and get a blessing.",
            p,
        )
        assertEquals("red-breasted robin", p.getString("yourFavoriteBird", ""))
    }

    @Test
    fun applySkillDescription_evaluatesBearEssenceExpression() {
        registerSkill(134, "Bear Essence")
        injectRules(
            134,
            "Bear Essence\t\\+(\\d+)% Picnic Basket\tskillLevel134=[\$1/20]",
        )
        val p = prefs()
        SkillDescriptionConsequenceSync.applySkillDescription(
            134,
            "+40% Picnic Basket capacity from Bear Essence",
            p,
        )
        assertEquals(2, p.getInt("skillLevel134", -1))
    }
}
