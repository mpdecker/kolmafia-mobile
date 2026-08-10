package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.concoction.StillsAvailability
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP116Test {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun fakeSkillManager(vararg skillIds: Int): SkillManager {
        val skills = skillIds.map { id ->
            SkillData(id, "Skill $id", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0)
        }
        val json = "{" + skills.joinToString(",") { s ->
            """"${s.id}":{"name":"${s.name}","type":5,"dailylimit":0,"timescast":0,"mpcost":0}"""
        } + "}"
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return SkillManager(client, SkillCastRequest(client), GameEventBus()).also { mgr ->
            runBlocking { mgr.fetchSkills() }
        }
    }

    private fun moxieChar(
        ascensions: String = "5",
        stills: String = "3",
        classId: String = "5",
        path: String = "None",
    ) = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                ascensions = ascensions,
                classId = classId,
                stills = stills,
                path = path,
            ),
        )
    }

    @Test
    fun stillsAvailable_returnsZeroWithoutSkill() {
        val lib = GameRuntimeLibrary(
            character = moxieChar(),
            skillManager = fakeSkillManager(),
            preferences = prefs { setInt("lastGuildStoreOpen", 5) },
        )
        assertEquals("0", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillsAvailable_returnsZeroForNonMoxieClass() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "1", ascensions = "5", stills = "3"),
            )
        }
        val lib = GameRuntimeLibrary(
            character = char,
            skillManager = fakeSkillManager(StillsAvailability.SUPER_COCKTAIL),
            preferences = prefs { setInt("lastGuildStoreOpen", 5) },
        )
        assertEquals("0", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillsAvailable_returnsZeroWithoutGuildStore() {
        val lib = GameRuntimeLibrary(
            character = moxieChar(),
            skillManager = fakeSkillManager(StillsAvailability.MIXOLOGIST),
            preferences = prefs(),
        )
        assertEquals("0", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillsAvailable_sneakyPeteBypassesGuildRequirement() {
        val lib = GameRuntimeLibrary(
            character = moxieChar(path = "Avatar of Sneaky Pete", stills = "4"),
            skillManager = fakeSkillManager(StillsAvailability.SUPER_COCKTAIL),
            preferences = prefs(),
        )
        assertEquals("4", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillsAvailable_readsCountWhenGatesPass() {
        val lib = GameRuntimeLibrary(
            character = moxieChar(stills = "6"),
            skillManager = fakeSkillManager(StillsAvailability.SUPER_COCKTAIL),
            preferences = prefs { setInt("lastGuildStoreOpen", 5) },
        )
        assertEquals("6", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillsAvailable_unknownStillsReturnsZero() {
        val lib = GameRuntimeLibrary(
            character = moxieChar(stills = "-1"),
            skillManager = fakeSkillManager(StillsAvailability.SUPER_COCKTAIL),
            preferences = prefs { setInt("lastGuildStoreOpen", 5) },
        )
        assertEquals("0", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun stillHook_updatesStillsAvailable() {
        val char = moxieChar(stills = "0")
        val lib = GameRuntimeLibrary(
            character = char,
            skillManager = fakeSkillManager(StillsAvailability.SUPER_COCKTAIL),
            preferences = prefs { setInt("lastGuildStoreOpen", 5) },
        )
        lib.processVisitResponseHooks(
            """You stand before a still with 2 bright copper stills.""",
            "https://www.kingdomofloathing.com/shop.php?whichshop=still",
        )
        assertEquals("2", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun haveMushroomPlot_falseWhenPrefMismatch() {
        val p = prefs { setInt("lastMushroomPlot", 3) }
        val lib = GameRuntimeLibrary(
            character = moxieChar(ascensions = "5"),
            preferences = p,
        )
        assertEquals("false", outputLib(lib, """print(have_mushroom_plot());""").trim())
    }

    @Test
    fun haveMushroomPlot_trueWhenPrefMatchesAscension() {
        val p = prefs { setInt("lastMushroomPlot", 5) }
        val lib = GameRuntimeLibrary(
            character = moxieChar(ascensions = "5"),
            preferences = p,
        )
        assertEquals("true", outputLib(lib, """print(have_mushroom_plot());""").trim())
    }

    @Test
    fun knollHook_setsHaveMushroomPlot() {
        val p = prefs()
        val char = moxieChar(ascensions = "9")
        val lib = GameRuntimeLibrary(character = char, preferences = p)
        lib.processVisitResponseHooks(
            """<b>Your Mushroom Plot:</b><p><table><tr><td></td></tr></table>""",
            "https://www.kingdomofloathing.com/knoll_mushrooms.php",
        )
        assertEquals("true", outputLib(lib, """print(have_mushroom_plot());""").trim())
    }

    @Test
    fun revision_phase160() {
        assertEquals("phase390", GameRuntimeLibrary.REVISION)
    }
}
