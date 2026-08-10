package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UneffectRemovableMapsTest {

    @AfterTest
    fun tearDown() {
        UneffectRemovableMaps.reset { false }
    }

    @Test
    fun reset_withoutAdventurerOfLeisure_discoNapHasBaseRemovablesOnly() {
        UneffectRemovableMaps.reset { false }
        assertEquals(6, UneffectRemovableMaps.removableEffectCountForSkill("Disco Nap"))
    }

    @Test
    fun reset_withAdventurerOfLeisure_expandsDiscoNapRemovables() {
        UneffectRemovableMaps.reset { name ->
            name.equals("Adventurer of Leisure", ignoreCase = true)
        }
        assertEquals(21, UneffectRemovableMaps.removableEffectCountForSkill("Disco Nap"))
    }

    @Test
    fun getUneffectSkill_aolOnlyEffect_returnsDiscoNapWhenAoLOwned() {
        UneffectRemovableMaps.reset { name ->
            name.equals("Adventurer of Leisure", ignoreCase = true)
        }
        val skillName = UneffectRemovableMaps.getUneffectSkill(388) {
            it.equals("Adventurer of Leisure", ignoreCase = true) ||
                it.equals("Disco Nap", ignoreCase = true)
        }
        assertEquals("Disco Nap", skillName)
    }

    @Test
    fun getUneffectSkill_aolOnlyEffect_emptyWithoutAoL() {
        UneffectRemovableMaps.reset { false }
        val skillName = UneffectRemovableMaps.getUneffectSkill(388) {
            it.equals("Disco Nap", ignoreCase = true)
        }
        assertTrue(skillName.isEmpty())
    }

    @Test
    fun reset_buildsItemRemovableMap() {
        UneffectRemovableMaps.reset { false }
        assertEquals(5, UneffectRemovableMaps.removableEffectIdsForItem(829).size)
    }

    @Test
    fun getUneffectItemId_poison_returnsAntidote() {
        UneffectRemovableMaps.reset { false }
        assertEquals(829, UneffectRemovableMaps.getUneffectItemId(8))
    }

    @Test
    fun removableByShakeItOff_sunburned_true() {
        UneffectRemovableMaps.reset { false }
        assertTrue(UneffectRemovableMaps.removableByShakeItOff(42))
    }

    @Test
    fun remedyConstants_matchDesktopItemPool() {
        assertEquals(588, UneffectRemovableMaps.REMEDY)
        assertEquals(7982, UneffectRemovableMaps.ANCIENT_CURE_ALL)
        assertEquals(6594, UneffectRemovableMaps.HOT_DREADSYLVANIAN_COCOA)
    }

    @Test
    fun needsCocoa_nauseated_true() {
        assertTrue(UneffectRemovableMaps.needsCocoa(1278))
    }

    @Test
    fun needsCocoa_curseOfClumsiness_true() {
        assertTrue(UneffectRemovableMaps.needsCocoa(1313))
    }

    @Test
    fun needsCocoa_beatenUp_false() {
        assertTrue(!UneffectRemovableMaps.needsCocoa(7))
    }
}
