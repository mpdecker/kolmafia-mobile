package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryFamiliarTest {

    private val goat = FamiliarData(
        id = 7, name = "Biscuit", race = "Angry Goat",
        weight = 12, experience = 0, kills = 0
    )

    private fun makeFamiliarManager(state: FamiliarState = FamiliarState()): FamiliarManager {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(state)
        return fm
    }

    private fun libWithGoat(): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFamiliar(id = 7, name = "Biscuit", weight = 12, exp = 0)
        val fm = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(goat)))
        return GameRuntimeLibrary(character = char, familiarManager = fm)
    }

    @Test
    fun haveFamiliar_trueWhenOwned() {
        assertEquals("true",
            outputLib(libWithGoat(),
                """print(to_string(have_familiar(to_familiar("Angry Goat"))));"""))
    }

    @Test
    fun haveFamiliar_falseWhenNotOwned() {
        val lib = GameRuntimeLibrary(familiarManager = makeFamiliarManager())
        assertEquals("false",
            outputLib(lib,
                """print(to_string(have_familiar(to_familiar("Purse Rat"))));"""))
    }

    @Test
    fun toFamiliar_roundTripsName() {
        assertEquals("Angry Goat",
            outputLib(GameRuntimeLibrary.forTesting(),
                """print(to_familiar("Angry Goat"));"""))
    }

    @Test
    fun myFamiliarWeight_returnsFromCharacterState() {
        val char = KoLCharacter()
        char.updateFamiliar(id = 7, name = "Biscuit", weight = 12, exp = 0)
        val lib = GameRuntimeLibrary(
            character = char,
            familiarManager = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(goat)))
        )
        assertEquals("12", outputLib(lib, "print(to_string(my_familiar_weight()));"))
    }

    @Test
    fun useFamiliar_returnsTrueWhenOwned() {
        // libWithGoat() has the goat registered and mock returns success
        assertEquals("true",
            outputLib(libWithGoat(),
                """print(to_string(use_familiar(to_familiar("Angry Goat"))));"""))
    }

    @Test
    fun useFamiliar_returnsFalseWithNoManager() {
        // no familiarManager injected → returns false immediately
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(),
                """print(to_string(use_familiar(to_familiar("Angry Goat"))));"""))
    }

    // ── my_familiar tests ────────────────────────────────────────────────────

    @Test
    fun myFamiliar_returnsRaceNotGivenName() {
        val sombrero = FamiliarData(
            id = 42, name = "Bob", race = "Hovering Sombrero",
            weight = 10, experience = 0, kills = 0
        )
        val fm = makeFamiliarManager(FamiliarState(activeFamiliar = sombrero))
        val lib = GameRuntimeLibrary(familiarManager = fm)
        assertEquals("Hovering Sombrero", outputLib(lib, "print(my_familiar());"))
    }

    @Test
    fun myFamiliar_returnsNoneWhenNoActiveFamiliar() {
        val fm = makeFamiliarManager(FamiliarState(activeFamiliar = null))
        val lib = GameRuntimeLibrary(familiarManager = fm)
        assertEquals("none", outputLib(lib, "print(my_familiar());"))
    }

    @Test
    fun myFamiliar_returnsNoneWhenNoFamiliarManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("none", outputLib(lib, "print(my_familiar());"))
    }

    @Test
    fun steal_returnsInventoryGain() {
        val itemId = 88
        val stealReq = object : FamiliarRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun stealItem(itemId: Int) = Result.success("ok")
        }
        val inv = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(InventoryState())
            override val state: StateFlow<InventoryState> = _s
            override suspend fun fetchInventory() {
                _s.value = InventoryState(items = mapOf(
                    itemId to InventoryItem(itemId, "stolen thing", 1, ItemType.OTHER)
                ))
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = itemId, name = "stolen thing", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(
            familiarRequest = stealReq,
            inventoryManager = inv,
            gameDatabase = db,
        )
        assertEquals("1", outputLib(lib, """print(to_string(steal(to_item("stolen thing"), 1)));"""))
    }
}
