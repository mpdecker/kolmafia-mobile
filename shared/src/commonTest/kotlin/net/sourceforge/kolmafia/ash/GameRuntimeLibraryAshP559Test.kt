package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP559Test {

    private val client = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun skill(name: String) = SkillData(
        id = name.hashCode() and 0xffff,
        name = name,
        type = SkillType.NONCOMBAT,
        mpCost = 0,
        dailyLimit = 1,
        timesCast = 0,
    )

    private fun breakfast(prefs: Preferences, casts: MutableList<Pair<String, Int>>): BreakfastManager {
        val skillMgr = object : SkillManager(client, SkillCastRequest(client), GameEventBus()) {
            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                casts += skill.name to quantity
                return Result.success(Unit)
            }
        }
        skillMgr.learnLocalSkill(skill("Pastamastery"))
        skillMgr.learnLocalSkill(skill("Summon Snowcones"))
        val campground = object : CampgroundRequest(client) {
            override suspend fun harvestGarden() = Result.success(Unit)
            override suspend fun useSpinningWheel() = Result.success("ok")
        }
        return BreakfastManager(
            campgroundRequest = campground,
            clanRumpusRequest = object : ClanRumpusRequest(client) {
                override suspend fun visit() = Result.success(Unit)
            },
            clanLoungeRequest = object : ClanLoungeRequest(client) {
                override suspend fun useKlaw() = Result.success("ok")
            },
            preferences = prefs,
            useItemRequest = UseItemRequest(client),
            hermitRequest = HermitRequest(client),
            httpClient = client,
            skillManager = skillMgr,
        )
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun breakfast_skills_castsWithoutCompleting() = runBlocking {
        val casts = mutableListOf<Pair<String, Int>>()
        val p = prefs { putString("breakfastSoftcore", "Pastamastery") }
        val mgr = breakfast(p, casts)
        val char = KoLCharacter()
        char.updateHpMp(currentHp = 0, maxHp = 0, currentMp = 50, maxMp = 50)
        val inv = InventoryManager(client, GameEventBus())
        outputLib(
            GameRuntimeLibrary(
                breakfastManager = mgr,
                character = char,
                inventoryManager = inv,
            ),
            """cli_execute("breakfast skills");""",
        )
        assertTrue(casts.any { it.first == "Pastamastery" })
        assertFalse(p.getBoolean(Preferences.BREAKFAST_COMPLETED, false))
    }

    @Test
    fun breakfast_books_castsWithoutCompleting() = runBlocking {
        val casts = mutableListOf<Pair<String, Int>>()
        val p = prefs { putString("tomeSkillsSoftcore", "Summon Snowcones") }
        val mgr = breakfast(p, casts)
        val char = KoLCharacter()
        char.updateHpMp(currentHp = 0, maxHp = 0, currentMp = 50, maxMp = 50)
        val inv = InventoryManager(client, GameEventBus())
        outputLib(
            GameRuntimeLibrary(
                breakfastManager = mgr,
                character = char,
                inventoryManager = inv,
            ),
            """cli_execute("breakfast books");""",
        )
        assertTrue(casts.any { it.first == "Summon Snowcones" })
        assertFalse(p.getBoolean(Preferences.BREAKFAST_COMPLETED, false))
    }
}
