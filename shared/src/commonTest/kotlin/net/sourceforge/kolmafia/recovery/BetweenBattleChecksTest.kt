package net.sourceforge.kolmafia.recovery

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.LightsOutManager
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Between-battle hub ordering + re-entrancy (Phases 1911–1925). */
class BetweenBattleChecksTest {

    private fun prefs(block: MapSettings.() -> Unit = {}) =
        Preferences(MapSettings().apply(block))

    private fun recoveryManager(preferences: Preferences = prefs()): RecoveryManager {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val bus = GameEventBus()
        return RecoveryManager(
            InventoryManager(client, bus),
            SkillManager(client, SkillCastRequest(client), bus),
            preferences,
        )
    }

    @Test
    fun runBetweenBattleChecks_skipsWhenAlreadyActive() = runBlocking {
        val rm = recoveryManager()
        rm.isRecoveryActive = true
        val result = rm.runBetweenBattleChecks(ctx = BetweenBattleContext())
        assertEquals(BetweenBattleResult.Skipped, result)
    }

    @Test
    fun runBetweenBattleChecks_ordersScriptMoodHpBurnMp() = runBlocking {
        val order = mutableListOf<String>()
        val rm = recoveryManager()
        val result = rm.runBetweenBattleChecks(
            isScriptCheck = true,
            isMoodCheck = true,
            isHealthCheck = true,
            isManaCheck = true,
            ctx = BetweenBattleContext(
                executeBetweenBattleScript = { order += "script" },
                executeMood = { order += "mood" },
                recoverHpStep = { order += "hp"; false },
                burnExtraMana = { order += "burn" },
                recoverMpStep = { order += "mp"; false },
                currentHp = { 50 },
                turnsPlayed = { 10 },
            ),
        )
        assertEquals(BetweenBattleResult.Ok, result)
        assertEquals(listOf("script", "mood", "hp", "burn", "mp"), order)
    }

    @Test
    fun runBetweenBattleChecks_abortsOnZeroHp() = runBlocking {
        val rm = recoveryManager()
        val result = rm.runBetweenBattleChecks(
            ctx = BetweenBattleContext(
                currentHp = { 0 },
                maxHp = { 100 },
                edFightInProgress = { false },
            ),
        )
        assertEquals(BetweenBattleResult.AbortedZeroHp, result)
    }

    @Test
    fun runBetweenBattleChecks_startsLightsOutCounter() = runBlocking {
        val p = prefs {
            putBoolean(LightsOutManager.TRACK_PREF, true)
            putString(LightsOutManager.NEXT_ELIZABETH, "The Haunted Kitchen")
        }
        val rm = recoveryManager(p)
        rm.runBetweenBattleChecks(
            ctx = BetweenBattleContext(currentHp = { 10 }, turnsPlayed = { 1 }),
        )
        assertTrue(TurnCounter.isCounting(p, LightsOutManager.COUNTER_LABEL, 1))
    }
}
