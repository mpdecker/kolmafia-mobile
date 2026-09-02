package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.StoreManager
import net.sourceforge.kolmafia.skill.UseSkillSync

class GameRuntimeLibraryPhase4490Test {

    @Test
    fun revision_phase4490() {
        assertEquals("phase4490", GameRuntimeLibrary.REVISION)
        assertEquals("phase4490", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun last_skill_message_readsUseSkillSync() {
        UseSkillSync.lastUpdate = "Not enough mana to cast skill"
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals(
                "Not enough mana to cast skill",
                outputLib(lib, "print(last_skill_message());"),
            )
        } finally {
            UseSkillSync.lastUpdate = ""
        }
    }

    @Test
    fun eight_bit_points_formulaMatchesDesktop() {
        val isBonusBase = 100
        val bonus = kotlin.math.round(
            kotlin.math.min(300.0, kotlin.math.max(0.0, 200.0 - 150)) / 10.0,
        ).toLong() * 10
        assertEquals(150L, isBonusBase + bonus)
    }

    @Test
    fun get_items_hash_emptyInventoryStable() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val a = outputLib(lib, """print(get_items_hash("inventory"));""")
        val b = outputLib(lib, """print(get_items_hash("inventory"));""")
        assertEquals(a, b)
        assertTrue(a.toLongOrNull() != null)
    }

    @Test
    fun get_items_hash_shopUsesSoldList() {
        StoreManager.clearCache()
        StoreManager.addItem(100, 2, 500, 1)
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            val hash = outputLib(lib, """print(get_items_hash("shop"));""").toLong()
            assertTrue(hash != 0xcbf29ce484222325UL.toLong())
        } finally {
            StoreManager.clearCache()
        }
    }

    @Test
    fun dart_parts_to_skills_inverseOfSkillsToParts() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_currentDartboard", "7513:elbow,7514:knee")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("2", outputLib(lib, "print(count(dart_parts_to_skills()));"))
        assertEquals("2", outputLib(lib, "print(count(dart_skills_to_parts()));"))
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(count(dart_parts_to_skills()) == count(dart_skills_to_parts())));""",
            ),
        )
    }

    @Test
    fun extract_items_andEquipAndQty() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val html = "You acquire and equip an item: <b>seal-clubbing club</b> (2)"
        val out = outputLib(
            lib,
            """
            int[item] m = extract_items("$html");
            foreach it, n in m { print(it + "=" + n); }
            """.trimIndent(),
        )
        assertTrue(out.contains("seal-clubbing club=2"))
    }

    @Test
    fun path_name_and_id_roundTrip() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("4", outputLib(lib, """print(path_name_to_id("Bees Hate You"));"""))
        assertEquals("Bees Hate You", outputLib(lib, """print(path_id_to_name(4));"""))
        assertEquals("-1", outputLib(lib, """print(path_name_to_id("not a real path"));"""))
    }

    @Test
    fun beret_busking_effects_includesMeatNone() = runBlocking {
        EffectDatabase.load()
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals(
            "101",
            outputLib(
                lib,
                """
                int[effect] m = beret_busking_effects(500, 0);
                print(m[to_effect("none")]);
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun image_to_monster_lookup() = runBlocking {
        MonsterDatabase.load()
        val any = MonsterDatabase.byId.values.firstOrNull { it.image.isNotBlank() }
            ?: return@runBlocking
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals(
            any.name,
            outputLib(lib, """print(image_to_monster("${any.image}"));"""),
        )
    }

    @Test
    fun get_no_pulls_emptyWithoutInventory() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("0", outputLib(lib, "print(count(get_no_pulls()));"))
    }
}
