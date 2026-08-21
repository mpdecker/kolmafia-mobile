package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryState
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

class GameRuntimeLibraryAshP553Test {

    private val client = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun skillManager(vararg skills: SkillData, casts: MutableList<Pair<String, Int>>): SkillManager {
        val mgr = object : SkillManager(client, SkillCastRequest(client), GameEventBus()) {
            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                casts += skill.name to quantity
                return Result.success(Unit)
            }
        }
        skills.forEach { mgr.learnLocalSkill(it) }
        return mgr
    }

    private fun skill(name: String, mp: Int = 0, limit: Int = 1, cast: Int = 0) = SkillData(
        id = name.hashCode() and 0xffff,
        name = name,
        type = SkillType.NONCOMBAT,
        mpCost = mp,
        dailyLimit = limit,
        timesCast = cast,
    )

    private fun breakfast(
        prefs: Preferences,
        skillManager: SkillManager,
    ): BreakfastManager {
        val campground = object : CampgroundRequest(client) {
            override suspend fun harvestGarden() = Result.success(Unit)
            override suspend fun useSpinningWheel() = Result.success("ok")
        }
        val rumpus = object : ClanRumpusRequest(client) {
            override suspend fun visit() = Result.success(Unit)
        }
        val lounge = object : ClanLoungeRequest(client) {
            override suspend fun useKlaw() = Result.success("ok")
        }
        return BreakfastManager(
            campgroundRequest = campground,
            clanRumpusRequest = rumpus,
            clanLoungeRequest = lounge,
            preferences = prefs,
            useItemRequest = UseItemRequest(client),
            hermitRequest = HermitRequest(client),
            httpClient = client,
            skillManager = skillManager,
        )
    }

    @Test
    fun revision_phase556() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun breakfast_castsPrefListedSkill() = runBlocking {
        val casts = mutableListOf<Pair<String, Int>>()
        val p = prefs {
            putString("breakfastSoftcore", "Pastamastery")
        }
        val mgr = breakfast(
            p,
            skillManager(skill("Pastamastery", mp = 0, limit = 1), casts = casts),
        )
        mgr.runBreakfast(CharacterState(currentMp = 50), InventoryState())
        assertTrue(casts.any { it.first == "Pastamastery" })
        assertTrue(p.getBoolean(Preferences.BREAKFAST_COMPLETED, false))
    }

    @Test
    fun breakfast_skipsUnownedSkill() = runBlocking {
        val casts = mutableListOf<Pair<String, Int>>()
        val p = prefs {
            putString("breakfastSoftcore", "Pastamastery")
        }
        breakfast(p, skillManager(casts = casts))
            .runBreakfast(CharacterState(currentMp = 50), InventoryState())
        assertTrue(casts.none { it.first == "Pastamastery" })
        assertTrue(p.getBoolean(Preferences.BREAKFAST_COMPLETED, false))
    }

    @Test
    fun breakfastAlways_castsEvenWithoutSoftcoreList() = runBlocking {
        val casts = mutableListOf<Pair<String, Int>>()
        val p = prefs {
            putString("breakfastAlways", "Communism!")
            putString("breakfastSoftcore", "")
        }
        breakfast(
            p,
            skillManager(skill("Communism!", mp = 0, limit = 0), casts = casts),
        ).runBreakfast(CharacterState(), InventoryState())
        assertTrue(casts.any { it.first == "Communism!" })
    }
}
