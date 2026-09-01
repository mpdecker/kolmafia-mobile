package net.sourceforge.kolmafia.adventure.choice

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoiceGoalCatalogTest {

    @Test
    fun violetFogChoicesHaveGoalButton() {
        assertTrue(ChoiceGoalCatalog.hasGoalButton(48))
        assertTrue(ChoiceGoalCatalog.hasGoalButton(70))
    }

    @Test
    fun unrelatedChoiceHasNoGoalButton() {
        assertFalse(ChoiceGoalCatalog.hasGoalButton(191))
    }
}
