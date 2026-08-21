package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.GrandpaRequest

class GameRuntimeLibraryAshP525Test {

    @Test
    fun revision_phase525() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun grandpa_eel_recordsTopic() {
        val topics = mutableListOf<String>()
        val fake = object : GrandpaRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun ask(
                topic: String,
                preferences: Preferences?,
                questDatabase: QuestDatabase?,
            ): Result<String> {
                topics += topic
                return Result.success("ok")
            }
        }
        val out = outputLib(
            GameRuntimeLibrary(grandpaRequest = fake),
            """cli_execute("grandpa eel");""",
        )
        assertEquals(listOf("eel"), topics)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun grandpa_unequipped_printsError() {
        val fake = object : GrandpaRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun ask(
                topic: String,
                preferences: Preferences?,
                questDatabase: QuestDatabase?,
            ): Result<String> = Result.failure(
                IllegalStateException("You're not equipped to visit the Sea Monkees."),
            )
        }
        val out = outputLib(
            GameRuntimeLibrary(grandpaRequest = fake),
            """cli_execute("grandpa eel");""",
        )
        assertTrue(out.contains("You're not equipped to visit the Sea Monkees."))
    }
}
