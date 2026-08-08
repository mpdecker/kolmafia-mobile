package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.preferences.Preferences

class GuildSkillSyncTest {

    private fun prefs() = Preferences(MapSettings())

    private fun character(
        characterClass: Int = CharacterClass.PASTAMANCER.id,
        meat: Int = 1000,
    ): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    classId = characterClass.toString(),
                    meat = meat.toString(),
                ),
            )
        }

    @BeforeTest
    fun setUp() {
        SkillDefinitionDatabase.resetForTest()
        registerEntanglingNoodles()
        registerLungeSmack()
    }

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun findSkillFromUrl_pastamancerSkill4_returns3004() {
        val skillId = SkillDefinitionProxy.findSkillFromUrl(
            "guild.php?action=buyskill&skillid=4",
            CharacterClass.PASTAMANCER.id,
        )
        assertEquals(3004, skillId)
    }

    @Test
    fun getGuildPurchaseCost_lungeSmackLevel1_returns125() {
        assertEquals(125, SkillDefinitionProxy.getGuildPurchaseCost(LUNGE_SMACK_ID))
    }

    @Test
    fun getGuildPurchaseCost_entanglingNoodlesLevel2_returns250() {
        assertEquals(250, SkillDefinitionProxy.getGuildPurchaseCost(ENTANGLING_NOODLES_ID))
    }

    @Test
    fun parseBuyskill_success_deductsMeatAndLearnsSkill() {
        val prefs = prefs()
        val char = character(meat = 1000)

        GuildSkillSync.parseBuyskill(
            url = "guild.php?action=buyskill&skillid=4",
            html = "You learn a new skill: <b>Entangling Noodles</b>",
            character = char,
            preferences = prefs,
            skillManager = null,
            inventoryManager = null,
        )

        assertEquals(750, char.state.value.meat)
        assertEquals(1, prefs.getInt("skillLevel$ENTANGLING_NOODLES_ID", 0))
    }

    @Test
    fun parseBuyskill_failure_noLearnMessage_leavesMeatAndSkillUnchanged() {
        val prefs = prefs()
        val char = character(meat = 1000)

        GuildSkillSync.parseBuyskill(
            url = "guild.php?action=buyskill&skillid=4",
            html = "You can't afford that skill.",
            character = char,
            preferences = prefs,
            skillManager = null,
            inventoryManager = null,
        )

        assertEquals(1000, char.state.value.meat)
        assertEquals(0, prefs.getInt("skillLevel$ENTANGLING_NOODLES_ID", 0))
    }

    @Test
    fun parseFromVisit_makestaff_doesNotRunBuyskillSync() {
        val prefs = prefs()
        val char = character(meat = 1000)

        GuildVisitSync.parseFromVisit(
            url = "guild.php?action=makestaff&whichstaff=123",
            html = "You learn a new skill: <b>Entangling Noodles</b>",
            character = char,
            preferences = prefs,
        )

        assertEquals(1000, char.state.value.meat)
        assertEquals(0, prefs.getInt("skillLevel$ENTANGLING_NOODLES_ID", 0))
    }

    @Test
    fun parseFromVisit_buyskill_success_routesToGuildSkillSync() {
        val prefs = prefs()
        val char = character(meat = 500)

        GuildVisitSync.parseFromVisit(
            url = "guild.php?action=buyskill&skillid=4",
            html = "You learn a new skill: <b>Entangling Noodles</b>",
            character = char,
            preferences = prefs,
        )

        assertEquals(250, char.state.value.meat)
        assertTrue(prefs.getInt("skillLevel$ENTANGLING_NOODLES_ID", 0) > 0)
    }

    private fun registerEntanglingNoodles() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = ENTANGLING_NOODLES_ID,
                name = "Entangling Noodles",
                image = "entnoodles.gif",
                tags = setOf("combat", "spell"),
                mpCost = 3,
                duration = 0,
                guildLevel = 2,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    private fun registerLungeSmack() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = LUNGE_SMACK_ID,
                name = "Lunge Smack",
                image = "club2.gif",
                tags = setOf("combat"),
                mpCost = 1,
                duration = 0,
                guildLevel = 1,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    companion object {
        private const val ENTANGLING_NOODLES_ID = 3004
        private const val LUNGE_SMACK_ID = 1004
    }
}
