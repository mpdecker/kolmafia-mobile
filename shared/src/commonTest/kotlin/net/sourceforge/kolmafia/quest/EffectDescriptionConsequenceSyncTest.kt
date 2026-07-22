package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConsequenceAction
import net.sourceforge.kolmafia.data.ConsequenceRule
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class EffectDescriptionConsequenceSyncTest {

    @BeforeTest
    fun loadModifiers() = runTest {
        ModifierDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
        EffectDescriptionConsequenceDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun registerEffect(id: Int, name: String, descId: String) {
        EffectDatabase.registerForTest(
            EffectData(
                id = id,
                name = name,
                image = "test.gif",
                descId = descId,
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
            ),
        )
    }

    private fun injectRules(descId: String, vararg ruleLines: String) {
        val rules = ruleLines.map { line ->
            val parts = line.split('\t')
            val pattern = if (parts[1].isEmpty()) Regex("") else Regex(parts[1])
            ConsequenceRule(
                spec = parts[0],
                pattern = pattern,
                actions = listOfNotNull(
                    net.sourceforge.kolmafia.data.ConsequenceActionParser.parseAction(parts[2]),
                    parts.getOrNull(3)?.let {
                        net.sourceforge.kolmafia.data.ConsequenceActionParser.parseAction(it)
                    },
                ),
            )
        }
        EffectDescriptionConsequenceDatabase.injectForTest(mapOf(descId to rules))
    }

    private fun parseEffectActionText(actionText: String): ConsequenceAction? {
        val eq = actionText.indexOf('=')
        if (eq <= 0) return null
        val key = actionText.substring(0, eq).trim()
        val value = actionText.substring(eq + 1).trim()
        if (value == "mods") return ConsequenceAction.SetEffectMods(key)
        return net.sourceforge.kolmafia.data.ConsequenceActionParser.parseAction(actionText)
    }

    private fun injectEffectRules(descId: String, vararg ruleLines: String) {
        val rules = ruleLines.map { line ->
            val parts = line.split('\t')
            val pattern = if (parts[1].isEmpty()) Regex("") else Regex(parts[1])
            val actions = buildList {
                add(parseEffectActionText(parts[2])!!)
                parts.getOrNull(3)?.let { add(parseEffectActionText(it)!!) }
            }
            ConsequenceRule(
                spec = parts[0],
                pattern = pattern,
                actions = actions,
            )
        }
        EffectDescriptionConsequenceDatabase.injectForTest(mapOf(descId to rules))
    }

    @Test
    fun applyEffectDescription_setsSimpleCapturePref() {
        registerEffect(615, "Antihangover", "antihangover-desc")
        injectRules(
            "antihangover-desc",
            "Antihangover\tMoxie \\+(\\d+)\t_antihangoverBonus=\$1",
        )
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription(
            "antihangover-desc",
            "Moxie +15",
            p,
        )
        assertEquals("15", p.getString("_antihangoverBonus", ""))
    }

    @Test
    fun applyEffectDescription_evaluatesKnightlifeExpression() {
        registerEffect(1, "Knightlife", "knight-desc")
        injectRules(
            "knight-desc",
            "Knightlife\t\\+(\\d+)%\tchessboardsCleared=[(\$1-100)/2]",
        )
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription(
            "knight-desc",
            "+150%",
            p,
        )
        assertEquals(25, p.getInt("chessboardsCleared", -1))
    }

    @Test
    fun applyEffectDescription_evaluatesStarryEyedExpression() {
        registerEffect(1, "Starry-Eyed", "starry-desc")
        injectRules(
            "starry-desc",
            "Starry-Eyed\tAll Attributes \\+(\\d+)%\ttelescopeUpgrades=[\$1/5]",
        )
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription(
            "starry-desc",
            "All Attributes +25%",
            p,
        )
        assertEquals(5, p.getInt("telescopeUpgrades", -1))
    }

    @Test
    fun applyEffectDescription_setModsPrefAndOverridesEffect() {
        ModifierDatabase.injectForTest(
            "Effect",
            "Grafted",
            "Lasts Until Rollover",
        )
        registerEffect(2964, "Grafted", "grafted-desc")
        injectEffectRules(
            "grafted-desc",
            "Grafted\t\tzootGraftedMods=mods",
        )
        val html = """
            <font color=blue><b>Muscle +10<br>+60% Meat from Monsters</b></font>
        """.trimIndent()
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription("grafted-desc", html, p)
        assertEquals("Muscle: +10, Meat Drop: +60", p.getString("zootGraftedMods", ""))
        val entry = ModifierDatabase.getEffect("Grafted")
        assertEquals("Muscle: +10, Meat Drop: +60", entry?.modifiers)
    }

    @Test
    fun applyEffectDescription_setModsCarriesOverConditionalSkills() {
        ModifierDatabase.injectForTest(
            "Effect",
            "Heartstone Attunement",
            """Conditional Skill (Equipped): "Heartstone: Luck"""",
        )
        registerEffect(3071, "Heartstone Attunement", "heartstone-desc")
        injectEffectRules(
            "heartstone-desc",
            "Heartstone Attunement\t\theartstoneAttunementMods=mods",
        )
        val html = """<font color=blue><b>+5 Familiar Weight<br>+40% Meat from Monsters</b></font>"""
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription("heartstone-desc", html, p)
        val entry = ModifierDatabase.getEffect("Heartstone Attunement")
        assertTrue(entry?.modifiers?.contains("Familiar Weight: +5") == true)
        assertTrue(entry?.modifiers?.contains("Heartstone: Luck") == true)
    }

    @Test
    fun applyEffectDescription_firesCaptureAndModsRules() {
        registerEffect(2822, "Citizen of a Zone", "citizen-desc")
        injectEffectRules(
            "citizen-desc",
            "Citizen of a Zone\tCitizen of ([^<]*)<\t_citizenZone=\$1",
            "Citizen of a Zone\t\t_citizenZoneMods=mods",
        )
        val html = """
            Citizen of The Sleaze<br>
            <font color=blue><b>Muscle +5</b></font>
        """.trimIndent()
        val p = prefs()
        EffectDescriptionConsequenceSync.applyEffectDescription("citizen-desc", html, p)
        assertEquals("The Sleaze", p.getString("_citizenZone", ""))
        assertEquals("Muscle: +5", p.getString("_citizenZoneMods", ""))
    }
}
