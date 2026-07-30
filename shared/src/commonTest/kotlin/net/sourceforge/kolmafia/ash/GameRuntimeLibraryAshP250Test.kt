package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP250Test {

    @Test
    fun skillBracket_booleanTags() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("true", outputLib(lib, """print(to_skill("CLEESH")["combat"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_skill("CLEESH")["spell"]);""").trim())
        assertEquals("false", outputLib(lib, """print(to_skill("CLEESH")["passive"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_skill("Liver of Steel")["passive"]);""").trim())
        assertEquals("false", outputLib(lib, """print(to_skill("Liver of Steel")["permable"]);""").trim())
    }

    @Test
    fun skillBracket_runtimeCastFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val client = HttpClient(MockEngine { respond("ok") })
        val skillMgr = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skillMgr.learnLocalSkill(
            SkillData(
                id = 140,
                name = "Ancestral Recall",
                type = SkillType.NONCOMBAT,
                mpCost = 0,
                dailyLimit = 10,
                timesCast = 3,
            ),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, skillManager = skillMgr)
        assertEquals("10", outputLib(lib, """print(to_skill("Ancestral Recall")["dailylimit"]);""").trim())
        assertEquals("3", outputLib(lib, """print(to_skill("Ancestral Recall")["timescast"]);""").trim())
        assertEquals(
            "_ancestralRecallCasts",
            outputLib(lib, """print(to_skill("Ancestral Recall")["dailylimitpref"]);""").trim(),
        )
    }

    @Test
    fun skillBracket_unownedCastDefaults() = runBlocking {
        val db = GameDatabase()
        db.load()
        val client = HttpClient(MockEngine { respond("ok") })
        val skillMgr = SkillManager(client, SkillCastRequest(client), GameEventBus())
        val lib = GameRuntimeLibrary(gameDatabase = db, skillManager = skillMgr)
        assertEquals("-1", outputLib(lib, """print(to_skill("CLEESH")["dailylimit"]);""").trim())
        assertEquals("0", outputLib(lib, """print(to_skill("CLEESH")["timescast"]);""").trim())
    }

    @Test
    fun dailyLimitDatabase_castPrefBySkillId() = runBlocking {
        DailyLimitDatabase.resetForTest()
        val db = GameDatabase()
        db.load()
        assertEquals("_ancestralRecallCasts", DailyLimitDatabase.getCastPrefForSkill(140))
    }
}
