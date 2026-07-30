package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class ItemMaximumUsesTest {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun food_usesFullnessRemaining() {
        val ctx = ctx(
            CharacterState(fullness = 10, fullnessLimit = 15),
        )
        assertEquals(5, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun drink_usesInebrietyRemaining() {
        val item = ItemDatabase.getByName("martini")!!
        val ctx = ctx(
            CharacterState(inebriety = 2, inebrietyLimit = 14),
        )
        assertEquals(4, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun spleen_usesDailyLimitWhenConfigured() {
        val item = ItemDatabase.getByName("turkey blaster")!!
        val ctx = ctx(
            CharacterState(spleenUsed = 0, spleenLimit = 15),
        )
        assertEquals(3, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun dailyUseLimit_exhaustedPrefReturnsZero() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_bagOfCandyUsed", true)
        val ctx = ctx(CharacterState(), prefs)
        assertEquals(0, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun dailyUseLimit_availableWhenPrefUnset() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val ctx = ctx(CharacterState(), Preferences(MapSettings()))
        assertEquals(1, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun restoreItem_capsByNeededHp() {
        val item = ItemDatabase.getByName("aspirin")!!
        val ctx = ctx(
            CharacterState(currentHp = 50, maxHp = 100, currentMp = 0, maxMp = 100),
        )
        assertEquals(1, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun limitMode_blocksFood() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15, limitMode = "spelunky"),
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun genericUsableItem_returnsMaxValue() {
        val item = ItemDatabase.getByName("ten-leaf clover")!!
        val ctx = ctx(CharacterState())
        assertEquals(Int.MAX_VALUE, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun multiFight_returnsZero() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15),
            inMultiFight = true,
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun choiceFollowsFight_returnsZero() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15),
            choiceFollowsFight = true,
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun spelunkyLimitItem_blocksOutOfRangeItem() {
        val ctx = ctx(
            CharacterState(limitMode = "spelunky"),
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun beecore_bannedItem_returnsZero() {
        val item = ItemDatabase.getByName("baseball")!!
        val ctx = ctx(
            CharacterState(challengePath = "Bees Hate You"),
        )
        assertEquals(0, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun beecore_exceptionItem_returnsMaxValue() {
        val item = ItemDatabase.getByName("ice baby")!!
        val ctx = ctx(
            CharacterState(challengePath = "Bees Hate You"),
        )
        assertEquals(Int.MAX_VALUE, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun cobsKnobMap_returnsEncryptionKeyCount() {
        val map = ItemDatabase.getByName("Cobb's Knob map")!!
        val ctx = ctx(
            CharacterState(),
            accessibleCount = { id ->
                if (id == ItemDatabase.ENCRYPTION_KEY) 3 else 0
            },
        )
        assertEquals(3, maximumUses(map.id, map.name, ctx))
    }

    @Test
    fun dailyLimitDatabase_getUsesRemaining() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val entry = DailyLimitDatabase.getEntry(item.id, DailyLimitKind.USE)!!
        val prefs = Preferences(MapSettings())
        prefs.setInt("_bagOfCandyUsed", 1)
        assertEquals(0, DailyLimitDatabase.getUsesRemaining(entry, prefs))
    }

    private fun ctx(
        character: CharacterState,
        preferences: Preferences? = null,
        inMultiFight: Boolean = false,
        choiceFollowsFight: Boolean = false,
        accessibleCount: (Int) -> Int = { 0 },
    ) = ItemUseLimitsContext(
        character = character,
        preferences = preferences,
        expressionContext = ExpressionContext(
            characterMaxHp = character.maxHp,
            characterMaxMp = character.maxMp,
            characterCurrentHp = character.currentHp,
            challengePath = character.challengePath,
        ),
        inMultiFight = inMultiFight,
        choiceFollowsFight = choiceFollowsFight,
        accessibleCount = accessibleCount,
    )
}
