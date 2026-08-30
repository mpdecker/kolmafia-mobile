package net.sourceforge.kolmafia.item

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetrieveSourceGatesTest {

    @Test
    fun nullPrefs_allowsAllSources() {
        assertTrue(RetrieveSourceGates.canUseCloset(null, null))
        assertTrue(RetrieveSourceGates.canUseStorage(null, null))
        assertTrue(RetrieveSourceGates.canUseNPCStores(null, null))
        assertTrue(RetrieveSourceGates.canUseMall(null, null, tradeable = true))
        assertTrue(RetrieveSourceGates.canUseCoinmasters(null, null))
    }

    @Test
    fun autoSatisfyOff_blocksMallAndNpc() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("autoSatisfyWithMall", false)
            setBoolean("autoSatisfyWithNPCs", false)
            setBoolean("autoSatisfyWithCloset", false)
            setBoolean("autoSatisfyWithStorage", true)
        }
        val state = CharacterState(roninLeft = 0, isHardcore = false, kingLiberated = true)
        assertFalse(RetrieveSourceGates.canUseMall(prefs, state, tradeable = true))
        assertFalse(RetrieveSourceGates.canUseNPCStores(prefs, state))
        assertFalse(RetrieveSourceGates.canUseCloset(prefs, state))
        assertTrue(RetrieveSourceGates.canUseStorage(prefs, state))
    }

    @Test
    fun autoSatisfyOn_allowsMallAndNpc() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("autoSatisfyWithMall", true)
            setBoolean("autoSatisfyWithNPCs", true)
            setBoolean("autoSatisfyWithCoinmasters", true)
        }
        val state = CharacterState(roninLeft = 0, isHardcore = false, kingLiberated = true)
        assertTrue(RetrieveSourceGates.canUseMall(prefs, state, tradeable = true))
        assertTrue(RetrieveSourceGates.canUseNPCStores(prefs, state))
        assertTrue(RetrieveSourceGates.canUseCoinmasters(prefs, state))
    }

    @Test
    fun nonTradeable_blocksMall() {
        val prefs = Preferences(MapSettings()).apply { setBoolean("autoSatisfyWithMall", true) }
        assertFalse(
            RetrieveSourceGates.canUseMall(
                prefs,
                CharacterState(kingLiberated = true),
                tradeable = false,
            ),
        )
    }
}
