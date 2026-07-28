package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.TinkeringBenchGates

class GameRuntimeLibraryAshP162Test {

    private companion object {
        const val BROBERRY_BROGURT = 7455
        const val BEACH_BUCK = 7429
        const val YAK_SKIN = 394
        const val BIPHASIC_OCULUS = 11550
        const val MILD_MANNERED_PROFESSOR = 2897
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
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

    @Test
    fun brogurt_blockedBeforeSbbPrefsAllowedAfter() {
        registerItem(BROBERRY_BROGURT, "broberry brogurt")
        registerItem(BEACH_BUCK, "Beach Buck")
        CoinmasterDatabase.loadFromText(
            shopsText = "sbb_brogurt\tThe Frozen Brogurt Stand\n",
            coinText = "The Frozen Brogurt Stand\tbuy\t10\tbroberry brogurt\tROW295\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                BROBERRY_BROGURT,
                state,
                p,
                accessibleCount = { if (it == BEACH_BUCK) 100 else 0 },
            ),
        )
        p.setBoolean("_sleazeAirportToday", true)
        p.setString("questESlBacteria", QuestDatabase.FINISHED)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                BROBERRY_BROGURT,
                state,
                p,
                accessibleCount = { if (it == BEACH_BUCK) 100 else 0 },
            ),
        )
    }

    @Test
    fun trapper_yakSkinBlockedBeforeQuestAllowedAfter() {
        registerItem(YAK_SKIN, "yak skin")
        CoinmasterDatabase.loadFromText(
            shopsText = "trapper\tThe Trapper\n",
            coinText = "The Trapper\tbuy\t1\tyak skin\tROW14\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(level = 10, ascensionNumber = 4, meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                YAK_SKIN,
                state,
                p,
                accessibleCount = { 0 },
            ),
        )
        p.setInt("lastTr4pz0rQuest", 4)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                YAK_SKIN,
                state,
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun tinkering_oculusBlockedWithoutProfessorEffect() {
        registerItem(BIPHASIC_OCULUS, "biphasic molecular oculus")
        CoinmasterDatabase.loadFromText(
            shopsText = "wereprofessor_tinker\tTinkering Bench\n",
            coinText = "Tinkering Bench\tbuy\t1\tbiphasic molecular oculus\tROW1467\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                BIPHASIC_OCULUS,
                state,
                p,
                hasEffect = { false },
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                BIPHASIC_OCULUS,
                state,
                p,
                hasEffect = { it == MILD_MANNERED_PROFESSOR },
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun tinkering_oculusBlockedWhenUpgradeTreeOwned() {
        assertFalse(
            TinkeringBenchGates.canMakeItem(BIPHASIC_OCULUS) { id ->
                if (id == BIPHASIC_OCULUS) 1 else 0
            },
        )
        assertTrue(
            TinkeringBenchGates.canMakeItem(BIPHASIC_OCULUS) { 0 },
        )
    }
}
