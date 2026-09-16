package net.sourceforge.kolmafia.session

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.TCRSDatabase

class TcrsCliManagerDeriveTest {

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
    }

    @Test
    fun derive_requiresTcrsPath() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Testy",
                    classId = "1",
                    sign = "Mongoose",
                    path = "None",
                ),
            )
        }
        val manager = TcrsCliManager(character = char)
        assertEquals(
            "You are not in a Two Crazy Random Summer run",
            manager.derive(null),
        )
    }

    @Test
    fun helpTokens_includeIntrospectAndUpdate() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Testy",
                    classId = "1",
                    sign = "Mongoose",
                    path = "Two Crazy Random Summer",
                ),
            )
        }
        val manager = TcrsCliManager(character = char)
        val status = manager.status().joinToString("\n")
        assertTrue(status.contains("TCRS path: true"))
        assertTrue(status.contains("Seal Clubber"))
        assertTrue(status.contains("Mongoose"))
    }
}
