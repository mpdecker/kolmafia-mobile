package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP39Test {

    @Test
    fun monsterLevelAdjustment_includesRaincoreWater() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "Dreadsylvanian Village")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = AscensionPath.HEAVY_RAINS.apiName),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        // Village indoor + stat≥40 → waterLevel 4 → ML += 40
        assertEquals("40", outputLib(lib, """print(monster_level_adjustment());""").trim())
    }

    @Test
    fun damageAbsorptionPercent_fromEquippedItem() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "balloon helmet")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals("20", outputLib(lib, """print(raw_damage_absorption());""").trim())
        val percent = outputLib(lib, """print(to_string(damage_absorption_percent()));""").trim().toDouble()
        assertEquals(CombatAdjustment.damageAbsorptionPercent(20), percent, absoluteTolerance = 0.0001)
    }

    @Test
    fun damageReduction_fromEquippedItem() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "bark beret")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals("4", outputLib(lib, """print(damage_reduction());""").trim())
    }

    @Test
    fun elementalResistance_elementUsesByLevelFormula() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Marmot", classId = "5"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Marmot: Cold Resistance +1 → 10%
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(elemental_resistance(to_element("cold"))));""").trim(),
        )
    }

    @Test
    fun elementalResistance_monsterUsesAttackElement() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Marmot", classId = "5"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // APS attacks with spooky; Marmot grants Spooky Resistance +1 → 10%
        assertEquals(
            "10.0",
            outputLib(
                lib,
                """print(to_string(elemental_resistance(to_monster("ancient protector spirit"))));""",
            ).trim(),
        )
    }

    @Test
    fun expectedDamage_usesAttackMoxieAndLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "5", buffedmox = "0", mox = "0"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        val expected = CombatAdjustment.expectedDamage(
            mosquito,
            char.state.value,
            lib.buildCurrentModifiers(),
        )
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(expected_damage());""").trim(),
        )
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(expected_damage(to_monster("huge mosquito")));""").trim(),
        )
        assertTrue(expected > 0)
        // base = atk + atk/4; DA=0 → absorb 1.1
        val base = mosquito.attack + mosquito.attack / 4
        assertEquals(kotlin.math.ceil(base * 1.1).toInt(), expected)
    }

    @Test
    fun weightAndManaWrappers_returnInts() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(weight_adjustment());""").trim())
        assertEquals("0", outputLib(lib, """print(mana_cost_modifier());""").trim())
        assertEquals("0", outputLib(lib, """print(combat_mana_cost_modifier());""").trim())
        assertEquals("0.0", outputLib(lib, """print(to_string(combat_rate_modifier()));""").trim())
    }
}
