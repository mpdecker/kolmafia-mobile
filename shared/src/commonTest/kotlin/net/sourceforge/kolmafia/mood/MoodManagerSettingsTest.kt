package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.platform.UserDataFilePaths
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class MoodManagerSettingsTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("kolmafia-mood-settings-", "").apply {
            delete()
            mkdirs()
        }
        UserDataFilePaths.testBasePath = tempDir.absolutePath
        MoodRemovalKnownSources.clear()
        UneffectSkillEffectMap.resetForTest()
        runBlocking {
            EffectDatabase.load()
            SkillDefinitionDatabase.load()
        }
        UneffectSkillEffectMap.rebuild()
    }

    @AfterTest
    fun tearDown() {
        UserDataFilePaths.testBasePath = null
        tempDir.deleteRecursively()
        MoodRemovalKnownSources.clear()
        UneffectSkillEffectMap.resetForTest()
    }

    private fun manager(prefs: Preferences = Preferences(MapSettings())): MoodManager {
        val client = HttpClient(MockEngine { respond("") })
        return MoodManager(
            SkillManager(client, SkillCastRequest(client), GameEventBus()),
            prefs,
        )
    }

    @Test
    fun saveSettings_writesDesktopCompatibleFile() {
        val mgr = manager()
        mgr.addMoodToLibrary(
            Mood(
                name = "run",
                parentNames = listOf("default"),
                removalTriggers = listOf(
                    MoodRemovalTrigger(
                        type = MoodRemovalTriggerType.UNCONDITIONAL,
                        effectId = 0,
                        effectName = "",
                        action = "restore mp",
                    ),
                ),
            ),
        )
        mgr.saveSettings("Test Player")

        val text = UserDataFileIO.readText("test_player_moods.txt").orEmpty()
        assertTrue(text.contains("[ run extends default ]"))
        assertTrue(text.contains("unconditional => restore mp"))
    }

    @Test
    fun loadSettings_readsFileIntoLibrary() {
        UserDataFileIO.writeText(
            "hero_moods.txt",
            """
            [ combat extends default ]
            gain_effect Beaten Up => uneffect Beaten Up
            """.trimIndent() + "\n",
        )

        val mgr = manager()
        mgr.loadSettings("Hero")
        val combat = mgr.moodLibrary["combat"] ?: error("missing combat mood")
        assertEquals(MoodRemovalTriggerType.GAIN_EFFECT, combat.removalTriggers.single().type)
    }

    @Test
    fun updateFromPreferences_restoresActiveMoodAndPersistsPrefs() {
        UserDataFileIO.writeText(
            "runner_moods.txt",
            """
            [ run extends default ]
            unconditional => restore mp
            """.trimIndent() + "\n",
        )

        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.ACTIVE_MOOD_NAME, "run extends default")
        val mgr = manager(prefs)
        mgr.updateFromPreferences("Runner", "run extends default")

        assertEquals("run", mgr.activeMood?.name)
        assertTrue(prefs.getString(Preferences.MOOD_LIBRARY_NAMES).contains("run extends default"))
    }
}
