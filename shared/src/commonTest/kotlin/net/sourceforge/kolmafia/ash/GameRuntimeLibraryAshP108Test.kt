package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP108Test {

    private fun familiarManagerWith(vararg races: FamiliarData): FamiliarManager =
        FamiliarManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ).also {
            it.testSetState(FamiliarState(ownedFamiliars = races.toList()))
        }

    @Test
    fun cliFamiliar_blockedOnAvatarPath() = runBlocking {
        val switchCalls = mutableListOf<String>()
        val fm = object : FamiliarManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            override suspend fun setFamiliar(name: String): Result<Unit> {
                switchCalls.add(name)
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(7, "Biscuit", "Angry Goat", 5, 0, 0),
                    ),
                ),
            )
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Avatar of Boris"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        runLib(lib, """cli_execute("familiar Angry Goat");""")
        assertTrue(switchCalls.isEmpty())
    }

    @Test
    fun enthroneFamiliar_skipsUnusableBeeRaceOnBeecore() = runBlocking {
        var enthroned: String? = null
        val fm = object : FamiliarManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            override suspend fun setEnthroned(name: String): Result<Unit> {
                enthroned = name
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(8, "Barn", "Barrrnacle", 5, 0, 0),
                        FamiliarData(1, "Donkey", "Miniature Donkey", 5, 0, 0),
                    ),
                ),
            )
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Bees Hate You"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(enthrone_familiar(to_familiar("Barrrnacle"))));"""),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(enthrone_familiar(to_familiar("Miniature Donkey"))));"""),
        )
        assertEquals("Miniature Donkey", enthroned)
    }

    @Test
    fun revision_phase155() {
        assertEquals("phase410", GameRuntimeLibrary.REVISION)
    }
}
