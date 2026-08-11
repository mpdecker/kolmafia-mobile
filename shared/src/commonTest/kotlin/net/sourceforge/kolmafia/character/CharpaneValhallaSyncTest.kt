package net.sourceforge.kolmafia.character

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData as DbEffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class CharpaneValhallaSyncTest {

    @AfterTest
    fun tearDown() {
        CharpaneValhallaSync.reset()
    }

    @Test
    fun isValhallaHtml_spiritGif() {
        val html = """<img src="otherimages/spirit.gif">"""
        assertTrue(CharpaneValhallaSync.isValhallaHtml(html, limitMode = ""))
    }

    @Test
    fun isValhallaHtml_compactLevelImg() {
        val html = """<br>Lvl. <img src="level.gif">"""
        assertTrue(CharpaneValhallaSync.isValhallaHtml(html, limitMode = "none"))
    }

    @Test
    fun isValhallaHtml_rejectsLimitMode() {
        val html = """<img src="otherimages/spirit.gif">"""
        assertFalse(CharpaneValhallaSync.isValhallaHtml(html, limitMode = "spelunky"))
    }

    @Test
    fun isValhallaHtml_normalCharpane() {
        val html = """<br>Lvl. 5>Mus</td><td><b>50</b></td>"""
        assertFalse(CharpaneValhallaSync.isValhallaHtml(html, limitMode = ""))
    }

    @Test
    fun parseKarma_compact() {
        val html = """Karma: <b>122</b>"""
        assertEquals(122, CharpaneValhallaSync.parseKarma(html, compact = true))
    }

    @Test
    fun parseKarma_expanded() {
        val html = """karma.gif" width=30 height=30><br>55</td>"""
        assertEquals(55, CharpaneValhallaSync.parseKarma(html, compact = false))
    }

    @Test
    fun apply_setsSpiritStatsAndKarma() {
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(
                level = "15",
                buffedmus = "100",
                buffedmys = "80",
                buffedmox = "60",
                hp = "500",
                hpmax = "600",
                mp = "200",
                mpmax = "250",
                meat = "9999",
                adventures = "40",
            ),
        )
        val prefs = Preferences(MapSettings())
        val html = """
            <br>Lvl. <img src="otherimages/spirit.gif">
            Karma: <b>88</b>
        """.trimIndent()
        CharpaneValhallaSync.apply(character, html, prefs, effectManager = null)

        val state = character.state.value
        assertTrue(CharpaneValhallaSync.inValhalla)
        assertEquals(1, state.buffedMusc)
        assertEquals(1, state.buffedMyst)
        assertEquals(1, state.buffedMoxie)
        assertEquals(1, state.currentHp)
        assertEquals(1, state.maxHp)
        assertEquals(1, state.currentMp)
        assertEquals(1, state.maxMp)
        assertEquals(0, state.meat)
        assertEquals(0, state.adventuresLeft)
        assertEquals(0, state.mindControlLevel)
        assertEquals(88, prefs.getInt("bankedKarma"))
    }

    @Test
    fun apply_clearsEffects() {
        EffectDatabase.registerForTest(
            DbEffectData(
                id = 9001,
                name = "Valhalla Test Effect",
                image = "val.gif",
                descId = "valhalladesc",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
            ),
        )
        val character = KoLCharacter()
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val effectManager = EffectManager(client, GameEventBus())
        effectManager.applyEffectsFromCharpane(
            """
            <br>Lvl. 5
            <img alt="Valhalla Test Effect" onClick='eff("valhalladesc",1);'><td>(5)
            """.trimIndent(),
        )
        assertTrue(effectManager.state.value.effects.isNotEmpty())

        val html = """<br>Lvl. <img src="otherimages/spirit.gif"> Karma: <b>0</b>"""
        CharpaneValhallaSync.apply(character, html, preferences = null, effectManager = effectManager)
        assertTrue(effectManager.state.value.effects.isEmpty())
    }

    @Test
    fun reset_clearsInValhallaFlag() {
        CharpaneValhallaSync.apply(
            KoLCharacter(),
            """<img src="otherimages/spirit.gif">""",
            preferences = null,
            effectManager = null,
        )
        assertTrue(CharpaneValhallaSync.inValhalla)
        CharpaneValhallaSync.reset()
        assertFalse(CharpaneValhallaSync.inValhalla)
    }
}
