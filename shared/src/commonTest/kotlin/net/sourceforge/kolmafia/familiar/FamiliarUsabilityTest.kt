package net.sourceforge.kolmafia.familiar

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import com.russhwolf.settings.MapSettings

class FamiliarUsabilityTest {

    private val goat = FamiliarData(
        id = 4, name = "Bill", race = "Angry Goat",
        weight = 10, experience = 0, kills = 0,
    )
    private val skeleton = FamiliarData(
        id = 7, name = "Cap'n", race = "Spooky Pirate Skeleton",
        weight = 10, experience = 0, kills = 0,
    )
    private val barrrnacle = FamiliarData(
        id = 8, name = "Barn", race = "Barrrnacle",
        weight = 10, experience = 0, kills = 0,
    )
    private val mosquito = FamiliarData(
        id = 1, name = "Buzz", race = "Mosquito",
        weight = 10, experience = 0, kills = 0,
    )
    private val globmule = FamiliarData(
        id = 215, name = "Glob", race = "Globmule",
        weight = 10, experience = 0, kills = 0,
    )

    @BeforeTest
    fun loadFamiliars() = runBlocking {
        FamiliarDefinitionDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        StandardRequest.resetForTest()
    }

    @Test
    fun isUsable_unrestrictedPath_allowsOwnedFamiliar() {
        assertTrue(FamiliarUsability.isUsable(goat, CharacterState()))
    }

    @Test
    fun isUsable_zombieSlayer_blocksLivingFamiliar() {
        val state = CharacterState(challengePath = "Zombie Slayer")
        assertFalse(FamiliarUsability.isUsable(goat, state))
        assertTrue(FamiliarUsability.isUsable(skeleton, state))
    }

    @Test
    fun isUsable_beecore_blocksBeeRaceName() {
        val state = CharacterState(challengePath = "Bees Hate You", kingLiberated = false)
        assertFalse(FamiliarUsability.isUsable(barrrnacle, state))
        assertTrue(FamiliarUsability.isUsable(mosquito, state))
    }

    @Test
    fun isUsable_gLover_requiresGInRaceName() {
        val state = CharacterState(challengePath = "G-Lover")
        assertTrue(FamiliarUsability.isUsable(goat, state))
        assertFalse(FamiliarUsability.isUsable(mosquito, state))
    }

    @Test
    fun isUsable_zootomist_blocksGraftedFamiliar() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("zootGraftedHeadFamiliar", goat.id)
        val state = CharacterState(challengePath = "Z Is for Zootomist")
        assertFalse(FamiliarUsability.isUsable(goat, state, prefs))
        assertTrue(FamiliarUsability.isUsable(skeleton, state, prefs))
    }

    @Test
    fun isUsable_pokefam_allowsPokefamTypeRegardlessOfOtherGates() {
        StandardRequest.parseResponse(
            """
            <b>Familiars</b><p><span class="i">globmule</span><p>
            """.trimIndent(),
        )
        val state = CharacterState(
            challengePath = "Pocket Familiars",
            isHardcore = true,
        )
        assertTrue(FamiliarUsability.isUsable(globmule, state))
    }

    @Test
    fun isUsable_hardcore_blocksStandardBannedFamiliar() {
        StandardRequest.parseResponse(
            """
            <b>Familiars</b><p><span class="i">angry goat</span><p>
            """.trimIndent(),
        )
        val state = CharacterState(isHardcore = true)
        assertFalse(FamiliarUsability.isUsable(goat, state))
    }
}
