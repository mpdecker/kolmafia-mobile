package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillCosts
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.NumberologyManager
import net.sourceforge.kolmafia.utilities.CharacterEntities
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameRuntimeLibraryAshP919TracksEHTest {

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        EffectDatabase.load()
        SkillDefinitionDatabase.load()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    // ──────────────────────────────────────────────────────────────
    // Track E — Matcher
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase919_createMatcher_findGroupBasic() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("(\\d+)", "abc123def456");
            if (find(m)) {
              print(group(m, 1));
            }
        """.trimIndent())
        assertEquals("123", result)
    }

    @Test
    fun phase919_createMatcher_replaceAll() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("\\d+", "a1b2c3");
            print(replace_all(m, "X"));
        """.trimIndent())
        assertEquals("aXbXcX", result)
    }

    @Test
    fun phase919_createMatcher_replaceFirst() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("\\d+", "a1b2c3");
            print(replace_first(m, "X"));
        """.trimIndent())
        assertEquals("aXb2c3", result)
    }

    @Test
    fun phase919_createMatcher_groupCount() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("(a)(b)(c)", "abc");
            find(m);
            print(group_count(m));
        """.trimIndent())
        assertEquals("3", result)
    }

    @Test
    fun phase919_createMatcher_startEnd() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("\\d+", "abc123def");
            find(m);
            print(start(m));
            print(end(m));
        """.trimIndent())
        assertEquals("3\n6", result)
    }

    @Test
    fun phase919_createMatcher_reset() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            matcher m = create_matcher("\\d+", "abc123");
            find(m);
            reset(m, "xyz789");
            find(m);
            print(group(m));
        """.trimIndent())
        assertEquals("789", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track E — URL encode/decode
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase922_urlEncode_basic() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(url_encode("hello world"));""")
        assertEquals("hello+world", result)
    }

    @Test
    fun phase922_urlEncode_specialChars() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(url_encode("a=b&c=d"));""")
        assertEquals("a%3Db%26c%3Dd", result)
    }

    @Test
    fun phase922_urlDecode_basic() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(url_decode("hello+world"));""")
        assertEquals("hello world", result)
    }

    @Test
    fun phase922_urlDecode_percent() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(url_decode("a%3Db%26c%3Dd"));""")
        assertEquals("a=b&c=d", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track E — Entity encode/decode (unit tests on CharacterEntities)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase923_entityEncode_amp() {
        assertEquals("&amp;", CharacterEntities.escape("&"))
    }

    @Test
    fun phase923_entityEncode_lt_gt() {
        assertEquals("&lt;b&gt;", CharacterEntities.escape("<b>"))
    }

    @Test
    fun phase923_entityDecode_amp() {
        assertEquals("&", CharacterEntities.unescape("&amp;"))
    }

    @Test
    fun phase923_entityDecode_numeric() {
        assertEquals("A", CharacterEntities.unescape("&#65;"))
    }

    @Test
    fun phase923_entityDecode_hex() {
        assertEquals("A", CharacterEntities.unescape("&#x41;"))
    }

    @Test
    fun phase923_entityEncodeDecode_ashFunction() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(entity_decode(entity_encode("a<b>c&d")));""")
        assertEquals("a<b>c&d", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track F — Skill costs
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase929_advCost_hibernate() {
        assertEquals(1, SkillCosts.getAdventureCost(1027))
    }

    @Test
    fun phase929_advCost_normalSkill() {
        assertEquals(0, SkillCosts.getAdventureCost(1))
    }

    @Test
    fun phase930_soulsauceCost() {
        assertEquals(5, SkillCosts.getSoulsauceCost(7182))
        assertEquals(100, SkillCosts.getSoulsauceCost(7184))
    }

    @Test
    fun phase931_thunderCost() {
        assertEquals(40, SkillCosts.getThunderCost(16001))
    }

    @Test
    fun phase932_rainCost() {
        assertEquals(50, SkillCosts.getRainCost(16011))
    }

    @Test
    fun phase933_lightningCost() {
        assertEquals(20, SkillCosts.getLightningCost(16021))
    }

    @Test
    fun phase934_fuelCost() {
        assertEquals(100, SkillCosts.getFuelCost(7286))
    }

    @Test
    fun phase934_hpCost() {
        assertEquals(3, SkillCosts.getHPCost(24020))
        assertEquals(30, SkillCosts.getHPCost(24012))
    }

    @Test
    fun phase934_meatCost() {
        assertEquals(1, SkillCosts.getMeatCost(33023))
        assertEquals(5, SkillCosts.getMeatCost(33002))
    }

    // ──────────────────────────────────────────────────────────────
    // Track H — desc_to_item / desc_to_effect
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase947_descToItem_knownDescId() {
        val descId = ItemDatabase.all().firstOrNull()?.descId
        if (descId != null && descId.isNotBlank()) {
            val item = ItemDatabase.getByDescId(descId)
            assertNotNull(item)
            val lib = GameRuntimeLibrary()
            val result = outputLib(lib, """print(desc_to_item("$descId"));""")
            assertEquals(item.name, result)
        }
    }

    @Test
    fun phase947_descToItem_unknownReturnsNone() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(desc_to_item("zzz_nonexistent_zzz"));""")
        assertEquals("", result)
    }

    @Test
    fun phase947_descToEffect_knownDescId() {
        val descId = EffectDatabase.all().firstOrNull()?.descId
        if (descId != null && descId.isNotBlank()) {
            val effect = EffectDatabase.getByDescId(descId)
            assertNotNull(effect)
            val lib = GameRuntimeLibrary()
            val result = outputLib(lib, """print(desc_to_effect("$descId"));""")
            assertEquals(effect.name, result)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Track H — numberology_prize
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase948_numberologyPrize_zero() {
        assertEquals("0 Meat", NumberologyManager.numberologyPrize(0))
    }

    @Test
    fun phase948_numberologyPrize_ashFunction() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(numberology_prize(0));""")
        assertEquals("0 Meat", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track H — florist_available
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase948_floristAvailable_noPrefs() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val result = outputLib(lib, """print(florist_available());""")
        assertEquals("false", result)
    }

    @Test
    fun phase948_floristAvailable_checked() {
        val lib = GameRuntimeLibrary(preferences = prefs {
            putBoolean("floristFriarChecked", true)
            putBoolean("floristFriarAvailable", true)
        })
        val result = outputLib(lib, """print(florist_available());""")
        assertEquals("true", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track H — auto_attack
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase943_getAutoAttack_default() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(get_auto_attack());""")
        assertEquals("0", result)
    }

    @Test
    fun phase943_getAutoAttack_withCharacter() {
        val char = KoLCharacter()
        char.setAutoAttackAction(42)
        val lib = GameRuntimeLibrary(character = char)
        val result = outputLib(lib, """print(get_auto_attack());""")
        assertEquals("42", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track E — to_wiki_url
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase924_toWikiUrl_basic() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """print(to_wiki_url("Seal Clubber"));""")
        assertEquals("https://wiki.a.kolmafia.us/wiki/Seal_Clubber", result)
    }

    // ──────────────────────────────────────────────────────────────
    // Track E — xpath
    // ──────────────────────────────────────────────────────────────

    @Test
    fun phase920_xpath_matchesBodyText() {
        val lib = GameRuntimeLibrary()
        val result = outputLib(lib, """
            string[int] arr = xpath("<html><body>Hi</body></html>", "//body");
            print(count(arr));
            print(arr[0]);
        """.trimIndent())
        assertEquals("1\n<body>Hi</body>", result)
    }
}
