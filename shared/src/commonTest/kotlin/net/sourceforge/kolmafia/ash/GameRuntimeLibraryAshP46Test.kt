package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP46Test {

    @Test
    fun willUsuallyDodge_withHighMoxie() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "40", buffedmus = "10"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        val expected = CombatAdjustment.willUsuallyDodge(mosquito, 40, 0)
        assertEquals(true, expected)
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(to_string(will_usually_dodge()));""").trim(),
        )
    }

    @Test
    fun willUsuallyMiss_withLowHitStat() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "10", buffedmus = "10"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        val hit = CombatAdjustment.buffedHitStat(char.state.value, lib.buildCurrentModifiers(), null)
        val expected = CombatAdjustment.willUsuallyMiss(mosquito, hit, 0)
        assertEquals(true, expected)
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(to_string(will_usually_miss()));""").trim(),
        )
    }

    @Test
    fun willUsually_emptyLastMonster_returnsFalse() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("false", outputLib(lib, """print(to_string(will_usually_dodge()));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(will_usually_miss()));""").trim())
    }

    @Test
    fun currentHitStat_moxWeaponVsMelee() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.WEAPON, "airblaster gun")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals("Moxie", outputLib(lib, """print(current_hit_stat());""").trim())

        char.updateEquipment(EquipmentSlot.WEAPON, "adobe adze")
        assertEquals("Muscle", outputLib(lib, """print(current_hit_stat());""").trim())
    }

    @Test
    fun buffedHitStat_attacksCantMiss() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmus = "20", buffedmox = "20"))
        }
        char.updateEquipment(EquipmentSlot.WEAPON, "legendary seal-clubbing club")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            Int.MAX_VALUE.toString(),
            outputLib(lib, """print(buffed_hit_stat());""").trim(),
        )
    }

    @Test
    fun buffedHitStat_usesBuffedMuscleForMelee() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmus = "55", buffedmox = "12"))
        }
        char.updateEquipment(EquipmentSlot.WEAPON, "adobe adze")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        val expected = CombatAdjustment.buffedHitStat(
            char.state.value,
            lib.buildCurrentModifiers(),
            "adobe adze",
        )
        assertEquals(expected.toString(), outputLib(lib, """print(buffed_hit_stat());""").trim())
    }
}
