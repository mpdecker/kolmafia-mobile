package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class MoodEditMoodCliTest {

    @BeforeTest
    fun setUp() {
        MoodRemovalKnownSources.clear()
        runBlocking { EffectDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        MoodRemovalKnownSources.clear()
    }

    @Test
    fun editMoodList_printsBuffAndRemovalLines() {
        val prefs = prefs()
        val (manager, lib) = buildFixture(prefs)
        manager.activeMood = Mood(
            "run",
            triggers = listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.LOSE_EFFECT,
                    effectId = 2,
                    effectName = "Sleepy",
                    action = "use 1 decorative fountain",
                ),
            ),
        )
        val output = outputLib(lib, """cli_execute("editmood list");""")
        assertEquals(
            "Effect 10 => cast Skill 200\nlose_effect Sleepy => use 1 decorative fountain",
            output,
        )
    }

    @Test
    fun editMoodClear_clearsBuffAndRemovalTriggers() {
        val prefs = prefs()
        val (manager, lib) = buildFixture(prefs)
        manager.addMoodToLibrary(
            Mood(
                "run",
                triggers = listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
                removalTriggers = listOf(
                    MoodRemovalTrigger(
                        type = MoodRemovalTriggerType.LOSE_EFFECT,
                        effectId = 2,
                        effectName = "Sleepy",
                        action = "uneffect Sleepy",
                    ),
                ),
            ),
        )
        manager.activeMood = manager.moodLibrary["run"]
        outputLib(lib, """cli_execute("editmood clear");""")
        assertTrue(manager.activeMood?.triggers?.isEmpty() == true)
        assertTrue(manager.activeMood?.removalTriggers?.isEmpty() == true)
        assertTrue(manager.moodLibrary["run"]?.triggers?.isEmpty() == true)
        assertTrue(manager.moodLibrary["run"]?.removalTriggers?.isEmpty() == true)
    }

    @Test
    fun editMoodLoseEffect_addsDefaultActionTrigger() {
        val prefs = prefs()
        val (manager, lib) = buildFixture(prefs)
        manager.addMoodToLibrary(Mood("run"))
        manager.activeMood = manager.moodLibrary["run"]
        val output = outputLib(lib, """cli_execute("editmood lose_effect, Sleepy");""")
        assertTrue(output.contains("Set mood trigger: lose_effect Sleepy => use 1 decorative fountain"))
        assertEquals("use 1 decorative fountain", manager.activeMood?.removalTriggers?.single()?.action)
    }

    @Test
    fun editMoodGainEffect_storesExplicitAction() {
        val prefs = prefs()
        val (manager, lib) = buildFixture(prefs)
        manager.addMoodToLibrary(Mood("run"))
        manager.activeMood = manager.moodLibrary["run"]
        val output = outputLib(lib, """cli_execute("editmood gain_effect, Sleepy, cast Skill 200");""")
        assertTrue(output.contains("Set mood trigger: gain_effect Sleepy => cast Skill 200"))
        assertEquals("cast Skill 200", manager.activeMood?.removalTriggers?.single()?.action)
    }

    @Test
    fun triggerList_aliasMatchesEditMoodList() {
        val prefs = prefs()
        val (manager, lib) = buildFixture(prefs)
        manager.activeMood = Mood(
            "run",
            removalTriggers = listOf(
                MoodRemovalTrigger(
                    type = MoodRemovalTriggerType.UNCONDITIONAL,
                    effectId = 0,
                    effectName = "",
                    action = "rest",
                ),
            ),
        )
        val output = outputLib(lib, """cli_execute("trigger list");""")
        assertEquals("unconditional => rest", output)
    }

    @Test
    fun saveAsMood_runsMinimalSetAndPersists() {
        val prefs = prefs()
        val skills = moodSkillManager()
        val manager = MoodManager(skills, prefs)
        manager.addMoodToLibrary(Mood("run"))
        manager.activeMood = manager.moodLibrary["run"]

        val effectsJson = """{"2":{"name":"Sleepy","duration":5}}"""
        val client = HttpClient(MockEngine {
            respond(effectsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectManager = EffectManager(client, GameEventBus())
        runBlocking { effectManager.fetchEffects() }

        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(mp = "500", mpmax = "500"))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = manager,
            skillManager = skills,
            effectManager = effectManager,
            preferences = prefs,
        )
        outputLib(lib, """cli_execute("save as mood");""")
        assertEquals("use 1 decorative fountain", manager.activeMood?.removalTriggers?.single()?.action)
        assertTrue(prefs.getString("moodRemovalTriggers_run", "").contains("Sleepy"))
    }

    @Test
    fun parseParameters_defaultsUnknownTypeToLoseEffect() {
        val parsed = EditMoodCommandParser.parseParameters("Sleepy, cast X")
        assertEquals(Triple("lose_effect", "Sleepy", "cast X"), parsed)
    }

    private fun buildFixture(prefs: Preferences): Pair<MoodManager, GameRuntimeLibrary> {
        val skills = moodSkillManager()
        val manager = MoodManager(skills, prefs)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(mp = "500", mpmax = "500"))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = manager,
            skillManager = skills,
            preferences = prefs,
        )
        return manager to lib
    }

    private fun prefs(): Preferences {
        val settings = MapSettings()
        settings.putBoolean(Preferences.AUTO_BUFF, true)
        return Preferences(settings)
    }

    private fun moodSkillManager(): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }
}
