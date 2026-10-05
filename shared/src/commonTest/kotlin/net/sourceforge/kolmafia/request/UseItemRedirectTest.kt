package net.sourceforge.kolmafia.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemRedirectTest {

    @Test
    fun fight_fromUrl() {
        assertEquals(
            UseItemRedirect.Kind.Fight,
            UseItemRedirect.classify("ok", "https://www.kingdomofloathing.com/fight.php"),
        )
    }

    @Test
    fun fight_fromBody() {
        assertEquals(
            UseItemRedirect.Kind.Fight,
            UseItemRedirect.classify("You're fighting <b>a photocopied monster</b>"),
        )
    }

    @Test
    fun choice_fromWhichchoice() {
        val kind = UseItemRedirect.classify(
            """<form action=choice.php><input type="hidden" name="whichchoice" value="123">""",
        )
        assertEquals(UseItemRedirect.Kind.Choice(123), kind)
        assertTrue(UseItemRedirect.shouldSkipParseConsumption(kind))
    }

    @Test
    fun choice_fromUrl() {
        val kind = UseItemRedirect.classify(
            "You miraculously find yourself in a choice.",
            "https://www.kingdomofloathing.com/choice.php?forceoption=0&whichchoice=456",
        )
        assertEquals(UseItemRedirect.Kind.Choice(456), kind)
    }

    @Test
    fun none_skipsNothing() {
        val kind = UseItemRedirect.classify("You acquire an item: <b>meat</b>")
        assertEquals(UseItemRedirect.Kind.None, kind)
        assertFalse(UseItemRedirect.shouldSkipParseConsumption(kind))
    }
}
