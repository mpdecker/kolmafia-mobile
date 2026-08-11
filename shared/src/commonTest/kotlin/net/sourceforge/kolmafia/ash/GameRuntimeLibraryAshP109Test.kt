package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GameRuntimeLibraryAshP109Test {

    private fun characterWithResources(): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    path = "standard",
                    fury = "4",
                    soulsauce = "50",
                    pp = "12",
                    ppmax = "20",
                    thunder = "30",
                    rain = "40",
                    lightning = "50",
                    robonenergy = "100",
                    robonscraps = "25",
                ),
            )
        }

    @Test
    fun characterResourceGetters_readFromCharacterState() = runBlocking {
        val lib = GameRuntimeLibrary(character = characterWithResources())
        assertEquals("4", outputLib(lib, """print(to_string(my_fury()));""").trim())
        assertEquals("50", outputLib(lib, """print(to_string(my_soulsauce()));""").trim())
        assertEquals("12", outputLib(lib, """print(to_string(my_pp()));""").trim())
        assertEquals("20", outputLib(lib, """print(to_string(my_maxpp()));""").trim())
        assertEquals("30", outputLib(lib, """print(to_string(my_thunder()));""").trim())
        assertEquals("40", outputLib(lib, """print(to_string(my_rain()));""").trim())
        assertEquals("50", outputLib(lib, """print(to_string(my_lightning()));""").trim())
        assertEquals("100", outputLib(lib, """print(to_string(my_robot_energy()));""").trim())
        assertEquals("25", outputLib(lib, """print(to_string(my_robot_scraps()));""").trim())
    }

    @Test
    fun myPathId_returnsAscensionPathId() {
        val lib = GameRuntimeLibrary(
            character = KoLCharacter().also {
                it.updateFromApiResponse(CharacterApiResponse(path = "standard"))
            },
        )
        assertEquals(
            AscensionPath.STANDARD.pathId.toString(),
            outputLib(lib, """print(to_string(my_path_id()));""").trim(),
        )
    }

    @Test
    fun nameAndToInt_useDatabaseAndResolvers() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)

        assertEquals("seal tooth", outputLib(lib, """print(name(to_item("seal tooth")));""").trim())
        assertEquals("Seal Clubber", outputLib(lib, """print(name(to_class("seal clubber")));""").trim())

        val classInt = outputLib(lib, """print(to_string(to_int(to_class("Seal Clubber"))));""").trim()
        assertEquals(CharacterClass.SEAL_CLUBBER.id.toString(), classInt)

        val hashStub = "Seal Clubber".hashCode().toLong().let { if (it < 0) -it else it }
        assertNotEquals(hashStub.toString(), classInt)
    }

    @Test
    fun revision_phase156() {
        assertEquals("phase410", GameRuntimeLibrary.REVISION)
    }
}
