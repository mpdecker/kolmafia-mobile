package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP22Test {

    @Test
    fun currentNumericModifier_returnsEquippedItemBonus() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.ACC1, "Jarlsberg's earring")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(numeric_modifier("Mysticality")));"""),
        )
    }

    @Test
    fun currentNumericModifier_returnsSignBonus() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(sign = "Marmot"),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier("Cold Resistance")));"""),
        )
    }
}
