package net.sourceforge.kolmafia.quest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConsequenceActionResolverTest {

    private fun match(text: String, pattern: String): MatchResult =
        Regex(pattern).find(text)!!

    @Test
    fun substituteGroups_replacesCaptureGroups() {
        val m = match("You have accumulated 1,234 Royalty", """You have accumulated ([\d,]+) Royalty""")
        assertEquals("[1,234]", ConsequenceActionResolver.substituteGroups("[\$1]", m))
    }

    @Test
    fun evaluateBracketExpressions_stripsCommasFromNumericInner() {
        assertEquals("1234", ConsequenceActionResolver.evaluateBracketExpressions("[1,234]"))
    }

    @Test
    fun resolveValue_royaltyExpression() {
        val m = match("You have accumulated 5,678 Royalty", """You have accumulated ([\d,]+) Royalty""")
        assertEquals("5678", ConsequenceActionResolver.resolveValue("[\$1]", m))
    }

    @Test
    fun parseForTest_includesRoyaltyRule() {
        val parsed = net.sourceforge.kolmafia.data.QuestLogConsequenceDatabase.parseForTest(
            "QUEST_LOG	royalty	You have accumulated ([\\d,]+) Royalty	royalty=[\$1]",
        )
        assertEquals(1, parsed.size)
        val action = parsed[0].actions.single()
        assertIs<net.sourceforge.kolmafia.data.ConsequenceAction.SetExpressionValue>(action)
        assertEquals("royalty", action.key)
        assertEquals("[\$1]", action.rawValue)
    }

    @Test
    fun resolveLiteralValue_monstername() {
        val m = match(
            """<!-- monsterid: 42 -->""",
            """<!-- monsterid: (\d+) -->""",
        )
        kotlinx.coroutines.test.runTest {
            net.sourceforge.kolmafia.data.MonsterDatabase.load()
            val name = ConsequenceActionResolver.resolveLiteralValue("monstername", m)
            assertEquals(net.sourceforge.kolmafia.data.MonsterDatabase.getById(42)?.name, name)
        }
    }

    @Test
    fun resolveValue_romanExpressionAfterGroupSubstitution() {
        val m = match(
            """Maximum HP +<span style="font-family: times new roman">XX</span>""",
            """Maximum HP \+<span style="font-family: times new roman">(.*?)</span>""",
        )
        assertEquals("0", ConsequenceActionResolver.resolveValue("[(roman(\$1)/10)-2]", m))
    }
}
