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

class ConcoctionDatabaseHotDogRefreshTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        HotDogAvailability.resetForTest()
    }

    @Test
    fun refresh_availableBasicHotDog_setsInitialOne() {
        HotDogAvailability.addForTest("basic hot dog")
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(1, ConcoctionDatabase.initialCount("basic hot dog"))
        assertEquals(0, ConcoctionDatabase.creatableCount("basic hot dog"))
        assertEquals(1, ConcoctionDatabase.totalCount("basic hot dog"))
        assertEquals(1, ConcoctionDatabase.availableCount("basic hot dog"))
    }

    @Test
    fun refresh_unavailableHotDog_setsInitialZero() {
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.initialCount("basic hot dog"))
        assertEquals(0, ConcoctionDatabase.totalCount("basic hot dog"))
    }

    @Test
    fun refresh_fancyHotDogEaten_setsInitialZeroEvenWhenAvailable() {
        HotDogAvailability.addForTest("sly dog")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, true)
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.initialCount("sly dog"))
        assertEquals(0, ConcoctionDatabase.totalCount("sly dog"))
    }

    @Test
    fun refresh_registersVirtualHotDogConcoction() {
        HotDogAvailability.addForTest("basic hot dog")

        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)

        val concoction = ConcoctionDatabase.getByResult("basic hot dog")
        assertNotNull(concoction)
        assertTrue(concoction.methods.contains("HOT_DOG"))
    }

    @Test
    fun isPermittedMethod_requiresAvailabilityAndNotLimitClan() {
        HotDogAvailability.addForTest("basic hot dog")
        val concoction = ConcoctionDatabase.getByResult("basic hot dog") ?: ConcoctionData(
            result = "basic hot dog",
            resultQuantity = 1,
            methods = setOf("HOT_DOG"),
            ingredients = emptyList(),
        )
        val state = CharacterState(limitMode = "none")

        assertTrue(ConcoctionPermitted.isPermittedMethod(concoction, state))
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state.copy(limitMode = "ed"),
                limitMode = "ed",
            ),
        )
        HotDogAvailability.reset()
        assertFalse(ConcoctionPermitted.isPermittedMethod(concoction, state))
    }
}
