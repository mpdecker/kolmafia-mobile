package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED

class NpcPurchaseAccessibilityTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun store(key: String, name: String = key): NpcStoreData =
        NpcStoreData(storeKey = key, storeName = name, storeType = "NPC")

    @Test
    fun whiteCitadel_falseOnStep1() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, "step1")
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }

    @Test
    fun whiteCitadel_trueOnStep5() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, "step5")
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }

    @Test
    fun whiteCitadel_trueWhenFinished() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, FINISHED)
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }

    @Test
    fun bugbear_blockedWithoutCostume() {
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 683,
                store = store("bugbear", "Bugbear Bakery"),
                state = CharacterState(),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun bugbear_allowedWithCostumePieces() {
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 683,
                store = store("bugbear", "Bugbear Bakery"),
                state = CharacterState(),
                prefs = prefs(),
                accessibleCount = { if (it == 169 || it == 79) 1 else 0 },
            ),
        )
    }

    @Test
    fun wildfire_blartBlockedAfterAscensionPurchase() {
        val p = prefs()
        p.setBoolean("itemBoughtPerAscension10790", true)
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 10790,
                store = store("wildfire", "FDKOL Auxiliary"),
                state = CharacterState(challengePath = net.sourceforge.kolmafia.character.AscensionPath.WILDFIRE.apiName),
                prefs = p,
            ),
        )
    }

    @Test
    fun hippy_peachAllowedAfterSync() {
        val p = prefs()
        p.setString(Quest.ISLAND_WAR.prefKey, "step2")
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hippy",
            html = "peach pear plum",
            prefs = p,
            ascensionNumber = 5,
        )
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 673,
                store = store("hippy", "Hippy Store (Hippy)"),
                state = CharacterState(ascensionNumber = 5),
                prefs = p,
                accessibleCount = { id ->
                    when (id) {
                        2337, 2032, 2033 -> 1
                        else -> 0
                    }
                },
            ),
        )
    }

    @Test
    fun fwshop_hatBlockedAfterSyncMarksBought() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "fwshop",
            html = "<b>Combat Explosives</b>",
            prefs = p,
            ascensionNumber = 1,
        )
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 10762,
                store = store("fwshop", "Clan Underground Fireworks Shop"),
                state = CharacterState(),
                prefs = p,
            ),
        )
    }

    @Test
    fun hiddentavern_blockedBeforeSync() {
        val p = prefs()
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 175,
                store = store("hiddentavern", "The Hidden Tavern"),
                state = CharacterState(ascensionNumber = 5),
                prefs = p,
            ),
        )
    }

    @Test
    fun hiddentavern_allowedAfterStoreSync() {
        val p = prefs()
        NpcShopSync.syncFromStoreHtml(
            storeKey = "hiddentavern",
            html = "<html>Hidden Tavern</html>",
            prefs = p,
            ascensionNumber = 5,
        )
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 175,
                store = store("hiddentavern", "The Hidden Tavern"),
                state = CharacterState(ascensionNumber = 5),
                prefs = p,
            ),
        )
    }
}
