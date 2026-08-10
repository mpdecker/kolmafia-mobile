package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class MoodCliCommandTest {

    @Test
    fun moodList_printsActiveTriggerLines() {
        val cast = mutableListOf<Int>()
        val prefs = prefs()
        val (manager, lib) = buildFixture(cast, prefs)
        manager.activeMood = Mood(
            "test",
            listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val output = outputLib(lib, """cli_execute("mood list");""")
        assertEquals("Effect 10 => cast Skill 200", output)
    }

    @Test
    fun moodListall_printsSortedLibraryNames() {
        val cast = mutableListOf<Int>()
        val prefs = prefs()
        val (manager, lib) = buildFixture(cast, prefs)
        manager.addMoodToLibrary(Mood("zebra", emptyList()))
        manager.addMoodToLibrary(Mood("alpha", emptyList()))
        val output = outputLib(lib, """cli_execute("mood listall");""")
        assertEquals("alpha\nzebra", output)
    }

    @Test
    fun moodClear_emptiesActiveTriggers() {
        val cast = mutableListOf<Int>()
        val prefs = prefs()
        val (manager, lib) = buildFixture(cast, prefs)
        val mood = Mood("test", listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)))
        manager.addMoodToLibrary(mood)
        manager.activeMood = mood
        outputLib(lib, """cli_execute("mood clear");""")
        assertTrue(manager.activeMood?.triggers?.isEmpty() == true)
        assertTrue(manager.moodLibrary["test"]?.triggers?.isEmpty() == true)
    }

    @Test
    fun moodName_setsActiveMoodWithoutExecuting() {
        val cast = mutableListOf<Int>()
        val prefs = prefs()
        val (manager, lib) = buildFixture(cast, prefs)
        manager.addMoodToLibrary(Mood("foo", listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1))))
        manager.addMoodToLibrary(Mood("bar", emptyList()))
        manager.setActiveMoodByName("bar")
        outputLib(lib, """cli_execute("mood foo");""")
        assertTrue(cast.isEmpty())
        assertEquals("foo", manager.activeMood?.name)
    }

    @Test
    fun moodNameWithMultiplicity_repeatsThenRestoresPreviousMood() {
        val cast = mutableListOf<Int>()
        val prefs = prefs()
        val (manager, lib) = buildFixture(cast, prefs)
        manager.addMoodToLibrary(Mood("foo", listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1))))
        manager.addMoodToLibrary(Mood("bar", emptyList()))
        manager.setActiveMoodByName("bar")
        val output = outputLib(lib, """cli_execute("mood foo 2");""")
        assertEquals(listOf(200, 200), cast)
        assertEquals("bar", manager.activeMood?.name)
        assertTrue(output.contains("Mood swing complete."))
    }

    private fun buildFixture(
        cast: MutableList<Int>,
        prefs: Preferences,
    ): Pair<MoodManager, GameRuntimeLibrary> {
        val skills = moodSkillManager(cast)
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

    private fun moodSkillManager(cast: MutableList<Int>): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return object : SkillManager(client, SkillCastRequest(client), GameEventBus()) {
            init {
                learnLocalSkill(
                    SkillData(
                        id = 200,
                        name = "Skill 200",
                        type = SkillType.PASSIVE,
                        mpCost = 10,
                        dailyLimit = 0,
                        timesCast = 0,
                    ),
                )
            }

            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                repeat(quantity) { cast.add(skill.id) }
                return Result.success(Unit)
            }
        }
    }
}
