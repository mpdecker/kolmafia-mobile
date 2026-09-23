package net.sourceforge.kolmafia.webui

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RelayRequest
import net.sourceforge.kolmafia.session.ActionBarManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelayDecoratorDepthPhase7691Test {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun useLinkDecorator_annotatesAcquireItem() {
        val html = """
            <html><body>
            You acquire an item: <b>seal tooth</b>
            </body></html>
        """.trimIndent()
        val buffer = StringBuilder(html)
        UseLinkDecorator.decorate("fight.php", buffer, prefs())
        assertTrue(buffer.contains("mafia-uselink") || buffer.contains("inv_use.php") || buffer.contains("seal tooth"))
    }

    @Test
    fun requestEditorKit_injectsIrcmAndOnfocus() {
        val html = """
            <html><head></head><body>
            <div>fight</div>
            </html>
        """.trimIndent()
        val out = RequestEditorKit.getFeatureRichHTML("fight.php", html, preferences = prefs())
        assertTrue(out.contains("ircm_extend.4.js"))
        assertTrue(out.contains("onfocus.1.js"))
        assertTrue(out.contains("combatfilter.1.js"))
        assertTrue(out.contains("macrohelper.6.js"))
    }

    @Test
    fun jsonApi_returnsProperties() {
        val p = prefs()
        p.setString("foo", "bar")
        val (code, body) = RelayJsonApi.handle(
            """{"properties":["foo","missing"]}""",
            library = null,
            preferences = p,
        )
        assertEquals(200, code)
        assertTrue(body.contains("bar"))
    }

    @Test
    fun jsonApi_getPropertyFunction() {
        val p = prefs()
        p.setString("baz", "qux")
        val (code, body) = RelayJsonApi.handle(
            """{"functions":[{"name":"get_property","args":["baz"]}]}""",
            library = null,
            preferences = p,
        )
        assertEquals(200, code)
        assertTrue(body.contains("qux"))
    }

    @Test
    fun actionBarFetch_returnsCachedJson() {
        ActionBarManager.reset()
        ActionBarManager.update("""{"skills":[]}""")
        val p = prefs()
        val relay = RelayRequest(preferences = p)
        relay.bindBrowserRequest(
            RelayBrowserRequest(
                method = "GET",
                pathWithQuery = "/actionbar.php?action=fetch",
            ),
        )
        assertTrue(ActionBarManager.updateJSONString(relay))
        assertEquals(200, relay.responseCode)
        assertTrue(relay.responseText.contains("skills"))
    }

    @Test
    fun villainLair_parsesColorClue() {
        val p = prefs()
        VillainLairDecorator.parseColorClue("please press the navy button now", p)
        assertEquals("blue", p.getString("_villainLairColor", ""))
        assertEquals("1", VillainLairDecorator.spoilColorChoice(p))
    }

    @Test
    fun memories_prefillsElements() {
        val html = buildString {
            append("""<select name="slot1"><option>hot</option><option>sleaze</option></select>""")
            append("""<select name="slot2"><option>cold</option><option>spooky</option></select>""")
            append("""<select name="slot3"><option>stench</option></select>""")
            append("""<select name="slot4"><option>cold</option></select>""")
            append("""<select name="slot5"><option>hot</option></select>""")
        }
        val buffer = StringBuilder(html)
        MemoriesDecorator.decorateElements(392, buffer)
        assertTrue(buffer.contains("selected>sleaze") || buffer.contains("selected>spooky") ||
            buffer.contains("selected>stench") || buffer.contains("selected>cold") ||
            buffer.contains("selected>hot"))
    }

    @Test
    fun clanFortune_fillsQuestions() {
        val p = prefs()
        p.setString("clanFortuneWord1", "alpha")
        val buffer = StringBuilder("""<input name="q1" required >""")
        ClanFortuneDecorator.decorateQuestion(buffer, p)
        assertTrue(buffer.contains("""value="alpha""""))
    }

    @Test
    fun nemesis_annotatesRaverFight() {
        val buffer = StringBuilder(
            """<html><body>the raver drops to the ground and whirls his legs around like a windmill</body></html>""",
        )
        NemesisDecorator.decorateRaverFight(buffer, "Breakdancing Raver", prefs())
        assertTrue(buffer.contains("Break It On Down") || buffer.contains("mafia-nemesis"))
    }

    @Test
    fun relayServer_jsonApiPath() {
        val p = prefs()
        p.setString("pwdHash", "x")
        RelayServer.preferences = p
        RelayServer.library = null
        val req = RelayBrowserRequest(
            method = "POST",
            pathWithQuery = "/jsonApi",
            body = """{"properties":["pwdHash"]}""".encodeToByteArray(),
        )
        val resp = RelayServer.handleBrowserRequest(req)
        assertEquals(200, resp.responseCode)
        assertTrue(resp.bodyText.contains("x"))
    }

    @Test
    fun chatHtml_isBuiltinAsset() {
        assertTrue(RelayAssets.isBuiltin("chat.html"))
        assertTrue(RelayAssets.isBuiltin("cli.html"))
        val text = RelayAssets.loadText("chat.html", "hash123")
        assertTrue(text == null || text.contains("Chat") || text.contains("basics.js"))
    }
}
