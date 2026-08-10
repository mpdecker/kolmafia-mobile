package net.sourceforge.kolmafia.mood

import kotlin.test.Test
import kotlin.test.assertEquals

class LibramSkillCastsTest {

    @Test fun libramSkillMpCost_matchesDesktopFormula() {
        assertEquals(1L, LibramSkillCasts.libramSkillMpCost(1))
        assertEquals(2L, LibramSkillCasts.libramSkillMpCost(2))
        assertEquals(4L, LibramSkillCasts.libramSkillMpCost(3))
        assertEquals(7L, LibramSkillCasts.libramSkillMpCost(4))
        assertEquals(11L, LibramSkillCasts.libramSkillMpCost(5))
    }

    @Test fun libramSkillCasts_countsAffordableCasts() {
        assertEquals(3, LibramSkillCasts.libramSkillCasts(libramSummonsPref = 0, availableMp = 7))
    }

    @Test fun libramSkillMpCost_negativeAdjustment_clampedAtOne() {
        assertEquals(1L, LibramSkillCasts.libramSkillMpCost(cast = 1, manaCostAdjustment = -5))
    }

    @Test fun libramSkillMpCost_positiveAdjustment_increasesCost() {
        assertEquals(6L, LibramSkillCasts.libramSkillMpCost(cast = 3, manaCostAdjustment = 2))
    }

    @Test fun libramSkillCasts_respectsAdjustment() {
        assertEquals(3, LibramSkillCasts.libramSkillCasts(libramSummonsPref = 0, availableMp = 7, manaCostAdjustment = 0))
        assertEquals(2, LibramSkillCasts.libramSkillCasts(libramSummonsPref = 0, availableMp = 7, manaCostAdjustment = 1))
    }

    @Test fun firstLibramBatch_rotatesFromLibramSummonsPref() {
        assertEquals(1 to 2, LibramSkillCasts.firstLibramBatch(totalCasts = 4, skillCount = 2, nextCastIndex = 1))
    }

    @Test fun libramSkillMpCostTotal_sumsSequentialCasts() {
        assertEquals(7L, LibramSkillCasts.libramSkillMpCostTotal(startCast = 1, count = 3))
    }
}
