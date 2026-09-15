package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.MonsterPathMaps
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.ShrunkenHeadDatabase
import net.sourceforge.kolmafia.data.WardrobeOMaticDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharSheetSync
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.utilities.PHPRandom

class GameRuntimeLibraryPhase4510Test {

    private fun mockClient() = HttpClient(MockEngine) {
        engine { addHandler { respond("", HttpStatusCode.OK) } }
    }

    @Test
    fun revision_phase4510() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun get_permed_skills_fromSkillManager() {
        val client = mockClient()
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.setPermedSkills(mapOf(19 to true, 15 to false))
        val lib = GameRuntimeLibrary(skillManager = skills, preferences = Preferences(MapSettings()))
        assertEquals("2", outputLib(lib, "print(count(get_permed_skills()));"))
    }

    @Test
    fun charSheet_parsePermedSkills() {
        val html = """
            <a onclick="javascript:poop(&quot;desc_skill.php?whichskill=19&amp;self=true&quot;,&quot;skill&quot;, 350, 300)">Tongue of the Walrus</a> (<b>HP</b>)<br>
            <a onclick="javascript:poop(&quot;desc_skill.php?whichskill=15&amp;self=true&quot;,&quot;skill&quot;, 350, 300)">Softcore Skill</a> (P)<br>
        """.trimIndent()
        val permed = CharSheetSync.parsePermedSkills(html)
        assertEquals(true, permed[19])
        assertEquals(false, permed[15])
    }

    @Test
    fun shrunken_head_zombie_deterministic() {
        val a = ShrunkenHeadDatabase.shrunkenHeadZombie(100, 41)
        val b = ShrunkenHeadDatabase.shrunkenHeadZombie(100, 41)
        assertEquals(a, b)
        assertTrue(a.isNotEmpty())
        kotlinx.coroutines.runBlocking { MonsterDatabase.load() }
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(
            lib,
            "print(count(shrunken_head_zombie(to_monster(\"Knob Goblin King\"), to_path(\"You, Robot\"))));",
        )
        assertTrue((out.toIntOrNull() ?: 0) >= 1)
    }

    @Test
    fun wardrobe_shirt_nonEmptyMods() {
        val clothing = WardrobeOMaticDatabase.shirt(1000, 2)
        assertTrue(clothing.name.isNotBlank())
        assertTrue(clothing.modifiers.size >= 2)
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val slot = EquipmentSlot.SHIRT.ordinal
        val count = outputLib(lib, "print(count(futuristic_wardrobe(1000, $slot, 2)));")
        assertTrue((count.toIntOrNull() ?: 0) >= 2)
    }

    @Test
    fun get_monster_mapping_youRobot() {
        val map = MonsterPathMaps.getMonsterPathMap("You, Robot")
        assertEquals("Boss Bot", map["Boss Bat"])
        assertEquals(null, map["Naughty Sorceress (2)"])
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(
            lib,
            """
            foreach src, dst in get_monster_mapping("You, Robot") {
              if (to_string(src) == "Boss Bat") {
                print(to_string(dst));
              }
            }
            """.trimIndent(),
        )
        assertEquals("Boss Bot", out.trim())
    }

    @Test
    fun outfit_name_with_codpiece_gems_encodesSuffix() {
        val name = EquipmentRequest.outfitNameWithCodpieceGems("My Outfit", listOf(0, 0, 0, 0, 0))
        assertTrue(name.startsWith("My Outfit c=~"))
        val lib = GameRuntimeLibrary(
            character = KoLCharacter(),
            preferences = Preferences(MapSettings()),
        )
        assertTrue(outputLib(lib, """print(outfit_name_with_codpiece_gems("Save Me"));""").contains(" c=~"))
    }

    @Test
    fun phpRandom_shufflePreservesElements() {
        val rand = PHPRandom(42)
        val list = mutableListOf(0, 1, 2, 3, 4)
        rand.shuffle(list)
        assertEquals(setOf(0, 1, 2, 3, 4), list.toSet())
    }

    @Test
    fun candy_for_tier_flags_registered() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertTrue(outputLib(lib, "print(count(candy_for_tier(1, 4)));").toIntOrNull() != null)
    }

    @Test
    fun update_candy_prices_void() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("", outputLib(lib, "update_candy_prices();").trim())
    }
}
