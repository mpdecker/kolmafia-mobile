package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TCRSAstralDatabaseTest {

    @BeforeTest
    fun setUp() {
        TCRSAstralDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        TCRSAstralDatabase.resetForTest()
    }

    @Test
    fun load_readsBothBundledFiles() = runBlocking {
        TCRSAstralDatabase.load()
        assertTrue(TCRSAstralDatabase.isLoaded)
        assertTrue(TCRSAstralDatabase.petEntryCount >= 800)
        assertTrue(TCRSAstralDatabase.consumableEntryCount >= 160)
    }

    @Test
    fun load_spotChecksKnownPetAndConsumableRows() = runBlocking {
        TCRSAstralDatabase.load()

        val consumable = TCRSAstralDatabase.getConsumableEntry(
            "Accordion Thief",
            "Blender",
            5043,
        )
        assertNotNull(consumable)
        assertEquals(5, consumable.size)
        assertNull(consumable.effectName)

        val effectConsumable = TCRSAstralDatabase.getConsumableEntry(
            "Accordion Thief",
            "Opossum",
            5043,
        )
        assertNotNull(effectConsumable)
        assertEquals(3, effectConsumable.size)
        assertEquals("Cold-Blooded Warm Fuzzies", effectConsumable.effectName)
        assertEquals(20, effectConsumable.effectDuration)

        val petModifiers = TCRSAstralDatabase.getPetModifiers(
            "Accordion Thief",
            "Blender",
            5028,
        )
        assertTrue(petModifiers.contains("Moxie: +10"))
    }

    @Test
    fun parseConsumableTextForTest_parsesSizeOnlyAndEffectRows() {
        val fixture = """
            Accordion Thief	Blender	[5043]astral hot dog	5/
            Accordion Thief	Opossum	[5043]astral hot dog	3/Effect: "Cold-Blooded Warm Fuzzies", Effect Duration: 20

        """.trimIndent()

        val parsed = TCRSAstralDatabase.parseConsumableTextForTest(fixture)
        assertEquals(2, parsed.size)

        val sizeOnly = parsed[TCRSAstralDatabase.AstralKey("Accordion Thief", "Blender", 5043)]
        assertNotNull(sizeOnly)
        assertEquals(5, sizeOnly.size)
        assertNull(sizeOnly.effectName)

        val withEffect = parsed[TCRSAstralDatabase.AstralKey("Accordion Thief", "Opossum", 5043)]
        assertNotNull(withEffect)
        assertEquals(3, withEffect.size)
        assertEquals("Cold-Blooded Warm Fuzzies", withEffect.effectName)
        assertEquals(20, withEffect.effectDuration)
    }

    @Test
    fun parsePetTextForTest_skipsInvalidClassSignAndBracketRows() {
        val fixture = """
            # comment
            Accordion Thief	Blender	[5028]astral bludgeon	Moxie: +10
            Ed the Undying	Blender	[5028]astral bludgeon	ignored
            Accordion Thief	Blender	invalid item token	ignored
            Accordion Thief	Blender	[5042]astral belt	Maximum MP: +50

        """.trimIndent()

        val parsed = TCRSAstralDatabase.parsePetTextForTest(fixture)
        assertEquals(2, parsed.size)
        assertTrue(parsed.containsKey(TCRSAstralDatabase.AstralKey("Accordion Thief", "Blender", 5028)))
        assertTrue(parsed.containsKey(TCRSAstralDatabase.AstralKey("Accordion Thief", "Blender", 5042)))
    }

    @Test
    fun parseConsumableValue_handlesSizeOnlySuffixAndEffectPattern() {
        val sizeOnly = TCRSAstralDatabase.parseConsumableValue("6/")
        assertEquals(6, sizeOnly.size)
        assertNull(sizeOnly.effectName)

        val withEffect = TCRSAstralDatabase.parseConsumableValue(
            """3/Effect: "Weird Flavor", Effect Duration: 15""",
        )
        assertEquals(3, withEffect.size)
        assertEquals("Weird Flavor", withEffect.effectName)
        assertEquals(15, withEffect.effectDuration)
    }

    @Test
    fun lookup_returnsEmptyForInvalidClassSignOrMissingItem() = runBlocking {
        TCRSAstralDatabase.load()

        assertEquals("", TCRSAstralDatabase.getPetModifiers("Ed the Undying", "Blender", 5028))
        assertNull(TCRSAstralDatabase.getConsumableEntry("Ed the Undying", "Blender", 5043))
        assertEquals("", TCRSAstralDatabase.getPetModifiers("Accordion Thief", "Blender", 9999))
        assertNull(TCRSAstralDatabase.getConsumableEntry("Accordion Thief", "Blender", 9999))
        assertFalse(TCRSAstralDatabase.hasPetEntry("Ed the Undying", "Blender", 5028))
        assertFalse(TCRSAstralDatabase.hasConsumableEntry("Ed the Undying", "Blender", 5043))
    }

    @Test
    fun hasEntry_returnsTrueForLoadedRows() = runBlocking {
        TCRSAstralDatabase.load()

        assertTrue(TCRSAstralDatabase.hasPetEntry("Accordion Thief", "Blender", 5028))
        assertTrue(TCRSAstralDatabase.hasConsumableEntry("Accordion Thief", "Blender", 5043))
    }
}
