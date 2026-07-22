package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MonsterConsequenceDatabaseTest {

    @AfterTest
    fun tearDown() {
        MonsterConsequenceDatabase.resetForTest()
    }

    @Test
    fun parseForTest_loadsAllMonsterRows() {
        val parsed = MonsterConsequenceDatabase.parseForTest(
            """
            MONSTER	Ed the Undying	/ed(\d)\.gif	"Ed the Undying ($1)"
            MONSTER	Ed the Undying	.	"Ed the Undying (1)"
            MONSTER	Count Drunkula	drunkula_hm\.gif	"Count Drunkula (Hard Mode)"
            MONSTER	Falls-From-Sky	fallsfromsky_hm\.gif	"Falls-From-Sky (Hard Mode)"
            MONSTER	Great Wolf of the Air	wolfoftheair_hm\.gif	"Great Wolf of the Air (Hard Mode)"
            MONSTER	Mayor Ghost	mayorghost_hm\.gif	"Mayor Ghost (Hard Mode)"
            MONSTER	The Unkillable Skeleton	ukskeleton_hm\.gif	"The Unkillable Skeleton (Hard Mode)"
            MONSTER	Zombie Homeowners' Association	zombiehoa_hm\.gif	"Zombie Homeowners' Association (Hard Mode)"
            """.trimIndent(),
        )
        assertEquals(7, parsed.size)
        assertEquals(2, parsed["Ed the Undying"]?.size)
        assertEquals(1, parsed["Count Drunkula"]?.size)
        val edGifRule = parsed["Ed the Undying"]?.first()
        val replacement = edGifRule?.actions?.single()
        assertIs<ConsequenceAction.ReturnReplacement>(replacement)
        assertEquals("Ed the Undying (\$1)", replacement.template)
    }

    @Test
    fun parseForTest_skipsRowsWithoutQuotedReplacement() {
        val parsed = MonsterConsequenceDatabase.parseForTest(
            """
            MONSTER	Ed the Undying	/ed(\d)\.gif	edUses=$1
            """.trimIndent(),
        )
        assertEquals(0, parsed.size)
    }
}
