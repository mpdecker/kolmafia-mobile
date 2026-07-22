package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConsequenceRule
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemDescriptionConsequenceDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.util.RomanNumerals

class ItemDescriptionConsequenceSyncTest {

    @BeforeTest
    fun loadMonsters() = runTest {
        MonsterDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
        ItemDescriptionConsequenceDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun registerItem(id: Int, name: String, descId: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = descId,
                image = "test.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
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
                    parts.getOrNull(3)?.let { net.sourceforge.kolmafia.data.ConsequenceActionParser.parseAction(it) },
                ),
            )
        }
        ItemDescriptionConsequenceDatabase.injectForTest(mapOf(descId to rules))
    }

    @Test
    fun applyItemDescription_setsSimpleCapturePref() {
        registerItem(10929, "designer sweatpants", "800334855")
        injectRules(
            "800334855",
            "designer sweatpants\tYour sweatpants are currently (\\d+)% sweat-logged.\tsweat=\$1",
        )
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription(
            "800334855",
            "Your sweatpants are currently 42% sweat-logged.",
            p,
        )
        assertEquals("42", p.getString("sweat", ""))
    }

    @Test
    fun applyItemDescription_evaluatesBracketMinExpression() {
        registerItem(1, "fossilized necklace", "fossil-desc")
        injectRules(
            "fossil-desc",
            "fossilized necklace\t(\\d+) bat\tfossilB=[min(5,\$1)]",
        )
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription(
            "fossil-desc",
            "This necklace contains 3 bat fossils.",
            p,
        )
        assertEquals(3, p.getInt("fossilB", -1))
    }

    @Test
    fun applyItemDescription_resolvesMonsterName() {
        registerItem(6677, "crude monster sculpture", "578558932")
        injectRules(
            "578558932",
            "crude monster sculpture\t<!-- monsterid: (\\d+) -->\tcrudeMonster=monstername",
        )
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription(
            "578558932",
            """<!-- monsterid: 42 -->""",
            p,
        )
        assertEquals(MonsterDatabase.getById(42)?.name, p.getString("crudeMonster", ""))
    }

    @Test
    fun applyItemDescription_evaluatesRomanCandelabraRule() {
        registerItem(11609, "Roman Candelabra", "552059394")
        injectRules(
            "552059394",
            "Roman Candelabra\tMaximum HP \\+<span style=\"font-family: times new roman\">(.*?)</span>\tromanCandelabraRedCasts=[(roman(\$1)/10)-2]",
        )
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription(
            "552059394",
            """Maximum HP +<span style="font-family: times new roman">XX</span>""",
            p,
        )
        assertEquals(0, p.getInt("romanCandelabraRedCasts", -1))
    }

    @Test
    fun applyItemDescription_firesMultipleActions() {
        registerItem(1, "durable dolphin whistle", "whistle-desc")
        injectRules(
            "whistle-desc",
            "durable dolphin whistle\tused (\\d+) of (\\d+) times? today\t_durableDolphinWhistleUsed=\$1\tseaPoints=\$2",
        )
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription(
            "whistle-desc",
            "You have used 2 of 5 times today",
            p,
        )
        assertEquals("2", p.getString("_durableDolphinWhistleUsed", ""))
        assertEquals("5", p.getString("seaPoints", ""))
    }

    @Test
    fun applyItemDescription_setModsPrefAndOverridesItem() {
        ModifierDatabase.injectForTest(
            "Item",
            "pantogram pants",
            "Lasts Until Rollover",
        )
        registerItem(9574, "pantogram pants", "pantogram-desc")
        injectRules(
            "pantogram-desc",
            "pantogram pants\t\t_pantogramModifier=mods",
        )
        val html = """
            <font color=blue>Muscle +10<br>+60% Meat from Monsters</font>
        """.trimIndent()
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription("pantogram-desc", html, p)
        assertEquals("Muscle: +10, Meat Drop: +60", p.getString("_pantogramModifier", ""))
        val entry = ModifierDatabase.getItem("pantogram pants")
        assertEquals("Muscle: +10, Meat Drop: +60", entry?.modifiers)
    }

    @Test
    fun applyItemDescription_setModsCarriesOverConditionalSkills() {
        ModifierDatabase.injectForTest(
            "Item",
            "latte lovers member's mug",
            """Conditional Skill (Equipped): "Throw Latte on Opponent"""",
        )
        registerItem(1, "latte lovers member's mug", "latte-desc")
        injectRules(
            "latte-desc",
            "latte lovers member's mug\t\tlatteModifier=mods",
        )
        val html = """<font color=blue>+5 Familiar Weight<br>+40% Meat from Monsters</font>"""
        val p = prefs()
        ItemDescriptionConsequenceSync.applyItemDescription("latte-desc", html, p)
        val entry = ModifierDatabase.getItem("latte lovers member's mug")
        assertTrue(entry?.modifiers?.contains("Familiar Weight: +5") == true)
        assertTrue(entry?.modifiers?.contains("Throw Latte on Opponent") == true)
    }

    @Test
    fun parseForTest_loadsModsRulesWithEmptyRegex() {
        registerItem(1, "no hat", "hat-desc")
        val parsed = ItemDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_ITEM	no hat		_noHatModifier=mods
            DESC_ITEM	no hat	has a brim	hatBrim=true
            """.trimIndent(),
        )
        assertEquals(2, parsed["hat-desc"]?.size)
    }

    @Test
    fun emptyRegex_matchesLikeDesktop() {
        assertTrue(Regex("").find("any html") != null)
    }
}

class RomanNumeralsTest {

    @Test
    fun parse_returnsExpectedValues() {
        assertEquals(14, RomanNumerals.parse("XIV"))
        assertEquals(4, RomanNumerals.parse("IV"))
    }

    @Test
    fun modifierExpression_romanFunction() {
        assertEquals(14.0, ModifierExpression.evaluate("[roman(XIV)]", ExpressionContext.EMPTY))
    }
}
