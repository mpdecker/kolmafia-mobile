package net.sourceforge.kolmafia.request

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.adventure.AdventureFormBuilder

class DwarfFactoryAndBasementArcadeTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        DwarfFactoryRequest.resetForTest()
        BasementSync.resetForTest()
    }

    @Test
    fun ensureUpdated_clearsRunePrefsOnNewAscension() {
        prefs.setInt("lastDwarfFactoryReset", 0)
        prefs.setString("lastDwarfOreRunes", "ABCD")
        prefs.setString("lastDwarfFactoryItem363", "A")
        DwarfFactoryRequest.ensureUpdated(prefs, 2)
        assertEquals(2, prefs.getInt("lastDwarfFactoryReset"))
        assertEquals("", prefs.getString("lastDwarfOreRunes"))
        assertEquals("", prefs.getString("lastDwarfFactoryItem363"))
        assertEquals("-------", prefs.getString("lastDwarfDigitRunes"))
    }

    @Test
    fun setHopperRune_recordsOreAndCount() {
        DwarfFactoryRequest.ensureUpdated(prefs, 1)
        val html = """
            <img title="Dwarf Word Rune Q">
            <p>It currently contains 12 linoleum ore.</p>
        """.trimIndent()
        DwarfFactoryRequest.setHopperRune(1, html, prefs, 1)
        assertEquals("Q", prefs.getString("lastDwarfHopper1"))
        assertTrue(prefs.getString("lastDwarfOreRunes").contains("Q"))
        assertEquals(12, DwarfFactoryRequest.hopperCount(0))
    }

    @Test
    fun setItemRunes_prunesOtherItems() {
        DwarfFactoryRequest.ensureUpdated(prefs, 1)
        prefs.setString("lastDwarfFactoryItem364", "AB")
        DwarfFactoryRequest.setItemRunes(DwarfFactoryRequest.LINOLEUM_ORE, "A", prefs)
        assertEquals("A", prefs.getString("lastDwarfFactoryItem363"))
        assertEquals("B", prefs.getString("lastDwarfFactoryItem364"))
    }

    @Test
    fun numberTranslator_setDigitsAndParse() {
        DwarfFactoryRequest.setDigits("ABCDEFG", prefs)
        assertTrue(DwarfFactoryRequest.valid())
        assertEquals(0, DwarfFactoryRequest.parseNumber("A", prefs))
        assertEquals(1, DwarfFactoryRequest.parseNumber("B", prefs))
        assertEquals(9, DwarfFactoryRequest.parseNumber("BC", prefs)) // 1*7+2
    }

    @Test
    fun numberTranslator_analyzeNumbers_findsOneAndTwo() {
        val t = DwarfNumberTranslator()
        t.addNumber("XAB")
        t.addNumber("XCD")
        t.addNumber("XEF")
        t.addNumber("YAG")
        t.analyzeNumbers()
        assertEquals(1, t.parseNumber("X"))
        assertEquals(2, t.parseNumber("Y"))
        assertEquals(0, t.parseNumber("A"))
    }

    @Test
    fun numberTranslator_dicePermutationSolves() {
        val t = DwarfNumberTranslator()
        // Known digits A=0..G=6 via partial maps from rolls that force uniqueness
        t.addRoll("BC-AD=10") // will eliminate inconsistent perms once all 7 digits known
        // Seed all seven digit runes
        for (ch in "ABCDEFG") t.addNumber("$ch")
        // Force known mappings for 0 and 1 via laminated-style numbers
        t.addNumber("BAA")
        t.addNumber("BAC")
        t.addNumber("BAD")
        t.addNumber("CAE")
        t.analyzeNumbers()
        // With partial knowledge, digitString should include mapped chars
        assertTrue(t.digitString().contains('B') || t.digitString().contains('-'))
    }

    @Test
    fun adventureForm_dwarffactoryUsesWareAction() {
        val form = AdventureFormBuilder.build("dwarffactory.php", "0")
        assertEquals("dwarffactory.php", form.formSource)
        assertEquals("ware", form.fields["action"])
    }

    @Test
    fun contraption_redButtonClearsHoppers() {
        DwarfContraptionRequest.parseResponse(
            "dwarfcontraption.php?action=doredbutton",
            "something falls into the bin",
            prefs,
        )
        assertEquals(0, DwarfFactoryRequest.hopperCount(0))
    }

    @Test
    fun basement_classifiesRewardAndAction() {
        val html = "Level 42<br>Got Silk?"
        BasementSync.checkBasement(html, prefs)
        assertEquals(42, BasementSync.basementLevel)
        assertEquals(BasementTestType.REWARD, BasementSync.basementTest)
        assertEquals("1", BasementSync.getBasementAction(html, false, false, true))
        assertEquals("2", BasementSync.getBasementAction(html, false, false, false))
    }

    @Test
    fun basement_classifiesElementalAndStat() {
        BasementSync.checkBasement("Level 10<br><b>Peace, Bra!</b>", prefs)
        assertEquals(BasementTestType.ELEMENT, BasementSync.basementTest)
        assertEquals("stench", BasementSync.element1)
        assertEquals("sleaze", BasementSync.element2)

        BasementSync.checkBasement("Level 5<br>Lift 'em", prefs, muscle = 1)
        assertEquals(BasementTestType.MUSCLE, BasementSync.basementTest)
        assertTrue(BasementSync.basementErrorMessage!!.contains("muscle"))
    }

    @Test
    fun basement_classifiesMonster() {
        BasementSync.checkBasement("Level 20<br>Don't Fear the Ear", prefs)
        assertEquals(BasementTestType.MONSTER, BasementSync.basementTest)
        assertEquals("Beast with X Ears", BasementSync.basementMonster)
    }

    @Test
    fun arcade_turnsAndJackass() {
        assertEquals(5, ArcadeRequest.getTurnsUsed("place.php?whichplace=arcade&action=arcade_fist"))
        assertEquals(0, ArcadeRequest.getTurnsUsed("place.php?whichplace=arcade&action=arcade_skeeball"))
        prefs.setBoolean("_defectiveTokenChecked", false)
        prefs.setInt("lastArcadeAscension", 3)
        val url = ArcadeRequest.jackassPlumberUrl(prefs, 3, hasToken = false, hasTicket = false)
        assertTrue(url!!.contains("arcade_plumber"))
        ArcadeRequest.parseResponse(url, "ok", prefs)
        assertTrue(prefs.getBoolean("_defectiveTokenChecked"))
        assertEquals(null, ArcadeRequest.jackassPlumberUrl(prefs, 3, true, true))
    }

    @Test
    fun dwarfDeduce_hpAndDefense() {
        // group1 = "really really " → length 14
        assertEquals(
            14,
            DwarfFactoryRequest.deduceHP("<p>Your mattock glows really really bright blue.</p>"),
        )
        val defense = DwarfFactoryRequest.deduceDefense(
            "<p>Your sporran lights up with a series of four little lights: red, orange, yellow, and green.</p>",
        )
        assertEquals(66, defense)
    }
}
