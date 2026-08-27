package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP898TrackBTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun testDb(): GameDatabase {
        val db = GameDatabase()
        return db
    }

    private fun fakeFamiliarManager(
        familiars: List<FamiliarData> = emptyList(),
        active: FamiliarData? = null,
    ): FamiliarManager {
        val client = HttpClient(MockEngine { respond("ok") })
        val fm = object : FamiliarManager(client, GameEventBus()) {}
        fm.testSetState(FamiliarState(ownedFamiliars = familiars, activeFamiliar = active))
        return fm
    }

    // ── AshP900 — can_equip(item) ───────────────────────────────────────────

    @Test
    fun phase900_canEquipItem_hatIsEquippable() {
        val hat = ItemData(
            id = 1001, name = "test hat", descId = "d1001", image = "h.gif",
            primaryUse = ItemPrimaryUse.HAT, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null,
        )
        ItemDatabase.registerForTest(hat)
        val db = testDb()
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs())
        assertEquals("true", outputLib(lib, """print(can_equip(to_item("test hat")));"""))
    }

    @Test
    fun phase900_canEquipItem_foodIsNotEquippable() {
        val food = ItemData(
            id = 1002, name = "test food", descId = "d1002", image = "f.gif",
            primaryUse = ItemPrimaryUse.FOOD, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null,
        )
        ItemDatabase.registerForTest(food)
        val db = testDb()
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs())
        assertEquals("false", outputLib(lib, """print(can_equip(to_item("test food")));"""))
    }

    @Test
    fun phase900_canEquipFamiliar_ownedReturnsTrue() {
        val goat = FamiliarData(
            id = 7, name = "Billy", race = "Angry Goat",
            weight = 10, experience = 0, kills = 0,
        )
        val fm = fakeFamiliarManager(familiars = listOf(goat))
        val lib = GameRuntimeLibrary(
            familiarManager = fm, preferences = prefs(),
        )
        assertEquals("true", outputLib(lib, """print(can_equip(to_familiar("Angry Goat")));"""))
    }

    @Test
    fun phase900_canEquipFamiliar_notOwnedReturnsFalse() {
        val fm = fakeFamiliarManager()
        val lib = GameRuntimeLibrary(
            familiarManager = fm, preferences = prefs(),
        )
        assertEquals("false", outputLib(lib, """print(can_equip(to_familiar("Angry Goat")));"""))
    }

    // ── AshP901 — equipped_amount ───────────────────────────────────────────

    @Test
    fun phase901_equippedAmount_countsEquippedItems() {
        val hat = ItemData(
            id = 2001, name = "cool hat", descId = "d2001", image = "h.gif",
            primaryUse = ItemPrimaryUse.HAT, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null,
        )
        ItemDatabase.registerForTest(hat)
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "cool hat")
        val db = testDb()
        val lib = GameRuntimeLibrary(character = char, gameDatabase = db, preferences = prefs())
        assertEquals("1", outputLib(lib, """print(equipped_amount(to_item("cool hat")));"""))
    }

    @Test
    fun phase901_equippedAmount_noneEquipped() {
        val hat = ItemData(
            id = 2002, name = "other hat", descId = "d2002", image = "h.gif",
            primaryUse = ItemPrimaryUse.HAT, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null,
        )
        ItemDatabase.registerForTest(hat)
        val char = KoLCharacter()
        val db = testDb()
        val lib = GameRuntimeLibrary(character = char, gameDatabase = db, preferences = prefs())
        assertEquals("0", outputLib(lib, """print(equipped_amount(to_item("other hat")));"""))
    }

    // ── AshP902 — familiar_weight(familiar) ─────────────────────────────────

    @Test
    fun phase902_familiarWeight_returnsBaseWeight() {
        val goat = FamiliarData(
            id = 7, name = "Billy", race = "Angry Goat",
            weight = 15, experience = 0, kills = 0,
        )
        val fm = fakeFamiliarManager(familiars = listOf(goat))
        val lib = GameRuntimeLibrary(familiarManager = fm, preferences = prefs())
        assertEquals("15", outputLib(lib, """print(familiar_weight(to_familiar("Angry Goat")));"""))
    }

    @Test
    fun phase902_familiarWeight_unknownFamiliarReturnsZero() {
        val fm = fakeFamiliarManager()
        val lib = GameRuntimeLibrary(familiarManager = fm, preferences = prefs())
        assertEquals("0", outputLib(lib, """print(familiar_weight(to_familiar("unknown fam")));"""))
    }

    // ── AshP903 — is_familiar_equipment_locked / lock_familiar_equipment ────

    @Test
    fun phase903_isFamiliarEquipmentLocked_defaultFalse() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, "print(is_familiar_equipment_locked());"))
    }

    @Test
    fun phase903_lockFamiliarEquipment_setsPreference() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        outputLib(lib, "lock_familiar_equipment(true);")
        assertEquals("true", outputLib(lib, "print(is_familiar_equipment_locked());"))
    }

    // ── AshP904 — my_effective_familiar / my_bjorned_familiar / my_companion

    @Test
    fun phase904_myEffectiveFamiliar_returnsActiveFamiliar() {
        val goat = FamiliarData(
            id = 7, name = "Billy", race = "Angry Goat",
            weight = 10, experience = 0, kills = 0,
        )
        val fm = fakeFamiliarManager(familiars = listOf(goat), active = goat)
        val lib = GameRuntimeLibrary(familiarManager = fm, preferences = prefs())
        assertEquals("Angry Goat", outputLib(lib, "print(my_effective_familiar());"))
    }

    @Test
    fun phase904_myEffectiveFamiliar_noneWhenNoFamiliar() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("none", outputLib(lib, "print(my_effective_familiar());"))
    }

    @Test
    fun phase904_myBjornedFamiliar_returnsBjornedRace() {
        val char = KoLCharacter()
        char.updateBjorned(8, "Barrrnacle")
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("Barrrnacle", outputLib(lib, "print(my_bjorned_familiar());"))
    }

    @Test
    fun phase904_myBjornedFamiliar_noneWhenEmpty() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("none", outputLib(lib, "print(my_bjorned_familiar());"))
    }

    @Test
    fun phase904_myCompanion_returnsEmptyDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(my_companion());"))
    }
}
