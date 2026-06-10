package net.sourceforge.kolmafia.equipment

import kotlin.test.Test
import kotlin.test.assertEquals

class EmbeddedOutfitActionsTest {

    @Test
    fun displayName_stripsEmbeddedActions() {
        val raw = "Mining c=set mining=1 e=Baby Sand Bug"
        assertEquals("Mining", EmbeddedOutfitActions.displayName(raw))
    }

    @Test
    fun runAfterWear_expandsActionPrefixes() {
        val commands = mutableListOf<String>()
        EmbeddedOutfitActions.runAfterWear("x c=set foo f=Mosquito t=Crown") { commands += it }
        assertEquals(
            listOf("set foo", "familiar Mosquito", "enthrone Crown"),
            commands
        )
    }
}
