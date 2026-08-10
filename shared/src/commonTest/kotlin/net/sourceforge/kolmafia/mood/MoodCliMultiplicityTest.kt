package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
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

class MoodCliMultiplicityTest {

    @Test
    fun moodRepeat_forwardsMultiplicity() {
        val cast = mutableListOf<Int>()
        val skills = moodSkillManager(cast)
        val mood = MoodManager(skills, prefs())
        mood.activeMood = Mood(
            "test",
            listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(mp = "500", mpmax = "500"))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = mood,
            skillManager = skills,
        )
        outputLib(lib, """cli_execute("mood repeat 2");""")
        assertEquals(listOf(200, 200), cast)
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
