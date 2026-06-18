package net.sourceforge.kolmafia.servant

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class EdServantChoiceSyncTest {

    private val freedCatRow = """
        <tr><td valign=top><img src="http://images.kingdomofloathing.com/itemimages/edserv1.gif"></td>
        <td>Hethys, the Cat<br><span class=tiny></span></td>
        <td valign=top>(level 14, 221 xp)</td></tr>
    """.trimIndent()

    private val freedMaidRow = """
        <tr><td valign=top><img src="itemimages/edserv3.gif"></td>
        <td>Chambermaid, the Maid<br><span class=tiny></span></td>
        <td valign=top>(level 5, 42 xp)</td></tr>
    """.trimIndent()

    @Test
    fun parseSummonedTypes_freedTableOnly() {
        val html = """
            <b>Freed, but Lazy Servants</b><table>
            $freedCatRow
            $freedMaidRow
            </table>
        """.trimIndent()
        assertEquals(listOf("Cat", "Maid"), EdServantChoiceSync.parseSummonedTypes(html))
    }

    @Test
    fun parseSummonedTypes_includesBusyServant() {
        val html = """
            <b>Busy Servant</b>: <img src="itemimages/edserv6.gif">Priestess, the Priest (lvl. 12, 1,234 XP)
            <b>Freed, but Lazy Servants</b><table>$freedCatRow</table>
        """.trimIndent()
        val result = EdServantChoiceSync.parse(html)
        assertEquals(listOf("Priest", "Cat"), result.summonedTypes)
        assertEquals("Priest", result.activeType)
        assertEquals(12, result.records.first { it.type == "Priest" }.level)
        assertEquals(1234, result.records.first { it.type == "Priest" }.experience)
    }

    @Test
    fun parse_recordsIncludeFreedRowLevelAndXp() {
        val html = """<b>Freed, but Lazy Servants</b><table>$freedCatRow</table>"""
        val record = EdServantChoiceSync.parse(html).records.single()
        assertEquals("Cat", record.type)
        assertEquals("Hethys", record.name)
        assertEquals(14, record.level)
        assertEquals(221, record.experience)
    }

    @Test
    fun syncFromChoice1053_populatesSummonedPrefWithoutManualSeed() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Test",
                    classId = "7",
                    path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName,
                ),
            )
        }
        val manager = EdServantManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            prefs,
            char,
        )
        val html = """<b>Freed, but Lazy Servants</b><table>$freedCatRow</table>"""
        manager.syncFromChoice1053(html)
        assertEquals("Cat", prefs.getString(EdServantManager.SERVANTS_PREF, ""))
        assertEquals(false, prefs.getString(EdServantManager.SERVANTS_PREF, "").contains("Maid"))
    }
}
