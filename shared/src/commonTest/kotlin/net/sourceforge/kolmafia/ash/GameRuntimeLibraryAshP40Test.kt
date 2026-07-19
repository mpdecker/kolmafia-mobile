package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP40Test {

    @Test
    fun meatDropModifier_fromEquippedItem() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "ancient turtle shell helmet")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        val expected = CombatAdjustment.meatDropModifier(lib.buildCurrentModifiers())
        assertEquals(10.0, expected, absoluteTolerance = 0.0001)
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(meat_drop_modifier()));""").trim(),
        )
    }

    @Test
    fun itemDropModifier_fromEquippedItem() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "bounty-hunting helmet")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        val expected = CombatAdjustment.itemDropModifier(lib.buildCurrentModifiers())
        assertEquals(20.0, expected, absoluteTolerance = 0.0001)
        assertEquals(
            "20.0",
            outputLib(lib, """print(to_string(item_drop_modifier()));""").trim(),
        )
    }

    @Test
    fun initiativeModifier_fromEquippedItem() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "8-billed baseball cap")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        val expected = CombatAdjustment.initiativeModifier(lib.buildCurrentModifiers())
        assertEquals(30.0, expected, absoluteTolerance = 0.0001)
        assertEquals(
            "30.0",
            outputLib(lib, """print(to_string(initiative_modifier()));""").trim(),
        )
    }

    @Test
    fun experienceBonus_picksPrimeStatMyst() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "3")) // Pastamancer
        }
        char.updateEquipment(EquipmentSlot.HAT, "googly-ball hat")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "3.0",
            outputLib(lib, """print(to_string(experience_bonus()));""").trim(),
        )
    }

    @Test
    fun experienceBonus_picksPrimeStatMuscle() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "1")) // Seal Clubber
        }
        char.updateEquipment(EquipmentSlot.HAT, "googly-star hat")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "3.0",
            outputLib(lib, """print(to_string(experience_bonus()));""").trim(),
        )
    }

    @Test
    fun experienceBonus_picksPrimeStatMoxie() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "5")) // Disco Bandit
        }
        char.updateEquipment(EquipmentSlot.HAT, "googly-heart hat")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "3.0",
            outputLib(lib, """print(to_string(experience_bonus()));""").trim(),
        )
    }

    @Test
    fun experienceBonus_ignoresOffPrimeStatXp() = runBlocking {
        val db = GameDatabase()
        db.load()
        // Seal Clubber (Muscle) wearing Myst XP hat → experience_bonus is 0
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "1"))
        }
        char.updateEquipment(EquipmentSlot.HAT, "googly-ball hat")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "0.0",
            outputLib(lib, """print(to_string(experience_bonus()));""").trim(),
        )
    }
}
