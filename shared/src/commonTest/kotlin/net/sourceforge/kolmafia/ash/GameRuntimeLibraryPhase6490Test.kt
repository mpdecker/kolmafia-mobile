package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.utilities.SimpleXPath

/**
 * Focused XLIII coverage (phases 6431–6490 Tracks A–B + E–F leftovers).
 * Revision bump deferred to parent (phase6490).
 */
class GameRuntimeLibraryPhase6490Test {

    // ── Tracks A–B (6431–6450) ──────────────────────────────────────────────

    @Test
    fun expected_damage_appliesTrackerAttackModifier() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6490 slug",
                id = 64901,
                image = "x.gif",
                attack = 40,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
        try {
            val char = KoLCharacter().also {
                it.updateFromApiResponse(
                    CharacterApiResponse(buffedmox = "10", buffedmus = "10", classId = "5"),
                )
            }
            val lib = GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                character = char,
            )
            val before = outputLib(lib, "print(expected_damage());").trim().toInt()
            MonsterStatusTracker.lowerMonsterAttack(20)
            val after = outputLib(lib, "print(expected_damage());").trim().toInt()
            assertTrue(after < before, "delevel should reduce expected_damage ($after !< $before)")
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun will_usually_dodge_respectsTrackerDelevel() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6490 bruiser",
                id = 64902,
                image = "x.gif",
                attack = 30,
                defense = 30,
                hp = 50,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
        try {
            val char = KoLCharacter().also {
                it.updateFromApiResponse(
                    CharacterApiResponse(buffedmox = "30", buffedmus = "10"),
                )
            }
            val lib = GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                character = char,
            )
            assertEquals("false", outputLib(lib, "print(to_string(will_usually_dodge()));").trim())
            MonsterStatusTracker.lowerMonsterAttack(10)
            assertEquals("true", outputLib(lib, "print(to_string(will_usually_dodge()));").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun will_usually_miss_respectsTrackerDefenseModifier() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6490 dodger",
                id = 64903,
                image = "x.gif",
                attack = 10,
                defense = 40,
                hp = 50,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
        try {
            val char = KoLCharacter().also {
                it.updateFromApiResponse(
                    CharacterApiResponse(buffedmox = "10", buffedmus = "40"),
                )
            }
            val lib = GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                character = char,
            )
            assertEquals("true", outputLib(lib, "print(to_string(will_usually_miss()));").trim())
            MonsterStatusTracker.lowerMonsterDefense(20)
            assertEquals("false", outputLib(lib, "print(to_string(will_usually_miss()));").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun item_drops_fractionalConditionalRate() = runBlocking {
        val db = GameDatabase()
        db.load()
        val sandworm = MonsterDatabase.getByName("giant sandworm")
        assertTrue(sandworm != null, "giant sandworm missing from monsters.txt")
        val drop = sandworm!!.drops.firstOrNull { it.itemName.equals("spice melange", ignoreCase = true) }
        assertTrue(drop != null, "spice melange drop missing")
        assertEquals(0.1, drop!!.dropRate, 0.0001)
        assertEquals('c', drop.prefix)

        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(
            lib,
            """print(to_string(item_drops(to_monster("giant sandworm"))[to_item("spice melange")]));""",
        ).trim()
        assertEquals("0.1", out)
    }

    @Test
    fun item_drops_multiDropStripsCountPrefix() = runBlocking {
        val db = GameDatabase()
        db.load()
        val king = MonsterDatabase.getByName("Knob Goblin King")
        assertTrue(king != null)
        val drop = king!!.drops.firstOrNull { it.itemName.equals("dense meat stack", ignoreCase = true) }
        assertTrue(drop != null, "dense meat stack multi-drop missing or unparsed")
        assertEquals('m', drop!!.prefix)
        assertEquals(100.0, drop.dropRate, 0.0001)
        assertEquals("2", drop.itemCount)

        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(
            lib,
            """print(to_string(item_drops(to_monster("Knob Goblin King"))[to_item("dense meat stack")]));""",
        ).trim()
        assertEquals("100.0", out)
    }

    @Test
    fun jump_chance_threeArgMlOnlyAffectsInitiative() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val atZero = CombatAdjustment.jumpChance(mosquito, 0, initMl = 0, attackMl = 0, baseMainstat = 0)
        val atFortyInit = CombatAdjustment.jumpChance(mosquito, 0, initMl = 40, attackMl = 0, baseMainstat = 0)
        val atFortyBoth = CombatAdjustment.jumpChance(mosquito, 0, initMl = 40, attackMl = 40, baseMainstat = 0)
        assertTrue(atFortyInit < atZero)
        assertTrue(atFortyBoth <= atFortyInit)

        val lib = GameRuntimeLibrary(gameDatabase = db)
        val ash = outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 40));""").trim()
        assertEquals(atFortyInit.toString(), ash)
    }

    @Test
    fun meat_drop_trackerBrokeOverlay() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6490 broke",
                id = 64904,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 10,
                initiative = 0,
                meatDrop = 100,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("broke"),
            ),
            listOf("broke"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals("5", outputLib(lib, "print(meat_drop());").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    // ── Tracks E–F leftovers (merged; do not overwrite) ─────────────────────

    @Test
    fun thrallModifier_modifierType_overload_registers() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """print(to_string(numeric_modifier(to_thrall("Pasta Thrall"), to_modifier("Meat Drop"))));""",
        )
        assertTrue(out.trim().isNotEmpty())
    }

    @Test
    fun fileToMap_compactThreeArg_roundTrip() {
        val file = "xliii_compact_map.txt"
        UserDataFileIO.writeText(file, "alpha\t1\nbeta\t2\n")
        val out = outputLib(
            GameRuntimeLibrary(),
            """
            int [string] m;
            boolean ok = file_to_map("$file", m, true);
            print(ok);
            print(m["alpha"]);
            print(m["beta"]);
            boolean wrote = map_to_file(m, "${file}_out", true);
            print(wrote);
            """.trimIndent(),
        )
        assertTrue(out.contains("true"), out)
        assertTrue(out.contains("1"), out)
        assertTrue(out.contains("2"), out)
    }

    @Test
    fun sessionLogs_zeroDays_emptyAggregate() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """
            string [int] logs = session_logs(0);
            print(count(logs));
            """.trimIndent(),
        )
        assertEquals("0", out.trim())
    }

    @Test
    fun sessionLogs_negativeDays_throws() {
        assertFailsWith<ScriptException> {
            outputLib(GameRuntimeLibrary(), """session_logs(-1);""")
        }
    }

    @Test
    fun formFields_duplicateKeysAndBareKey() {
        val fields = AggregateValue(AggregateType(AshType.STRING, AshType.STRING))
        parseQueryFormFields("choice.php?pwd=abc&pwd=def&flag", fields)
        assertEquals("abc", fields[AshValue.of("pwd")].toString())
        assertEquals("def", fields[AshValue.of("pwd_")].toString())
        assertEquals("", fields[AshValue.of("flag")].toString())
    }

    @Test
    fun xpath_childText_and_followingSibling_live() {
        val html = """<div><b>Label</b><span>Value</span></div>"""
        assertEquals(listOf("Label"), SimpleXPath.evaluate(html, "//b/text()"))
        assertEquals(listOf("Value"), SimpleXPath.evaluate(html, "//b/following-sibling::span/text()"))
    }
}
