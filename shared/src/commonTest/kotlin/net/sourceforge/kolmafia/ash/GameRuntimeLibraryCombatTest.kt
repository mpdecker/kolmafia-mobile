package net.sourceforge.kolmafia.ash

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
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCombatTest {

    @Test
    fun inMultiFight_reflectsAdventureManagerState() {
        val mgr = stubAdventureManager()
        mgr.testSetCombatFlags(inMultiFight = true, fightFollowsChoice = false)
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("true", outputLib(lib, "print(to_string(in_multi_fight()));"))
    }

    @Test
    fun fightFollowsChoice_reflectsAdventureManagerState() {
        val mgr = stubAdventureManager()
        mgr.testSetCombatFlags(inMultiFight = true, fightFollowsChoice = true)
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("true", outputLib(lib, "print(to_string(fight_follows_choice()));"))
    }

    @Test
    fun combatFlags_falseWithoutAdventureManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(in_multi_fight()));"))
        assertEquals("false", outputLib(lib, "print(to_string(fight_follows_choice()));"))
    }

    private fun stubAdventureManager() = net.sourceforge.kolmafia.adventure.AdventureManager(
        adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(HttpClient(MockEngine { respond("") })),
        fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(HttpClient(MockEngine { respond("") })),
        choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(HttpClient(MockEngine { respond("") })),
        characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(HttpClient(MockEngine { respond("") })),
        character = net.sourceforge.kolmafia.character.KoLCharacter(),
        preferences = prefs(),
        eventBus = GameEventBus(),
    )

    @Test
    fun lastMonster_readsFromPref() {
        val p = prefs()
        p.setString(Preferences.LAST_MONSTER, "bunny")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("bunny", outputLib(lib, "print(last_monster());"))
    }

    @Test
    fun lastMonster_emptyWhenNoPrefSet() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(last_monster());"))
    }

    @Test
    fun copiersUsed_returnsSkillTimesCast() {
        val json = """{"1":{"name":"Digitize","type":5,"dailylimit":0,"timescast":7,"mpcost":0}}"""
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val mgr = SkillManager(client, SkillCastRequest(client), GameEventBus())
        runBlocking { mgr.fetchSkills() }
        val lib = GameRuntimeLibrary(skillManager = mgr)
        assertEquals("7",
            outputLib(lib, """print(to_string(copiers_used(to_skill("Digitize"))));"""))
    }

    @Test
    fun copiersUsed_unknownSkill_returnsZero() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("0",
            outputLib(lib, """print(to_string(copiers_used(to_skill("No Such Skill"))));"""))
    }
}
