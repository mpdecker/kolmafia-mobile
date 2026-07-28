package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.BattleLearnSkillIds
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

class SkillLearnFromResponseTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    private fun prefs() = Preferences(MapSettings())

    private fun skillManager(): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }

    private fun registerTimberwolf() {
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
    }

    @Test
    fun learnSkillFromResponse_parsesNewSkillByName() {
        registerTimberwolf()
        val manager = skillManager()
        val html = """<td>You learn a new skill: <b>Snarl of the Timberwolf</b>"""
        val learned = SkillLearnFromResponse.learnSkillFromResponse(
            html,
            prefs(),
            manager,
            inventoryManager = null,
        )
        assertEquals(BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF, learned)
        assertEquals(
            BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
            manager.state.value.skills.single().id,
        )
    }

    @Test
    fun learnSkillFromResponse_parsesNewSkillById() {
        registerTimberwolf()
        val manager = skillManager()
        val html = """You acquire a skill:&nbsp;&nbsp;whichskill=${BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF}"""
        val learned = SkillLearnFromResponse.learnSkillFromResponse(
            html,
            prefs(),
            manager,
            inventoryManager = null,
        )
        assertEquals(BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF, learned)
    }

    @Test
    fun learnSkillFromResponse_noMatch_returnsZero() {
        assertEquals(
            0,
            SkillLearnFromResponse.learnSkillFromResponse(
                "<html><body>nothing here</body></html>",
                prefs(),
                skillManager(),
                inventoryManager = null,
            ),
        )
    }
}
