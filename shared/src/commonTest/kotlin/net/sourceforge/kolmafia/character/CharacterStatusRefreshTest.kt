package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot.HAT
import net.sourceforge.kolmafia.character.EquipmentSlot.WEAPON

class CharacterStatusRefreshTest {

    @Test
    fun needsCharpaneFallback_noobcore() {
        val state = CharacterState(challengePath = AscensionPath.GELATINOUS_NOOB.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_pokefam() {
        val state = CharacterState(challengePath = AscensionPath.POKEFAM.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_disguise() {
        val state = CharacterState(challengePath = AscensionPath.DISGUISES_DELIMIT.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_spelunkyLimitMode() {
        val state = CharacterState(limitMode = "spelunky")
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_batmanLimitMode() {
        val state = CharacterState(limitMode = "batman")
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_transfunctionerEquipped() {
        val state = CharacterState(
            equipment = mapOf(HAT to CharpaneStatusSync.TRANSFUNCTIONER_NAME),
        )
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_normalStandardPath() {
        val state = CharacterState(
            challengePath = AscensionPath.STANDARD.apiName,
            limitMode = "none",
            equipment = mapOf(WEAPON to "titanium assault umbrella"),
        )
        assertFalse(CharacterStatusRefresh.needsCharpaneFallback(state))
    }
}
