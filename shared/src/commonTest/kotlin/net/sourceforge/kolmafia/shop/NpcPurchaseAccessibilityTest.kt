package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class NpcPurchaseAccessibilityTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
    }

    @Test
    fun validate_trueForUnrestrictedShop() {
        registerItem(9401, "open shop item")
        NpcStoreDatabase.loadFromText("Open Shop\topenstore\topen shop item\t10\n")
        val store = NpcStoreData("openstore", "Open Shop", "NPC")
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                9401,
                store,
                CharacterState(),
                Preferences(MapSettings()),
            ),
        )
        assertTrue(NpcStoreDatabase.containsItem(9401, validate = true))
    }

    @Test
    fun validate_falseWhenGuildStoreClosed() {
        registerItem(9402, "guild item")
        NpcStoreDatabase.loadFromText("Smacketeria\tguildstore3\tguild item\t100\n")
        val store = NpcStoreData("guildstore3", "Smacketeria", "NPC")
        val prefs = Preferences(MapSettings())
        val state = CharacterState(
            characterClass = CharacterClass.SEAL_CLUBBER.id,
            ascensionNumber = 3,
        )
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(9402, store, state, prefs),
        )
        prefs.setInt("lastGuildStoreOpen", 3)
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(9402, store, state, prefs),
        )
    }

    @Test
    fun validate_falseWhenDispensaryLocked() {
        registerItem(9403, "disp item")
        NpcStoreDatabase.loadFromText("Knob Dispensary\tknobdisp\tdisp item\t50\n")
        val store = NpcStoreData("knobdisp", "Knob Dispensary", "NPC")
        val prefs = Preferences(MapSettings())
        val state = CharacterState(ascensionNumber = 2)
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                9403,
                store,
                state,
                prefs,
            ) { 0 },
        )
        prefs.setInt("lastDispensaryOpen", 2)
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                9403,
                store,
                state,
                prefs,
            ) { id -> if (id == 339) 1 else 0 },
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
