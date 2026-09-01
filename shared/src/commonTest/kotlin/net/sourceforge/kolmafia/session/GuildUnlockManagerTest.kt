package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class GuildUnlockManagerTest {
    @Test
    fun availabilityMatchesDesktopClassAndPathGates() {
        assertTrue(GuildUnlockManager.canUnlockGuild(CharacterState(characterClass = 1)))
        assertTrue(GuildUnlockManager.canUnlockGuild(CharacterState(characterClass = 6)))
        assertFalse(GuildUnlockManager.canUnlockGuild(CharacterState(characterClass = 7)))
        assertFalse(
            GuildUnlockManager.canUnlockGuild(
                CharacterState(characterClass = 1, challengePath = "Pocket Familiars"),
            ),
        )
        assertFalse(
            GuildUnlockManager.canUnlockGuild(
                CharacterState(characterClass = 1, challengePath = "You, Robot"),
            ),
        )
    }

    @Test
    fun storeAvailabilityRequiresCurrentAscensionAndRejectsNuclearAutumn() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(characterClass = 1, ascensionNumber = 12)
        prefs.setInt("lastGuildStoreOpen", 12)
        assertTrue(GuildUnlockManager.guildStoreAvailable(state, prefs))
        assertFalse(
            GuildUnlockManager.guildStoreAvailable(
                state.copy(challengePath = "Nuclear Autumn"),
                prefs,
            ),
        )
        prefs.setInt("lastGuildStoreOpen", 11)
        assertFalse(GuildUnlockManager.guildStoreAvailable(state, prefs))
    }

    @Test
    fun planUsesMobileAdventureIdsAndRestoresMoxiePantsMetadata() {
        val muscle = GuildUnlockManager.planFor(CharacterState(characterClass = 1))
        assertNotNull(muscle)
        assertEquals(543, muscle.challengeChoice)
        assertEquals("114", muscle.location.id)
        assertEquals(5193, muscle.targetItemId)

        val moxie = GuildUnlockManager.planFor(
            CharacterState(
                characterClass = 5,
                equipment = mapOf(
                    net.sourceforge.kolmafia.character.EquipmentSlot.PANTS to "blue suede shoes",
                ),
            ),
        )
        assertNotNull(moxie)
        assertEquals(542, moxie.challengeChoice)
    }
}
