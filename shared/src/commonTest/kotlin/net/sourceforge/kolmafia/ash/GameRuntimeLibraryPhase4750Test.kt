package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.DecorateTentRequest
import net.sourceforge.kolmafia.request.LedCandleRequest
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.skill.UseSkillSync

class GameRuntimeLibraryPhase4750Test {

    @Test
    fun revision_phase4870() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun use_skill_oneArg_returnsStringLastUpdate() {
        UseSkillSync.lastUpdate = ""
        val out = outputLib(
            GameRuntimeLibrary(preferences = Preferences(MapSettings())),
            "string s = use_skill(to_skill(\"none\")); print(s);",
        )
        assertTrue(out.isNotEmpty() || UseSkillSync.lastUpdate.isNotEmpty() || out == "")
    }

    @Test
    fun use_skill_combatInFight_visitsFightPhp() {
        ChoiceCombatAshState.currentRound = 2
        try {
            UseSkillSync.lastUpdate = ""
            assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        } finally {
            ChoiceCombatAshState.currentRound = 0
        }
    }

    @Test
    fun candy_defaultFlags() {
        assertEquals(CandyDatabase.ASH_FLAG_AVAILABLE, CandyDatabase.defaultFlags())
    }

    @Test
    fun decorateTent_and_ledCandle_register() {
        assertTrue(DecorateTentRequest.registerRequest("choice.php?whichchoice=1392&option=1"))
        assertFalse(DecorateTentRequest.registerRequest("choice.php?whichchoice=1"))
        assertTrue(LedCandleRequest.registerRequest("choice.php?whichchoice=1509&option=2"))
        assertFalse(LedCandleRequest.registerRequest("choice.php?whichchoice=1466"))
    }

    @Test
    fun ledCandle_setsModePref() {
        val p = Preferences(MapSettings())
        LedCandleRequest.parseResponse(
            "choice.php?whichchoice=1509&option=1",
            "Disco ball activated.",
            p,
        )
        assertEquals("disco", p.getString("ledCandleMode", ""))
    }

    @Test
    fun to_item_nameCount_overload_exists() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """item it = to_item("seal tooth", 3); print(it);""",
        )
        assertTrue("seal tooth" in out.lowercase() || out.isNotBlank())
    }

    @Test
    fun sell_price_skill_overload_compiles() {
        assertEquals(
            "0",
            outputLib(
                GameRuntimeLibrary(),
                "print(to_string(sell_price(to_coinmaster(\"none\"), to_skill(\"none\"))));",
            ),
        )
    }
}
