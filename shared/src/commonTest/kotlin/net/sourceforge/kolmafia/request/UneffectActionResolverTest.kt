package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastManager

class UneffectActionResolverTest {

    @AfterTest
    fun tearDown() {
        UneffectRemovableMaps.reset { false }
    }

    private fun prefs(block: Preferences.() -> Unit = {}): Preferences =
        Preferences(MapSettings()).also(block)

    private fun ctx(
        effectId: Int,
        effectName: String = "",
        moodPredefinedAction: String? = null,
        preferences: Preferences? = prefs(),
        characterState: CharacterState? = CharacterState(roninLeft = 1),
        itemIds: Set<Int> = emptySet(),
        hasSkill: (String) -> Boolean = { false },
        canCastSkill: (String) -> Boolean = { false },
        canRetrieveRemedy: Boolean = false,
        canAcquireUneffectItem: (Int) -> Boolean = { false },
    ): UneffectActionContext = UneffectActionContext(
        effectId = effectId,
        effectName = effectName,
        moodPredefinedAction = moodPredefinedAction,
        preferences = preferences,
        characterState = characterState,
        hasItemId = { id -> id in itemIds },
        hasSkill = hasSkill,
        canCastSkill = canCastSkill,
        canRetrieveRemedy = canRetrieveRemedy,
        canAcquireUneffectItem = canAcquireUneffectItem,
    )

    @Test
    fun resolve_castableSkill_returnsCastSkill() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 7,
                hasSkill = { it.equals("Shake It Off", ignoreCase = true) },
                canCastSkill = { it.equals("Shake It Off", ignoreCase = true) },
            ),
        )
        assertEquals(UneffectAction.CastSkill("Shake It Off"), action)
    }

    @Test
    fun resolve_shakeItOffInRoninWithVipKey_returnsHotTub() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 42,
                itemIds = setOf(BreakfastManager.VIP_LOUNGE_KEY_ID),
            ),
        )
        assertEquals(UneffectAction.HotTub, action)
    }

    @Test
    fun resolve_hotTubBlockedWhenCanInteract_returnsHttpUneffect() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 42,
                characterState = CharacterState(roninLeft = 0),
                itemIds = setOf(BreakfastManager.VIP_LOUNGE_KEY_ID),
            ),
        )
        assertIs<UneffectAction.HttpUneffect>(action)
    }

    @Test
    fun resolve_poisonWithAntidote_returnsUseItem() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 8,
                itemIds = setOf(829),
            ),
        )
        assertEquals(UneffectAction.UseItem(829), action)
    }

    @Test
    fun resolve_ancientCureAllBeforeRemedyRetrieve() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 999,
                itemIds = setOf(
                    UneffectRemovableMaps.ANCIENT_CURE_ALL,
                    UneffectRemovableMaps.REMEDY,
                ),
                canRetrieveRemedy = true,
            ),
        )
        assertEquals(UneffectAction.UseItem(UneffectRemovableMaps.ANCIENT_CURE_ALL), action)
    }

    @Test
    fun resolve_noItemsWithRetrieve_returnsRemedyRetrieve() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 999,
                canRetrieveRemedy = true,
            ),
        )
        assertEquals(
            UneffectAction.UseItem(UneffectRemovableMaps.REMEDY, retrieveFirst = true),
            action,
        )
    }

    @Test
    fun canUseHotTub_falseWhenSoaksExhausted() {
        val preferences = prefs { setInt("_hotTubSoaks", 5) }
        assertTrue(
            !UneffectActionResolver.canUseHotTub(
                ctx(
                    effectId = 42,
                    preferences = preferences,
                    itemIds = setOf(BreakfastManager.VIP_LOUNGE_KEY_ID),
                ),
            ),
        )
    }

    @Test
    fun canUseHotTub_falseWhenBadMoon() {
        assertTrue(
            !UneffectActionResolver.canUseHotTub(
                ctx(
                    effectId = 42,
                    characterState = CharacterState(zodiacSign = "Bad Moon", roninLeft = 1),
                    itemIds = setOf(BreakfastManager.VIP_LOUNGE_KEY_ID),
                ),
            ),
        )
    }

    @Test
    fun resolve_moodPredefinedUseItem_beatsHttpUneffect() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 999,
                moodPredefinedAction = "use 1 [829]",
                itemIds = setOf(829),
            ),
        )
        assertEquals(UneffectAction.UseItem(829), action)
    }

    @Test
    fun resolve_moodPredefinedCastSkill_whenOwned() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 42,
                moodPredefinedAction = "cast Shake It Off",
                hasSkill = { it.equals("Shake It Off", ignoreCase = true) },
                canCastSkill = { it.equals("Shake It Off", ignoreCase = true) },
            ),
        )
        assertEquals(UneffectAction.CastSkill("Shake It Off"), action)
    }

    @Test
    fun resolve_moodPredefinedUneffectPrefix_fallsThroughToSkill() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 42,
                moodPredefinedAction = "uneffect Beaten Up",
                hasSkill = { it.equals("Shake It Off", ignoreCase = true) },
                canCastSkill = { it.equals("Shake It Off", ignoreCase = true) },
            ),
        )
        assertEquals(UneffectAction.CastSkill("Shake It Off"), action)
    }

    @Test
    fun resolve_moodPredefinedCastWithoutSkill_fallsThrough() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 8,
                moodPredefinedAction = "cast Shake It Off",
                hasSkill = { false },
                itemIds = setOf(829),
            ),
        )
        assertEquals(UneffectAction.UseItem(829), action)
    }

    @Test
    fun resolve_poisonWithAcquirableAntidote_returnsRetrieveUseItem() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 8,
                canAcquireUneffectItem = { it == 829 },
            ),
        )
        assertEquals(UneffectAction.UseItem(829, retrieveFirst = true), action)
    }

    @Test
    fun resolve_needsCocoaWithoutAcquisition_returnsHttpUneffect() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 1278,
                canAcquireUneffectItem = { false },
            ),
        )
        assertIs<UneffectAction.HttpUneffect>(action)
    }

    @Test
    fun resolve_needsCocoaWithAcquirableCocoa_returnsRetrieveUseItem() {
        val action = UneffectActionResolver.resolve(
            ctx(
                effectId = 1278,
                canAcquireUneffectItem = { it == UneffectRemovableMaps.HOT_DREADSYLVANIAN_COCOA },
            ),
        )
        assertEquals(
            UneffectAction.UseItem(UneffectRemovableMaps.HOT_DREADSYLVANIAN_COCOA, retrieveFirst = true),
            action,
        )
    }
}
