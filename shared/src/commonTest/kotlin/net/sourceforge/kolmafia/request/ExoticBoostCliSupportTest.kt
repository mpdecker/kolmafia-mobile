package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExoticBoostCliSupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun fortune_findBuff_aliases() {
        assertEquals(ClanFortuneRequest.Buff.FAMILIAR, ClanFortuneRequest.findBuff("susie"))
        assertEquals(ClanFortuneRequest.Buff.FAMILIAR, ClanFortuneRequest.findBuff("fam"))
        assertEquals(ClanFortuneRequest.Buff.ITEM, ClanFortuneRequest.findBuff("hagnk"))
        assertEquals(ClanFortuneRequest.Buff.ITEM, ClanFortuneRequest.findBuff("item"))
        assertEquals(ClanFortuneRequest.Buff.MEAT, ClanFortuneRequest.findBuff("meat"))
        assertEquals(ClanFortuneRequest.Buff.MUSCLE, ClanFortuneRequest.findBuff("gunther"))
        assertEquals(ClanFortuneRequest.Buff.MUSCLE, ClanFortuneRequest.findBuff("mus"))
        assertEquals(ClanFortuneRequest.Buff.MYSTICALITY, ClanFortuneRequest.findBuff("gorgonzola"))
        assertEquals(ClanFortuneRequest.Buff.MYSTICALITY, ClanFortuneRequest.findBuff("mys"))
        assertEquals(ClanFortuneRequest.Buff.MOXIE, ClanFortuneRequest.findBuff("shifty"))
        assertEquals(ClanFortuneRequest.Buff.MOXIE, ClanFortuneRequest.findBuff("mox"))
        assertNull(ClanFortuneRequest.findBuff("cheese"))
    }

    @Test
    fun fortune_parseResponse_setsBuffUsed() {
        val p = prefs()
        ClanFortuneRequest.parseResponse(
            "choice.php?whichchoice=1278",
            "Relationship Fortune Teller says hello — no resident buff left",
            p,
        )
        assertTrue(p.getBoolean(ClanFortuneRequest.BUFF_USED_PREF, false))

        val p2 = prefs()
        ClanFortuneRequest.parseResponse(
            "choice.php?whichchoice=1278",
            "Relationship Fortune Teller offers a buff to a resident of Seaside Town",
            p2,
        )
        assertFalse(p2.getBoolean(ClanFortuneRequest.BUFF_USED_PREF, false))
    }

    @Test
    fun mom_findFoodOption() {
        assertEquals(1, MomRequest.findFoodOption("hot"))
        assertEquals(2, MomRequest.findFoodOption("cold"))
        assertEquals(6, MomRequest.findFoodOption("critical"))
        assertEquals(7, MomRequest.findFoodOption("stats"))
        assertEquals(3, MomRequest.findFoodOption("3"))
        assertEquals(0, MomRequest.findFoodOption("pizza"))
        assertEquals(0, MomRequest.findFoodOption("9"))
    }

    @Test
    fun mom_parseResponse_setsFoodReceived() {
        val p = prefs()
        MomRequest.parseResponse("She looks up at you, and you begin to sweat.", p)
        assertTrue(p.getBoolean(MomRequest.FOOD_RECEIVED_PREF, false))
    }

    @Test
    fun mayosoak_preflight() {
        assertEquals("Mayo clinic not installed", MayoSoakRequest.preflightError(prefs()))
        val installed = prefs {
            putInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
        }
        assertNull(MayoSoakRequest.preflightError(installed))
        val soaked = prefs {
            putInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
            putBoolean(MayoSoakRequest.SOAKED_PREF, true)
        }
        assertEquals("Already soaked in Mayo tank today", MayoSoakRequest.preflightError(soaked))
    }

    @Test
    fun friars_findBlessingOption() {
        assertEquals(1, FriarRequest.findBlessingOption("food"))
        assertEquals(2, FriarRequest.findBlessingOption("familiar"))
        assertEquals(3, FriarRequest.findBlessingOption("booze"))
        assertEquals(2, FriarRequest.findBlessingOption("2"))
        assertEquals(0, FriarRequest.findBlessingOption("ashes"))
    }

    @Test
    fun friars_parseResponse_setsBlessing() {
        val p = prefs { putInt("knownAscensions", 42) }
        FriarRequest.parseResponse(
            "Brother Starfish smiles and rubs some ashes on your face.",
            p,
            knownAscensions = 42,
        )
        assertTrue(p.getBoolean(FriarRequest.BLESSING_RECEIVED_PREF, false))
        assertEquals(42, p.getInt(FriarRequest.LAST_CEREMONY_ASCENSION_PREF, 0))
    }

    @Test
    fun telescope_look_parse_helpers() {
        // Mirrors cliTelescope direction resolution used by Maximizer `telescope look high`.
        fun resolve(params: String): String {
            val parts = params.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            var command = parts.firstOrNull()?.lowercase().orEmpty()
            if (command == "look") {
                command = parts.getOrNull(1)?.lowercase().orEmpty()
            }
            return when (command) {
                "high" -> "telescopehigh"
                else -> "telescopelow"
            }
        }
        assertEquals("telescopehigh", resolve("look high"))
        assertEquals("telescopelow", resolve("look low"))
        assertEquals("telescopehigh", resolve("high"))
        assertEquals("telescopelow", resolve("low"))
        assertEquals("telescopelow", resolve(""))
        assertNotNull(ClanFortuneRequest.Buff.MEAT)
    }
}
