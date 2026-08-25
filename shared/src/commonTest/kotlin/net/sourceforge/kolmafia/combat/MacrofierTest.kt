package net.sourceforge.kolmafia.combat

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

class MacrofierTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        CombatActionManager.resetForTest()
        ChoiceCombatAshState.reset()
        prefs = Preferences(MapSettings())
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun filterOverrideTakesPrecedence() {
        val result = Macrofier.macrofy(
            monsterName = "default",
            preferences = prefs,
            filterOverride = "abort \"filtered\"",
        )
        assertEquals("abort \"filtered\"", result)
    }

    @Test
    fun quotedFilterBecomesMacroBlock() {
        val result = Macrofier.macrofy(
            filterOverride = "\"skill saucegeyser; attack\"",
        )
        assertNotNull(result)
        assertContains(result!!, "#macro action")
        assertContains(result, "skill saucegeyser; attack")
    }

    @Test
    fun expandsCcsAttackAndAbort() {
        CombatActionManager.loadFromText(
            """
            [ default ]
            skill saucegeyser
            attack with weapon
            """.trimIndent(),
            preferences = prefs,
        )
        prefs.setString("battleAction", "custom combat script")
        val macro = Macrofier.macrofy("default", prefs)
        assertNotNull(macro)
        assertContains(macro!!, "hasskill saucegeyser")
        assertContains(macro, "attack")
        assertContains(macro, "mafiafinal")
    }

    @Test
    fun expandActionAbort() {
        assertContains(Macrofier.expandAction("abort"), "abort")
        assertEquals("call mafiaround; attack", Macrofier.expandAction("attack with weapon"))
        assertEquals("pickpocket", Macrofier.expandAction("try to steal an item"))
    }

    @Test
    fun macroDirectivesPassThrough() {
        CombatActionManager.loadFromText(
            """
            [ default ]
            if hasskill saucegeyser
            skill saucegeyser
            endif
            attack with weapon
            """.trimIndent(),
            preferences = prefs,
        )
        prefs.setString("battleAction", "custom combat script")
        val macro = Macrofier.macrofy("default", prefs)
        assertNotNull(macro)
        assertTrue(macro!!.contains("if hasskill saucegeyser"))
    }
}
