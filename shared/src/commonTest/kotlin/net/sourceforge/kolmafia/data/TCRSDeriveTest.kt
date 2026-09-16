package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.modifiers.StringModifier

class TCRSDeriveTest {

    @BeforeTest
    fun loadData() = runBlocking {
        ItemDatabase.load()
        EffectDatabase.load()
        ModifierDatabase.load()
        ConsumableDatabase.load()
        CafeDatabase.load()
        TCRSStringTables.load()
        TCRSDerive.rebuildEffectPoolForTest()
    }

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
    }

    @Test
    fun seedFor_matchesDesktopFormula() {
        assertEquals(
            133245,
            TCRSDerive.seedFor(418, CharacterClass.SEAL_CLUBBER, ZodiacSign.MONGOOSE),
        )
    }

    @Test
    fun stringTables_preserveOrder() {
        assertTrue(TCRSStringTables.list("Color").isNotEmpty())
        assertTrue(TCRSStringTables.list("Potion Mod").isNotEmpty())
        assertTrue(TCRSStringTables.equipmentModifiers().isNotEmpty())
        assertEquals("red", TCRSStringTables.list("Color").first())
    }

    @Test
    fun derivePotion_sealClubberMongoose_ferrignoElixir() {
        val entry = TCRSDerive.deriveItem(
            CharacterClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            418,
        )
        assertNotNull(entry)
        assertEquals(
            "enhanced quadruple-magnetized spinning maroon jittery Ferrigno's Elixir of Power",
            entry.name,
        )
        assertTrue(entry.modifiers.contains("Effect: \"Dances with Tweedles\""))
        assertTrue(entry.modifiers.contains("Effect Duration: 12"))
    }

    @Test
    fun derivePotion_saucerorVole_spookyPowderOverflowsToTikiTemerity() {
        val entry = TCRSDerive.deriveItem(
            CharacterClass.SAUCEROR,
            ZodiacSign.VOLE,
            1441,
        )
        assertNotNull(entry)
        assertEquals("irradiated altered powder", entry.name)
        assertTrue(entry.modifiers.contains("Effect: \"Tiki Temerity\""))
        assertTrue(entry.modifiers.contains("Effect Duration: 69"))
    }

    @Test
    fun notReRolled_keepsBaseName() {
        val ring = ItemDatabase.getById(10252)
        assertNotNull(ring)
        val entry = TCRSDerive.deriveItem(
            CharacterClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            10252,
        )
        assertNotNull(entry)
        assertEquals(ring.name, entry.name)
    }

    @Test
    fun deriveGeneric_combatItemGetsCosmeticName() {
        val base = ItemDatabase.getItemName(27)
        val entry = TCRSDerive.deriveGeneric(
            CharacterClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            27,
        )
        assertTrue(entry.name.endsWith(base) || entry.name.contains(base))
        assertTrue(entry.name.length >= base.length)
    }

    @Test
    fun deriveAll_populatesItemAndCafeMaps() {
        val count = TCRSDerive.deriveAll(CharacterClass.SEAL_CLUBBER, ZodiacSign.MONGOOSE)
        assertTrue(count > 1000)
        assertTrue(TCRSDatabase.cafeBoozeCount() > 0)
        assertTrue(TCRSDatabase.cafeFoodCount() > 0)
        val elixir = TCRSDatabase.getEntry(418)
        assertNotNull(elixir)
        assertEquals(
            "enhanced quadruple-magnetized spinning maroon jittery Ferrigno's Elixir of Power",
            elixir.name,
        )
    }

    @Test
    fun displayName_modifierIsRecognized() {
        assertEquals("Display Name", StringModifier.DISPLAY_NAME.tag)
    }
}
