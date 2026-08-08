package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionDatabaseSpeakeasyRefreshTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        SpeakeasyAvailability.resetForTest()
    }

    @Test
    fun refresh_availableLuckyLindyWithMeat_setsInitialOne() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        SpeakeasyAvailability.addLoungeId(4)
        val prefs = Preferences(MapSettings())
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 0)
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(meat = 500),
            preferences = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(1, ConcoctionDatabase.initialCount("Lucky Lindy"))
        assertEquals(1, ConcoctionDatabase.creatableCount("Lucky Lindy"))
        assertEquals(1, ConcoctionDatabase.availableCount("Lucky Lindy"))
    }

    @Test
    fun refresh_unavailableDrink_setsInitialZero() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(meat = 5000),
            preferences = Preferences(MapSettings()),
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.initialCount("Lucky Lindy"))
        assertEquals(0, ConcoctionDatabase.creatableCount("Lucky Lindy"))
    }

    @Test
    fun refresh_threeDrinksDrunk_setsInitialZeroEvenWhenAvailable() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        SpeakeasyAvailability.addLoungeId(4)
        val prefs = Preferences(MapSettings())
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 3)
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(meat = 5000),
            preferences = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.initialCount("Lucky Lindy"))
    }

    @Test
    fun refresh_registersVirtualSpeakeasyConcoction() {
        SpeakeasyAvailability.addLoungeId(4)

        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)

        val concoction = ConcoctionDatabase.getByResult("Lucky Lindy")
        assertNotNull(concoction)
        assertTrue(concoction.methods.contains("SPEAKEASY"))
    }

    @Test
    fun isPermittedMethod_requiresAvailabilityAndNotLimitClan() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        SpeakeasyAvailability.addLoungeId(4)
        val concoction = ConcoctionDatabase.getByResult("Lucky Lindy") ?: ConcoctionData(
            result = "Lucky Lindy",
            resultQuantity = 1,
            methods = setOf("SPEAKEASY"),
            ingredients = emptyList(),
        )
        val state = CharacterState(limitMode = "none")

        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
            ),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state.copy(limitMode = "ed"),
                limitMode = "ed",
            ),
        )
        SpeakeasyAvailability.reset()
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
            ),
        )
    }

    private fun registerSpeakeasyDrink(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }
}
