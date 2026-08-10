package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync

class UneffectItemAcquisitionTest {

    @BeforeTest
    fun loadItems() {
        runBlocking { ItemDatabase.load() }
    }

    private fun prefs(block: Preferences.() -> Unit = {}): Preferences =
        Preferences(MapSettings()).also(block)

    private fun ctx(itemIds: Set<Int> = emptySet()): UneffectActionContext =
        UneffectActionContext(
            effectId = 8,
            preferences = prefs(),
            characterState = CharacterState(roninLeft = 0),
            hasItemId = { it in itemIds },
            hasSkill = { false },
            canCastSkill = { false },
        )

    private fun checkContext(
        canInteract: Boolean = true,
        hasClan: Boolean = true,
        stashItemIds: Set<Int> = emptySet(),
    ): DynamicItemModifierSync.CheckContext =
        DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = emptySet(),
            storageItemIds = emptySet(),
            stashItemIds = stashItemIds,
            limitMode = "",
            canInteract = canInteract,
            hasClan = hasClan,
            ascensionPath = AscensionPath.NONE,
            codpieceGemNames = emptySet(),
            hermitCloverCount = 0,
        )

    @Test
    fun canAcquireUneffectItem_mallWhenGateOpenAndPrefEnabled() {
        val preferences = prefs { setBoolean("autoSatisfyWithMall", true) }
        val actionCtx = ctx()
        val db = GameDatabase()
        assertTrue(
            UneffectItemAcquisition.canAcquireUneffectItem(
                itemId = 829,
                effectId = 8,
                ctx = actionCtx,
                checkContext = checkContext(),
                prefs = preferences,
                db = db,
                charState = CharacterState(roninLeft = 0),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun canAcquireUneffectItem_stashWhenGateOpenAndItemPresent() {
        val preferences = prefs { setBoolean("autoSatisfyWithStash", true) }
        val actionCtx = ctx()
        assertTrue(
            UneffectItemAcquisition.canAcquireUneffectItem(
                itemId = 829,
                effectId = 8,
                ctx = actionCtx,
                checkContext = checkContext(stashItemIds = setOf(829)),
                prefs = preferences,
                db = null,
                charState = CharacterState(roninLeft = 0),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun canAcquireUneffectItem_mallBlockedWhenHasRemedyAndNotNeedsCocoa() {
        val preferences = prefs { setBoolean("autoSatisfyWithMall", true) }
        val actionCtx = ctx(
            itemIds = setOf(UneffectRemovableMaps.REMEDY),
        )
        assertFalse(
            UneffectItemAcquisition.canAcquireUneffectItem(
                itemId = 829,
                effectId = 8,
                ctx = actionCtx,
                checkContext = checkContext(),
                prefs = preferences,
                db = GameDatabase(),
                charState = CharacterState(roninLeft = 0),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun shouldBlockNeedsCocoaHttpUneffect_whenCocoaNotAcquirable() {
        val preferences = prefs()
        val actionCtx = ctx()
        assertTrue(
            UneffectItemAcquisition.shouldBlockNeedsCocoaHttpUneffect(
                effectId = 1278,
                ctx = actionCtx,
                checkContext = checkContext(),
                prefs = preferences,
                db = null,
                charState = CharacterState(roninLeft = 0),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun shouldBlockNeedsCocoaHttpUneffect_falseWhenCocoaInInventory() {
        val preferences = prefs()
        val actionCtx = ctx(itemIds = setOf(UneffectRemovableMaps.HOT_DREADSYLVANIAN_COCOA))
        assertFalse(
            UneffectItemAcquisition.shouldBlockNeedsCocoaHttpUneffect(
                effectId = 1278,
                ctx = actionCtx,
                checkContext = checkContext(),
                prefs = preferences,
                db = null,
                charState = CharacterState(roninLeft = 0),
                accessibleCount = { 0 },
            ),
        )
    }
}
