package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.QuestLogConsequenceDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SummoningChamberManager
import net.sourceforge.kolmafia.request.SummoningChamberRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

class QuestLogConsequenceSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun injectDemonRules() {
        QuestLogConsequenceDatabase.injectForTest(
            QuestLogConsequenceDatabase.parseForTest(
                """
                QUEST_LOG	demonName1	;&middot;([^<]*?), Lord of the Pies<br	demonName1=${'$'}1
                QUEST_LOG	demonName9	;&middot;([^<]*?), the Smith<br	demonName9=${'$'}1
                QUEST_LOG	demonName14	;&middot;([^<]*?), Bane of Allies<br	demonName14=${'$'}1
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun applyAccomplishments_setsDemonName9() {
        injectDemonRules()
        val p = prefs()
        QuestLogConsequenceSync.applyAccomplishments(
            ";&middot;Ak'gyxoth, the Smith<br",
            p,
        )
        assertEquals("Ak'gyxoth", p.getString("demonName9", ""))
    }

    @Test
    fun applyAccomplishments_setsDemonName1() {
        injectDemonRules()
        val p = prefs()
        QuestLogConsequenceSync.applyAccomplishments(
            ";&middot;Pie Demon, Lord of the Pies<br",
            p,
        )
        assertEquals("Pie Demon", p.getString("demonName1", ""))
    }

    @Test
    fun applyAccomplishments_noMatch_leavesPrefUnchanged() {
        injectDemonRules()
        val p = prefs()
        p.setString("demonName9", "existing")
        QuestLogConsequenceSync.applyAccomplishments("<html>no demons here</html>", p)
        assertEquals("existing", p.getString("demonName9", ""))
    }

    @Test
    fun applyAccomplishments_ascensionsAction() {
        QuestLogConsequenceDatabase.injectForTest(
            QuestLogConsequenceDatabase.parseForTest(
                "QUEST_LOG	lastPlusSignUnlock	You have discovered the secret of the Dungeons of Doom	lastPlusSignUnlock=ascensions",
            ),
        )
        val p = prefs()
        QuestLogConsequenceSync.applyAccomplishments(
            "You have discovered the secret of the Dungeons of Doom",
            p,
            ascensionNumber = 7,
        )
        assertEquals(7, p.getInt("lastPlusSignUnlock", 0))
    }

    @Test
    fun applyAccomplishments_skipsModifierExpressionRules() {
        val parsed = QuestLogConsequenceDatabase.parseForTest(
            "QUEST_LOG	royalty	You have accumulated ([\\d,]+) Royalty	royalty=[\$1]",
        )
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun resolveDemon_findsNameAfterSync() {
        injectDemonRules()
        val p = prefs()
        QuestLogConsequenceSync.applyAccomplishments(
            ";&middot;Smithy, the Smith<br",
            p,
        )
        val mgr = SummoningChamberManager(
            p,
            SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })),
            null,
            null,
            null,
            null,
        )
        val resolved = mgr.resolveDemon("9")
        assertEquals(9, resolved?.number)
        assertEquals("Smithy", resolved?.name)
    }
}
