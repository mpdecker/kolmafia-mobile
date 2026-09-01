package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GoalConditionParserTest {

    @Test
    fun parseChoiceAdventureCount() {
        val parsed = GoalConditionParser.parse("3 choice")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.CHOICE_ADVENTURES, parsed.kind)
        assertEquals(3, parsed.count)
    }

    @Test
    fun parseFloundryFish() {
        val parsed = GoalConditionParser.parse("2 floundry fish")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.FLOUNDRY, parsed.kind)
        assertEquals(2, parsed.count)
    }

    @Test
    fun parseItemWithCount() {
        val parsed = GoalConditionParser.parse("5 seal tooth")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.ITEM_NAME, parsed.kind)
        assertEquals(5, parsed.count)
        assertEquals("seal tooth", parsed.itemName)
    }

    @Test
    fun parseMeatCondition() {
        val parsed = GoalConditionParser.parse("10,000 meat")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.MEAT, parsed.kind)
        assertEquals(10000, parsed.count)
    }
}
