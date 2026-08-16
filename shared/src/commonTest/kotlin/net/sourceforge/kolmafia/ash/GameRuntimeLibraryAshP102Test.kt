package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.BattleLearnSkillIds
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP102Test {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun revision_phase144() {
        assertEquals("phase490", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun fightVisit_learnsSkillFromHtml() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
                name = "Snarl of the Timberwolf",
                image = "wolfmask.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 10,
                duration = 10,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val skillManager = SkillManager(client, SkillCastRequest(client), GameEventBus())
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            skillManager = skillManager,
            preferences = prefs,
        )
        val html = """
            You're fighting a wolf.
            You acquire a skill:&nbsp;&nbsp;whichskill=${BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF}
        """.trimIndent()
        lib.processVisitResponseHooks(html, "fight.php")
        assertTrue(
            outputLib(
                lib,
                """print(have_skill(to_skill("Snarl of the Timberwolf")));""",
            ).toBoolean(),
        )
    }
}
