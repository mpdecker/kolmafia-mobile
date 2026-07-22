package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class DescriptionConsequenceRegistryTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
        EffectDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        DescriptionConsequenceRegistry.resetForTest()
    }

    private fun registerFixtures() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9270,
                name = "no hat",
                descId = "887807812",
                image = "nohat.gif",
                primaryUse = ItemPrimaryUse.HAT,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 9493,
                name = "Kremlin's Greatest Briefcase",
                descId = "311743898",
                image = "kgbcase.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        EffectDatabase.registerForTest(
            EffectData(
                id = 1,
                name = "Buzzed on Distillate",
                image = "buzzed.gif",
                descId = "buzzed-desc",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
            ),
        )
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 42,
                name = "Banishing Shout",
                image = "shout.gif",
                tags = emptySet(),
                mpCost = 10,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    @Test
    fun parse_buildsUrlsInFileOrderIncludingEmptyRegexRows() {
        registerFixtures()
        val text = buildString {
            appendLine("DESC_ITEM\tno hat\t\t_noHatModifier=mods")
            appendLine("DESC_EFFECT\tBuzzed on Distillate\t\\+(\\d+)%\tcurrentDistillateMods=mods")
            appendLine("DESC_SKILL\tBanishing Shout\tcurrently banished:<br>([^<]+)\tbanishingShoutMonsters=\$1")
            append("DESC_ITEM\tKremlin's Greatest Briefcase\t\t")
        }
        val urls = DescriptionConsequenceRegistry.parseForTest(text)
        assertEquals(
            listOf(
                "desc_item.php?whichitem=887807812",
                "desc_effect.php?whicheffect=buzzed-desc",
                "desc_skill.php?whichskill=42&self=true",
                "desc_item.php?whichitem=311743898",
            ),
            urls,
        )
    }

    @Test
    fun urlForDay_wrapsWithModulo() {
        DescriptionConsequenceRegistry.injectForTest(
            listOf(
                "desc_item.php?whichitem=a",
                "desc_item.php?whichitem=b",
                "desc_item.php?whichitem=c",
            ),
        )
        assertEquals("desc_item.php?whichitem=a", DescriptionConsequenceRegistry.urlForDay(0))
        assertEquals("desc_item.php?whichitem=b", DescriptionConsequenceRegistry.urlForDay(1))
        assertEquals("desc_item.php?whichitem=c", DescriptionConsequenceRegistry.urlForDay(2))
        assertEquals("desc_item.php?whichitem=a", DescriptionConsequenceRegistry.urlForDay(3))
    }

    @Test
    fun urlForDay_handlesNegativeDayDifference() {
        DescriptionConsequenceRegistry.injectForTest(
            listOf("desc_item.php?whichitem=a", "desc_item.php?whichitem=b"),
        )
        assertEquals("desc_item.php?whichitem=b", DescriptionConsequenceRegistry.urlForDay(-1))
    }

    @Test
    fun urlForDay_returnsNullForEmptyCatalog() {
        DescriptionConsequenceRegistry.injectForTest(emptyList())
        assertNull(DescriptionConsequenceRegistry.urlForDay(0))
    }

    @Test
    fun parse_skipsUnknownSpecs() {
        val text = "DESC_ITEM	unknown item	foo=bar"
        assertTrue(DescriptionConsequenceRegistry.parseForTest(text).isEmpty())
    }
}
