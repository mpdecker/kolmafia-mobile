package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mood.Mood
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.mood.MoodTrigger
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP119Test {

    @Test
    fun revision_phase170() {
        assertEquals("phase260", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun inMuscleSign_mongoose_true() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(in_muscle_sign());""").trim())
        assertEquals("false", outputLib(lib, """print(in_mysticality_sign());""").trim())
        assertEquals("false", outputLib(lib, """print(in_moxie_sign());""").trim())
        assertEquals("false", outputLib(lib, """print(in_bad_moon());""").trim())
    }

    @Test
    fun inMysticalitySign_wallaby_true() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Wallaby"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("false", outputLib(lib, """print(in_muscle_sign());""").trim())
        assertEquals("true", outputLib(lib, """print(in_mysticality_sign());""").trim())
    }

    @Test
    fun inMoxieSign_vole_true() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Vole"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(in_moxie_sign());""").trim())
    }

    @Test
    fun inBadMoon_true() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(sign = "Bad Moon"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(in_bad_moon());""").trim())
        assertEquals("false", outputLib(lib, """print(in_muscle_sign());""").trim())
    }

    @Test
    fun moodExecute_castsMissingTrigger() {
        val cast = mutableListOf<Int>()
        val skills = moodSkillManager(cast)
        val mood = MoodManager(skills, prefs())
        mood.activeMood = Mood(
            "test",
            listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(mp = "50", mpmax = "100"))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = mood,
            skillManager = skills,
        )
        outputLib(lib, """mood_execute(0);""")
        assertEquals(listOf(200), cast)
    }

    @Test
    fun moodExecute_skipsWhenRecoveryActive() {
        val cast = mutableListOf<Int>()
        val skills = moodSkillManager(cast)
        val mood = MoodManager(skills, prefs())
        mood.activeMood = Mood(
            "test",
            listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(mp = "50", mpmax = "100"))
        }
        val client = HttpClient(MockEngine { respond("") })
        val rm = RecoveryManager(
            inventoryManager = InventoryManager(client, GameEventBus()),
            skillManager = skills,
            preferences = prefs(),
        ).also { it.isRecoveryActive = true }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = mood,
            skillManager = skills,
            recoveryManager = rm,
        )
        outputLib(lib, """mood_execute(0);""")
        assertTrue(cast.isEmpty())
    }

    @Test
    fun moodExecute_skipsUnderLimitModeRecovery() {
        val cast = mutableListOf<Int>()
        val skills = moodSkillManager(cast)
        val mood = MoodManager(skills, prefs())
        mood.activeMood = Mood(
            "test",
            listOf(MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(mp = "50", mpmax = "100", limitmode = "spelunky"),
            )
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = mood,
            skillManager = skills,
        )
        outputLib(lib, """mood_execute(0);""")
        assertTrue(cast.isEmpty())
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
